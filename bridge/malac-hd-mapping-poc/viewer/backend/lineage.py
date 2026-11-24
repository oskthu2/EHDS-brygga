"""Combines the STATIC trace parsed out of the shared .map files (fml_parser.py) with the ONE
declarative list of fields the DRIVER sets directly on the transform() result, not via FML -
../../driver-fields.json, read here via driver_fields.py (see that module's docstring for why
this used to be its own hand-written, independently-maintained table, and no longer is) - then
resolves every edge's "[]" placeholders against ONE concrete record (a chosen fixture's single
<diagnosis> entry + its actual Condition/Provenance JSON) into the exact dotted/bracketed
paths source_tree.py and target_tree.py use, so the frontend can match a hover on one tree's
node to the connected row(s) on the other side by plain string equality.

Resolving WHICH concrete Condition.extension[] entry a given diagnosisBody.relatedDiagnosis[K]
produced - the one place either fixture has a genuinely repeating element - used to be an
ORDER-based ASSUMPTION (the K-th relatedDiagnosis paired with the K-th matching extension,
unproven for more than one entry). For the Python/MaLaC-HD engine this is now a REAL,
per-request FACT instead: traced_python_engine.py runs the actual compiled code with the
relevant target list instrumented, and resolve_edges()'s `runtime_hints` carries that real
{source index -> target index} map in, which _disambiguate() uses outright whenever it is
given (see resolve_target_path()'s docstring). The Java/HAPI engine has no such hook (see
../README.md and HapiFmlEngine.java's Javadoc for why) and keeps the order-based fallback,
clearly labelled as such by server.py's "related_index_source" response field.

A THIRD, now PRIMARY, layer of evidence sits above both of those (Oskar's "traceability
anchor" idea - see the PR task description and value_match.py's module docstring for the full
account of why it is VALUE-MATCHING rather than literal marker injection): for every edge
whose rule has a known, FHIR-type-safe transform (value_match.expected_value()), this module
computes what the REAL transform should have produced from the raw XML leaf value this
record actually carries, and - while walking a repeating target container ("[]") - checks
each candidate index's real value against it. A UNIQUE match wins outright, for EITHER
engine's real, unmodified output, with no engine-specific code at all beyond "which JSON did
this value come out of" - strictly more trustworthy than both the static parse (Part 1) and
the order/runtime-hint fallback (Part 2) below, since it is attested by an actually-executed
transform's real output, not inferred from the .map file's text or from list order. Each edge
resolve_edges() returns now carries a `match_source` field: "value" (this layer resolved it
uniquely), "runtime" (traced_python_engine.py's real observed index, Python only),
"heuristic" (the order-based assumption, Java's relatedDiagnosis fallback), or "static" (a
single, already-unambiguous candidate - correct by construction, just not independently
value-verified, e.g. a field with no known transform in value_match.py, or an edge
value_match.py deliberately skips - see resolve_edges()'s handling of `kind == "translate"`).
"""
import os
import re
import sys

import fml_parser
import value_match

HERE = os.path.dirname(os.path.abspath(__file__))
FML_DIR = os.path.join(HERE, "..", "..", "python", "fml")
PYTHON_DIR = os.path.join(HERE, "..", "..", "python")
if PYTHON_DIR not in sys.path:
    sys.path.insert(0, PYTHON_DIR)
import driver_fields  # noqa: E402

_UNSET = object()

CHRONIC_EXT_URL = "https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-chronic-condition"
RELATED_EXT_URL = "https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-related-condition"

# The ext-asserted-date extension's own URL is no longer a separate copy here - it is read
# straight out of ../driver-fields.json's "assertedDate-value" entry (see _driver_edges()
# below), the SAME file run_pipeline.py/driver_fields.py and HapiLineageCli.java now read to
# APPLY that field - so there is exactly one place this URL is declared for driver-field
# purposes (the .map file's own "assertedDateExt.url = '...'" literal, which CREATES the
# extension, necessarily still has its own copy - FML builds the container, the driver fills
# its value; see driver-fields.json's module-level comment for that split).
EXTENSION_URL_BY_RULE_PREFIX = {
    "chronic": CHRONIC_EXT_URL,
    "related": RELATED_EXT_URL,
}
AGENT_CODE_BY_RULE_PREFIX = {"custodian": "custodian", "author": "author", "assembler": "assembler"}


def _driver_edges():
    """Fields set by the driver (run_pipeline.py / the viewer's Python engine, both via
    driver_fields.py; HapiLineageCli.java on the Java side), not by FML - see this module's
    docstring and ../../driver-fields.json (the ONE declarative list all of those, and this
    function, now read - previously this was its own hand-written table, independent of the
    driver code it was meant to describe). Source paths use the SAME lm-diagnosis path shape
    fml_parser's static edges do, with "diagnosis[]" left as a placeholder like every other
    static edge (resolved alongside them, below)."""
    edges = []
    for field in driver_fields.load_driver_fields()["condition"]:
        edges.append(fml_parser.Edge(field["source_path"], field["target_path"], field["rule"], "driver"))
        if "extension_url" in field:
            EXTENSION_URL_BY_RULE_PREFIX[_rule_prefix(field["rule"])] = field["extension_url"]
    return edges


def _driver_provenance_edges():
    return [
        fml_parser.Edge(field["source_path"], field["target_path"], field["rule"], "driver")
        for field in driver_fields.load_driver_fields()["provenance"]
    ]


def load_static_edges():
    """Returns (condition_edges, provenance_edges), each a list of fml_parser.Edge with
    "[]" placeholders still unresolved - the static trace, independent of any one record."""
    cond = fml_parser.parse_condition_map(os.path.join(FML_DIR, "get-diagnosis-to-condition.map"))
    cond += _driver_edges()
    prov = fml_parser.parse_provenance_map(os.path.join(FML_DIR, "get-diagnosis-to-provenance.map"))
    prov += _driver_provenance_edges()
    return cond, prov


_ARRAY_TOKEN_RE = re.compile(r"^(\w+)(\[\])?$")


def _rule_prefix(rule: str) -> str:
    m = re.match(r"^([A-Za-z]+)", rule)
    return m.group(1) if m else rule


def _get_path(obj, tokens):
    """Plain navigation of `tokens` (dotted/"[]" path segments, e.g. ["who", "identifier",
    "value"]) off ONE candidate array item - any further "[]" token along the way always
    resolves to index 0 (consistent with _disambiguate's own rule for category/coding/
    identifier, the only further-nested array fields either .map file ever produces).
    Returns None if the path does not fully resolve - used ONLY to pull a comparison leaf
    value out of a candidate while value-matching (see _disambiguate), never to build the
    final target_path itself."""
    cur = obj
    for token in tokens:
        m = _ARRAY_TOKEN_RE.match(token)
        if not m:
            return None
        name, is_arr = m.group(1), bool(m.group(2))
        cur = cur.get(name) if isinstance(cur, dict) else None
        if cur is None:
            return None
        if is_arr:
            if not isinstance(cur, list) or not cur:
                return None
            cur = cur[0]
    return cur


def resolve_target_path(pattern: str, resource_json: dict, rule: str, related_k,
                         related_index_hint=None, expected_value=_UNSET):
    """Walks `pattern` (e.g. "extension[].valueReference.identifier.value") against the
    ACTUAL resource JSON, replacing every "name[]" token with a concrete "name[idx]" - see
    the module docstring for how each ambiguous array field is disambiguated. Returns
    (concrete_path, ok, match_source) - ok=False means this record simply has no matching
    element (e.g. no chronicDiagnosis extension was created at all), so the caller should drop
    the edge. match_source is "value" wherever VALUE-MATCHING (see value_match.py) resolved
    an array token uniquely, or confirmed the final leaf's value outright when the pattern had
    no array token at all to disambiguate; otherwise "runtime"/"heuristic" (set by
    _disambiguate, "related" rule only) or "static" (no array ambiguity / no known transform
    to check against).

    `related_index_hint`: for the "related" rule's extension[] ONLY, the REAL Condition.
    extension[] index observed while the Python/MaLaC-HD engine actually ran (see
    traced_python_engine.py) - used as a fallback when VALUE-MATCHING itself can't resolve the
    index uniquely (see _disambiguate). `expected_value`: value_match.expected_value()'s
    result for this edge's (rule, raw source value) - _UNSET/None disables value-matching for
    this call entirely (falls back to the pre-existing behaviour unchanged)."""
    parts = pattern.split(".")
    cur = resource_json
    out = []
    match_source = "static"
    for i, token in enumerate(parts):
        m = _ARRAY_TOKEN_RE.match(token)
        if not m:
            return None, False, match_source
        name, is_arr = m.group(1), bool(m.group(2))
        if not is_arr:
            out.append(name)
            cur = cur.get(name) if isinstance(cur, dict) else None
            if cur is None:
                return None, False, match_source
            continue
        lst = cur.get(name) if isinstance(cur, dict) else None
        if not isinstance(lst, list) or not lst:
            return None, False, match_source
        idx, match_source = _disambiguate(
            name, lst, rule, related_k, related_index_hint, parts[i + 1 :], expected_value
        )
        if idx is None or idx >= len(lst):
            return None, False, match_source
        out.append("%s[%d]" % (name, idx))
        cur = lst[idx]
    if (
        match_source == "static"
        and expected_value is not _UNSET
        and expected_value is not None
        and cur == expected_value
    ):
        # No array token needed disambiguating at all (a single, already-unambiguous
        # candidate) - but the REAL value at that one candidate position still equals what
        # the real transform should have produced, so this edge is just as value-verified as
        # one that needed disambiguating. Promote it from "static" to "value" rather than
        # under-selling a perfectly good match just because there was nothing to pick between.
        match_source = "value"
    return ".".join(out), True, match_source


def _disambiguate(name, lst, rule, related_k, related_index_hint=None,
                   remaining_tokens=None, expected_value=_UNSET):
    value_idx = None
    if expected_value is not _UNSET and expected_value is not None:
        candidates = [
            i for i, item in enumerate(lst)
            if _get_path(item, remaining_tokens or []) == expected_value
        ]
        if len(candidates) == 1:
            value_idx = candidates[0]
    if name in ("category", "coding", "identifier"):
        if value_idx is not None:
            return value_idx, "value"
        return 0, "static"
    if name == "agent":
        if value_idx is not None:
            return value_idx, "value"
        expected = AGENT_CODE_BY_RULE_PREFIX.get(_rule_prefix(rule))
        for i, item in enumerate(lst):
            codings = ((item.get("type") or {}).get("coding")) or []
            if codings and codings[0].get("code") == expected:
                return i, "static"
        return None, "static"
    if name == "extension":
        expected_url = EXTENSION_URL_BY_RULE_PREFIX.get(_rule_prefix(rule))
        matches = [i for i, item in enumerate(lst) if item.get("url") == expected_url]
        if not matches:
            return None, "static"
        if value_idx is not None and value_idx in matches:
            # A REAL match against the actual output of an unmodified run (see
            # value_match.py) - the most trustworthy evidence this viewer can produce,
            # stronger than either the runtime hint or the order-based assumption below.
            return value_idx, "value"
        if _rule_prefix(rule) == "related":
            if related_index_hint is not None and related_index_hint in matches:
                # A REAL index, observed while the actual generated code ran (see
                # traced_python_engine.py) - not an assumption, so it wins outright, even if
                # it disagrees with list order.
                return related_index_hint, "runtime"
            k = related_k or 0
            return (matches[k] if k < len(matches) else None), "heuristic"
        return matches[0], "static"
    if value_idx is not None:
        return value_idx, "value"
    return 0, "static"


def resolve_edges(diagnosis_index: int, related_count: int, resource_jsons: dict,
                   runtime_hints: dict = None, lm_values: dict = None):
    """resource_jsons: {"condition": <dict>, "provenance": <dict>}. Returns a flat list of
    {source_path, resource, target_path, rule, kind, conceptmap, match_source} - the
    "diagnosis[]" and "relatedDiagnosis[]" placeholders resolved using diagnosis_index/
    related_count (known directly from the chosen fixture's XML, see server.py), everything
    else resolved against the actual resource JSON (see resolve_target_path). `runtime_hints`:
    whatever the engine that produced resource_jsons observed while actually running (see
    engines.py) - currently just {"related_extension_index": {k: real Condition.extension[]
    index}}, non-empty only for the Python engine; {} (the Java engine) means "no real hints,
    use the fallback". `lm_values`: {lm_path: raw_xml_value} for this SAME record (see
    source_tree.flatten_lm_values) - used to look up each edge's raw source value and, via
    value_match.expected_value(), what the REAL transform should have produced from it, so
    resolve_target_path can attempt a VALUE-MATCH (see this module's docstring, "Part 3")
    before falling back to the runtime-hint/order-based/static behaviour. {} or None disables
    value-matching entirely (every edge falls back exactly as before this layer existed)."""
    related_index_map = (runtime_hints or {}).get("related_extension_index") or {}
    lm_values = lm_values or {}
    cond_edges, prov_edges = load_static_edges()
    out = []
    for resource, edges in (("condition", cond_edges), ("provenance", prov_edges)):
        resource_json = resource_jsons.get(resource) or {}
        for e in edges:
            src_pattern = e.source_path.replace("diagnosis[]", "diagnosis[%d]" % diagnosis_index, 1)
            # "coding[]" (diagnosisCode/typeOfDiagnosis's single nested Coding - the only
            # OTHER array source field fml_parser.ARRAY_SOURCE_FIELDS tracks, besides
            # diagnosis/relatedDiagnosis which are resolved above/below) always has exactly
            # ONE entry in this source model (lm_diagnosis_json_builder.py's
            # _codeable_concept() builds a single coding dict, never several) - resolving it
            # to its one concrete index here, the same way source_tree.py's leaves already do
            # (their lm_path literally says "coding[0]"), is what makes a source_path
            # produced here ever equal to source_tree's (needed for both the frontend's
            # hover-highlighting AND this function's own lm_values lookup below - neither
            # worked for these two fields before this fix, since "coding[]" vs "coding[0]"
            # never matched as strings).
            src_pattern = src_pattern.replace("coding[]", "coding[0]")
            # ConceptMap/translate()-driven edges stay on the static-rule-based trace at
            # whole-element granularity by design (Oskar's own follow-up, relayed via the
            # orchestrating session): the .map file's translate() call plus the ConceptMap
            # resource IS the authoritative definition of where a coded element lands, so
            # value-matching a sub-field of it would be re-verifying an already-solved
            # question with a less reliable technique, not strengthening it.
            skip_value_match = e.kind == "translate"
            if "relatedDiagnosis[]" in src_pattern:
                for k in range(related_count):
                    src = src_pattern.replace("relatedDiagnosis[]", "relatedDiagnosis[%d]" % k, 1)
                    exp = None if skip_value_match else value_match.expected_value(e.rule, lm_values.get(src))
                    tgt, ok, match_source = resolve_target_path(
                        e.target_path, resource_json, e.rule, related_k=k,
                        related_index_hint=related_index_map.get(k), expected_value=exp,
                    )
                    if ok:
                        out.append({"source_path": src, "resource": resource, "target_path": tgt,
                                    "rule": e.rule, "kind": e.kind, "conceptmap": e.conceptmap,
                                    "match_source": match_source})
            else:
                exp = None if skip_value_match else value_match.expected_value(e.rule, lm_values.get(src_pattern))
                tgt, ok, match_source = resolve_target_path(
                    e.target_path, resource_json, e.rule, related_k=None, expected_value=exp
                )
                if ok:
                    out.append({"source_path": src_pattern, "resource": resource, "target_path": tgt,
                                "rule": e.rule, "kind": e.kind, "conceptmap": e.conceptmap,
                                "match_source": match_source})
    return out


def resolve_cross_target_links(resource_jsons: dict):
    """Provenance.target[].reference <-> the Condition it points at: NOT sourced from any
    RIVTA field at all - both engines mint a fresh UUID for Condition.id and copy it into
    Provenance.target themselves (see get-diagnosis-to-provenance.map's header comment) - so
    this is a target-tree-to-target-tree link, not a source->target lineage edge, and is kept
    in its own list rather than forced into the source->target shape above."""
    prov = resource_jsons.get("provenance") or {}
    cond = resource_jsons.get("condition") or {}
    links = []
    if prov.get("target") and cond.get("id"):
        links.append({"from_resource": "condition", "from_path": "id",
                      "to_resource": "provenance", "to_path": "target[0].reference"})
    return links


if __name__ == "__main__":
    cond_edges, prov_edges = load_static_edges()
    print("condition edges:", len(cond_edges))
    print("provenance edges:", len(prov_edges))
