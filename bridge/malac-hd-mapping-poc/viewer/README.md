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

One-time setup. First, install this module's Python dependencies (a fresh checkout, e.g. a
GitHub Codespace, won't have them yet):

```bash
cd bridge/malac-hd-mapping-poc/python
pip install -r requirements.txt
```

Then build and install this module's sibling dependency (`mapping-engine`) into your local
Maven repo - a fresh checkout has nothing installed yet, so `mvn dependency:build-classpath` run
from this module alone fails with "Could not find artifact
se.inera.ehds:mapping-engine:jar:0.1.0-SNAPSHOT" unless this has been run at least once:

```bash
cd bridge
mvn install -pl malac-hd-mapping-poc -am -DskipTests -q
```

Then warm the Java classpath cache so the first `java` request from the viewer isn't slow
(optional - the backend will run this itself on first use if you skip it, see `engines.py`):

```bash
cd bridge/malac-hd-mapping-poc
mvn -q compile dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
```

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
driver-fields.json  the ONE declarative list of fields set DIRECTLY on a transform() result
                    by driver code, not by either .map file (onset, assertedDate's value, the
                    three "uri" fields, Provenance.recorded) - read (not copied) by
                    python/driver_fields.py, src/main/java/.../HapiLineageCli.java, and (via
                    driver_fields.py) viewer/backend/lineage.py - see its own header comment

viewer/backend/
  server.py        stdlib http.server: serves the frontend + two JSON endpoints
                    (/api/fixtures, /api/map?fixture=...&engine=python|java)
  engines.py        runs the SAME lm-diagnosis JSON (built once, in Python) through either
                    traced_python_engine.py (in-process, instrumented), or (via subprocess)
                    the Java CLI HapiLineageCli, reusing HapiFmlEngine; applies driver-fields.json
                    via python/driver_fields.py on the Python side either way
  traced_python_engine.py
                    runs the REAL compiled get-diagnosis-to-{condition,provenance}.py
                    (unmodified) IN-PROCESS instead of via subprocess, instrumenting
                    Condition.extension's append() calls to observe, for REAL, which
                    relatedDiagnosis[K] produced which Condition.extension[] entry - see its
                    own module docstring for the full mechanism and why this exists
  fml_parser.py      a small, grammar-specific parser for get-diagnosis-to-{condition,
                    provenance}.map - extracts "source field -> target field" per named rule
  lineage.py         combines fml_parser's static trace with driver-fields.json's declarative
                    list (via python/driver_fields.py), then resolves every edge's "[]"
                    placeholders against one concrete record - using the REAL indices
                    traced_python_engine.py observed where it has them (Python engine only),
                    falling back to a content/order-based resolver otherwise
  source_tree.py     RIVTA XML -> navigable tree, each leaf annotated with its resolved
                    lm-diagnosis JSON path (or None - a genuinely unmapped field)
  target_tree.py     generic FHIR-JSON -> navigable tree (used for both Condition and
                    Provenance)

viewer/frontend/
  index.html, style.css, app.js   three columns (source | Condition | Provenance), one SVG
                    overlay for the connecting lines, hover = highlight the whole chain

python/driver_fields.py
                    reads driver-fields.json and APPLIES it - run_pipeline.py and
                    engines.py both call this instead of carrying their own copy of the
                    field list (see its own module docstring for the history: this used to
                    be three independent copies, one of them a hand-written table in
                    lineage.py)

src/main/java/se/inera/ehds/malac/hapi/HapiLineageCli.java
                    thin CLI entry point for this viewer: takes two already-built
                    lm-diagnosis JSON files (condition-pass/provenance-pass, same shape
                    run_pipeline.py builds), runs HapiFmlEngine, and applies driver-fields.json
                    via a small per-field Java dispatch table (CONDITION_SETTERS/
                    PROVENANCE_SETTERS) - no new RIVTA-XML parsing on the Java side at all

src/test/java/se/inera/ehds/malac/hapi/DriverFieldsConfigTest.java
                    fails if HapiLineageCli's Java dispatch tables ever drift from
                    driver-fields.json's own field-id list (an id with no Java setter, or a
                    setter for an id the config no longer has), and regression-checks that
                    applying them against a real fixture sets exactly the fields the config
                    declares
```

### How the source -> target lineage is computed

Oskar's objection to the first version of this tool (see the PR's task description for his
own words) was that the trace came from a **static interpretation of the `.map` files plus a
small, independently-maintained, hand-written table** for the handful of fields the driver
code sets directly - so a change to the driver code's own logic for those fields would not
show up on reload. This is addressed in two parts:

#### Part 1: ONE declarative source for the driver-set fields

`../driver-fields.json` is now the only place that lists which Condition/Provenance fields are
set outside FML, and where each comes from/goes. Three independent copies used to exist (the
Python driver's own hardcoded logic, the Java driver's own hardcoded logic, and `lineage.py`'s
hand-written trace table) - now there is one JSON file, and:

- **Python** (`python/driver_fields.py`): `run_pipeline.py` and the viewer's `engines.py` both
  call `apply_condition_fields()`/`apply_provenance_fields()`, which iterate the config and
  dispatch each field id to a small Python setter. A field's source value is read straight out
  of the SAME lm-diagnosis JSON the compiled transform just consumed (that JSON already
  carries every date-like value in its final, converted form - see
  `lm_diagnosis_json_builder.py` - so this is also a genuine simplification over the old code,
  which used to re-extract raw RIVTA date strings from the XML and re-convert them a second
  time, redundantly).
- **Java** (`HapiLineageCli.java`): reads the SAME `driver-fields.json` at runtime via Gson and
  dispatches each field id to a small per-field Java setter (`CONDITION_SETTERS`/
  `PROVENANCE_SETTERS`) - full generic dispatch down to "which HAPI-typed setter to call" was
  judged not reasonably achievable (HAPI's model is statically typed: `onsetDateTime` needs a
  `DateTimeType`, `recorded` needs an `InstantType`, etc.), so that one mechanical step stays
  in Java, but WHICH fields exist and where they come from/go is config-driven, and
  `DriverFieldsConfigTest` fails the build if the Java dispatch tables and the config's id list
  ever diverge.
- **The trace** (`lineage.py`): reads the SAME config (via `driver_fields.py`) instead of its
  own hand-written `_driver_edges()`/`_driver_provenance_edges()` table.

A change to which fields are driver-set, or to where they come from/go, is now made in exactly
one place, and every consumer re-reads that file rather than carrying its own copy.

#### Part 2: real execution tracing where it is achievable

The lineage trace itself still combines two pieces:

1. **Static trace from the `.map` files** (`fml_parser.py`): a small recursive-descent parser
   (NOT a general FML implementation) that understands exactly the constructs
   `get-diagnosis-to-condition.map`/`get-diagnosis-to-provenance.map` use - `<var>[.path] [as
   <newvar>] [where ...]` on the left of `->`, `create('Type') [as <var>]` / `cc(...)` /
   `translate(<var>, '<url>', 'Coding')` / a bare variable / a `'literal'` on the right - and
   emits one edge per LEAF value assignment. Variable names are globally unique in both files
   (confirmed by inspection, and MaLaC-HD itself relies on this too), so a single
   `{name: resolved_path}` dict per file is enough. This part already tracked `.map` edits on
   reload before this change (re-parsing the current file each time) and still does - it was
   never the stale part Oskar was objecting to.

2. **Index resolution** (`lineage.py`'s `resolve_target_path`): the static trace above still
   has `[]` placeholders (e.g. `extension[].valueBoolean`, `agent[].who.identifier.value`) -
   which concrete array index a given rule's output landed at depends on the actual data.
   Non-repeating containers (`category`, `coding`, the top-level `identifier`) always resolve
   to index 0 in this data model. `extension` and `agent` entries are disambiguated by
   **content** (an extension's `.url`, an agent's `type.coding[0].code`) - except for the ONE
   genuinely repeating, same-kind case: pairing the K-th `relatedDiagnosis` source entry with
   the K-th matching `ext-related-condition` extension.

   **For the Python/MaLaC-HD engine, that pairing is now a REAL, observed fact, not an
   assumption.** `traced_python_engine.py` runs the actual, unmodified compiled
   `GetDiagnosisToCondition()` function **in-process** (imported directly, not via subprocess)
   with `Condition.extension` swapped for a small list subclass right before the call, so every
   real `.append()` the generated code makes is observed; each append is tagged with whichever
   concrete `relatedDiagnosis[K]` source node the current Python stack frame has bound to the
   name `rel` at that moment (the source document is parsed into the same `SimpleNamespace`
   tree MaLaC-HD's own model builds, with every node additionally carrying its own concrete
   JSON path), then cross-checked against the FINAL exported JSON's `.url` so a stale `rel`
   left over from an earlier, unrelated loop can never be mistaken for a real pairing. The
   result is a REAL `{relatedDiagnosis index -> Condition.extension index}` map, which
   `lineage.py` uses outright whenever it is given one, instead of its order-based fallback -
   see that module's and `traced_python_engine.py`'s own docstrings for the full mechanism.

   **For the Java/HAPI engine, this is NOT implemented, for a documented reason.** The exact
   `org.hl7.fhir.r4:6.3.11` jar this repo pins was decompiled and inspected directly (not just
   web search) for a usable hook. `StructureMapUtilities` DOES expose one real extensibility
   point: its constructor takes an `ITransformerServices` callback, and its private `log(...)`
   method routes every debug line through `services.log(...)` instead of `System.out` when one
   is supplied - including one call, in `executeRule()`, of the exact shape
   `log(ruleName + " / " + vars.summary())` **once per rule execution**, with real variable
   bindings (confirmed by running it: see the `rule : ... ; vars = ...` lines `mvn test`
   prints for this module, currently sent to `System.out` because no `ITransformerServices` is
   supplied). This is real, and genuinely more than nothing - but it fires at RULE ENTRY, with
   a free-text `Variables.summary()` snapshot, not at the point `Base.setProperty()`/the
   target's list `.append()` actually runs (that code is in `executeRule`/`processTarget`/
   `runTransform`, all `private`, with no hook at all), so turning it into the same kind of
   real, concrete-index trace the Python side now has would mean parsing ANOTHER free-text
   format (not the `.map` file's this time, but `Variables.summary()`'s) - not a clear
   improvement in rigor over the thing it would replace. Going further still - wrapping the
   private target-processing methods via reflection, or instrumenting `Base.setProperty()`
   itself - was judged to require something close to a Java agent/bytecode-weaving approach,
   which the task's own guidance says is disproportionate for this dev tool. So the Java engine
   keeps the content/order-based fallback for `relatedDiagnosis` specifically, clearly labelled
   as such: the `/api/map` response's `related_index_source` field says `"runtime"` for the
   Python engine and `"heuristic (order-based assumption - ...)"` for Java, and the frontend
   shows this next to the fixture/engine picker rather than presenting both as equally trustworthy.

   A third category - `Provenance.target[].reference` <-> `Condition.id` - is not a RIVTA-field
   lineage edge at all (both engines just mint a fresh UUID and copy it across), so it is kept
   in its own `cross_target_links` list and drawn/highlighted the same way, but as a
   target-to-target link rather than a source-to-target one.

### Verified example

`get-diagnosis-flera-relaterade.xml` (added for this - a QA/test fixture Oskar can also use
directly, with TWO `relatedDiagnosis` entries instead of the old fixtures' zero-or-one) proves
the real tracing end to end: running it through the Python engine, `relatedDiagnosis[0]`
(`doc-0`) resolves to `Condition.extension[1]` and `relatedDiagnosis[1]` (`doc-2`) to
`Condition.extension[2]` - and the response's `related_index_source` says `"runtime"`, meaning
these two concrete indices came from actually observing `traced_python_engine.py`'s
instrumented `.append()` calls during a real run, not from assuming list order. The SAME
fixture through the Java engine resolves to `extension[2]`/`extension[3]` instead (HAPI builds
one more `Condition.extension` entry than MaLaC-HD does for this input - a pre-existing,
unrelated engine difference, not a bug in this viewer) with `related_index_source` honestly
reported as the order-based fallback.

Hovering **`documentId: doc-1`** under `diagnosis[0].diagnosisHeader` in the source tree
highlights **`identifier[0].value: doc-1`** in the Condition tree (rule `documentId-value`), for
both the Python and the Java engine, confirmed by actually running the tool. Hovering
**`chronicDiagnosis: true`** highlights **`extension[1].valueBoolean: true`**. Hovering
**`healthcareProfessionalCareGiverHSAId`** highlights
**`Provenance.agent[0].who.identifier.value`** (the custodian agent) - and the reverse
direction (hovering that Provenance row) highlights back to the same source row. Hovering
`Condition.id` highlights `Provenance.target[0].reference` via the cross-target link.
