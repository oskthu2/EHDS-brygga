# GetDiagnosis FML lineage viewer

A small local dev tool (NOT part of the Maven build or the production bridge) that:

1. Lets you pick a GetDiagnosis sample fixture (the same XML fixtures the comparison harness
   uses - `../src/test/resources/testmessages/*.xml`).
2. Renders the RIVTA source message as a navigable field tree (not raw XML text).
3. Runs the shared `.map` files (`../python/fml/*.map`) through either engine - MaLaC-HD
   (compiled Python) or HAPI FHIR's `StructureMapUtilities` (Java) - and renders the resulting
   Condition + Provenance as navigable trees.
4. Draws a line from every source field to where it lands in the target resource(s), and on
   hover highlights the whole chain (source row, line(s), target row(s)) - in both directions.

It only covers GetDiagnosis (the one contract with both engines wired up) but the backend is
structured so a second contract (e.g. GetCareDocumentation) could be added later by adding
another `fixtures dir` + another static-trace table, without changing the tree/SVG/hover code.

## Running it

One-time setup (installs nothing new - `lxml` is already in `../python/requirements.txt`,
already used by the rest of this module; this just warms the Java classpath cache so the first
`java` request isn't slow):

```bash
cd bridge/malac-hd-mapping-poc
mvn -q compile dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
```

(The backend will run this itself on first use if you skip it - see `engines.py`.)

Start the backend:

```bash
cd bridge/malac-hd-mapping-poc/viewer/backend
python3 server.py            # serves the API *and* the frontend on http://127.0.0.1:8008/
```

Open <http://127.0.0.1:8008/> in a browser. Pick a fixture, pick an engine (Python/MaLaC-HD or
Java/HAPI), and hover any leaf row in any of the three columns.

No `npm install`, no build step - the frontend is one HTML file, one `app.js` (vanilla JS,
inline SVG for the lines), one stylesheet.

## Architecture

```
viewer/backend/
  server.py        stdlib http.server: serves the frontend + two JSON endpoints
                    (/api/fixtures, /api/map?fixture=...&engine=python|java)
  engines.py        runs the SAME lm-diagnosis JSON (built once, in Python) through either
                    run_pipeline.py's compiled-transform helpers, or (via subprocess) the new
                    Java CLI HapiLineageCli, reusing HapiFmlEngine
  fml_parser.py      a small, grammar-specific parser for get-diagnosis-to-{condition,
                    provenance}.map - extracts "source field -> target field" per named rule
  lineage.py         combines fml_parser's static trace with a hand-written table of the
                    handful of fields the DRIVER sets directly (onset, assertedDate's value,
                    the three "uri" fields, Provenance.recorded/.target - see
                    get-diagnosis-to-{condition,provenance}.map's own header comments for why
                    FML cannot set these on either engine), then resolves every edge's "[]"
                    placeholders against one concrete record
  source_tree.py     RIVTA XML -> navigable tree, each leaf annotated with its resolved
                    lm-diagnosis JSON path (or None - a genuinely unmapped field)
  target_tree.py     generic FHIR-JSON -> navigable tree (used for both Condition and
                    Provenance)

viewer/frontend/
  index.html, style.css, app.js   three columns (source | Condition | Provenance), one SVG
                    overlay for the connecting lines, hover = highlight the whole chain

src/main/java/se/inera/ehds/malac/hapi/HapiLineageCli.java
                    new thin CLI entry point added for this viewer: takes two already-built
                    lm-diagnosis JSON files (condition-pass/provenance-pass, same shape
                    run_pipeline.py builds) and prints {"condition": ..., "provenance": ...}
                    by running HapiFmlEngine - no new RIVTA-XML parsing on the Java side at all
```

### How the source -> target lineage is computed

Two pieces, combined:

1. **Static trace from the `.map` files** (`fml_parser.py`): a small recursive-descent parser
   (NOT a general FML implementation) that understands exactly the constructs
   `get-diagnosis-to-condition.map`/`get-diagnosis-to-provenance.map` use - `<var>[.path] [as
   <newvar>] [where ...]` on the left of `->`, `create('Type') [as <var>]` / `cc(...)` /
   `translate(<var>, '<url>', 'Coding')` / a bare variable / a `'literal'` on the right - and
   emits one edge per LEAF value assignment (a `create()`/`cc()`/literal-only rule registers
   structure but emits no edge itself; the real value flows through a nested rule). Variable
   names are globally unique in both files (confirmed by inspection, and MaLaC-HD itself
   relies on this too), so a single `{name: resolved_path}` dict per file is enough - no lexical
   scope stack needed.

   A handful of fields are deliberately **not** set via FML at all on either engine - see each
   `.map` file's own header comment (`Condition.onset[x]`, the `ext-asserted-date` extension's
   value, the three `meta.source`/`subject.identifier.system`/`code.coding[0].system` "uri"
   fields, `Provenance.recorded`/`.target`) - those are added to the trace by hand in
   `lineage.py`'s `_driver_edges()`/`_driver_provenance_edges()`, since their real logic lives
   in the driver code (`run_pipeline.py` / `HapiLineageCli.java`), not in FML.

2. **Index resolution against one concrete record** (`lineage.py`'s `resolve_target_path`):
   the static trace above still has `[]` placeholders (e.g. `extension[].valueBoolean`,
   `agent[].who.identifier.value`) - which concrete array index a given rule's output landed at
   depends on the actual data. Non-repeating containers (`category`, `coding`, the top-level
   `identifier`) always resolve to index 0 in this data model (the lm-diagnosis JSON builder
   never produces more than one). `extension` and `agent` entries are disambiguated by
   **content**, not position: each extension/agent kind carries a distinguishing literal
   constant from the SAME `.map` rule (an extension's `.url`, an agent's
   `type.coding[0].code`), so the resolver matches on that instead of assuming a fixed order.

   **Known limitation**, stated rather than hidden: `relatedDiagnosis` is the one genuinely
   repeating element either fixture exercises (0 or 1 entries in the two existing fixtures).
   Pairing the K-th `relatedDiagnosis` source entry with the K-th matching
   `ext-related-condition` extension in the output is done by **list order** - i.e. the
   resolver assumes both engines append repeated elements in the same order the source list had
   them, which holds for every fixture tried but is not independently proven for more than one
   entry. A fully robust fix would need runtime instrumentation of the generated Python/Java
   setter calls (see the task that produced this viewer for the original discussion); this
   order-based heuristic was judged reliable enough for a demo tool given the fixtures on hand.

   A third category - `Provenance.target[].reference` <-> `Condition.id` - is not a RIVTA-field
   lineage edge at all (both engines just mint a fresh UUID and copy it across), so it is kept
   in its own `cross_target_links` list and drawn/highlighted the same way, but as a
   target-to-target link rather than a source-to-target one.

### Verified example

Hovering **`documentId: doc-1`** under `diagnosis[0].diagnosisHeader` in the source tree
highlights **`identifier[0].value: doc-1`** in the Condition tree (rule `documentId-value`), for
both the Python and the Java engine, confirmed by actually running the tool (see the PR
description for a screenshot). Hovering **`chronicDiagnosis: true`** highlights
**`extension[1].valueBoolean: true`**. Hovering **`healthcareProfessionalCareGiverHSAId`**
highlights **`Provenance.agent[0].who.identifier.value`** (the custodian agent) - and the reverse
direction (hovering that Provenance row) highlights back to the same source row. Hovering
`Condition.id` highlights `Provenance.target[0].reference` via the cross-target link.
