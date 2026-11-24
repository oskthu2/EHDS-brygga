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
"""
import os
import re
import sys

import fml_parser

HERE = os.path.dirname(os.path.abspath(__file__))
FML_DIR = os.path.join(HERE, "..", "..", "python", "fml")
PYTHON_DIR = os.path.join(HERE, "..", "..", "python")
if PYTHON_DIR not in sys.path:
    sys.path.insert(0, PYTHON_DIR)
import driver_fields  # noqa: E402

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


def resolve_target_path(pattern: str, resource_json: dict, rule: str, related_k, related_index_hint=None):
    """Walks `pattern` (e.g. "extension[].valueReference.identifier.value") against the
    ACTUAL resource JSON, replacing every "name[]" token with a concrete "name[idx]" - see
    the module docstring for how each ambiguous array field is disambiguated. Returns
    (concrete_path, ok) - ok=False means this record simply has no matching element (e.g. no
    chronicDiagnosis extension was created at all), so the caller should drop the edge.

    `related_index_hint`: for the "related" rule's extension[] ONLY, the REAL Condition.
    extension[] index observed while the Python/MaLaC-HD engine actually ran (see
    traced_python_engine.py) - when given, _disambiguate() uses it directly instead of its
    order-based assumption. None (the Java engine, or any other rule) falls back to that
    assumption, unchanged."""
    parts = pattern.split(".")
    cur = resource_json
    out = []
    for token in parts:
        m = _ARRAY_TOKEN_RE.match(token)
        if not m:
            return None, False
        name, is_arr = m.group(1), bool(m.group(2))
        if not is_arr:
            out.append(name)
            cur = cur.get(name) if isinstance(cur, dict) else None
            if cur is None:
                return None, False
            continue
        lst = cur.get(name) if isinstance(cur, dict) else None
        if not isinstance(lst, list) or not lst:
            return None, False
        idx = _disambiguate(name, lst, rule, related_k, related_index_hint)
        if idx is None or idx >= len(lst):
            return None, False
        out.append("%s[%d]" % (name, idx))
        cur = lst[idx]
    return ".".join(out), True


def _disambiguate(name, lst, rule, related_k, related_index_hint=None):
    if name in ("category", "coding", "identifier"):
        return 0
    if name == "agent":
        expected = AGENT_CODE_BY_RULE_PREFIX.get(_rule_prefix(rule))
        for i, item in enumerate(lst):
            codings = ((item.get("type") or {}).get("coding")) or []
            if codings and codings[0].get("code") == expected:
                return i
        return None
    if name == "extension":
        expected_url = EXTENSION_URL_BY_RULE_PREFIX.get(_rule_prefix(rule))
        matches = [i for i, item in enumerate(lst) if item.get("url") == expected_url]
        if not matches:
            return None
        if _rule_prefix(rule) == "related":
            if related_index_hint is not None and related_index_hint in matches:
                # A REAL index, observed while the actual generated code ran (see
                # traced_python_engine.py) - not an assumption, so it wins outright, even if
                # it disagrees with list order.
                return related_index_hint
            k = related_k or 0
            return matches[k] if k < len(matches) else None
        return matches[0]
    return 0


def resolve_edges(diagnosis_index: int, related_count: int, resource_jsons: dict, runtime_hints: dict = None):
    """resource_jsons: {"condition": <dict>, "provenance": <dict>}. Returns a flat list of
    {source_path, resource, target_path, rule, kind, conceptmap} - the "diagnosis[]" and
    "relatedDiagnosis[]" placeholders resolved using diagnosis_index/related_count (known
    directly from the chosen fixture's XML, see server.py), everything else resolved against
    the actual resource JSON (see resolve_target_path). `runtime_hints`: whatever the engine
    that produced resource_jsons observed while actually running (see engines.py) - currently
    just {"related_extension_index": {k: real Condition.extension[] index}}, non-empty only
    for the Python engine; {} (the Java engine) means "no real hints, use the fallback"."""
    related_index_map = (runtime_hints or {}).get("related_extension_index") or {}
    cond_edges, prov_edges = load_static_edges()
    out = []
    for resource, edges in (("condition", cond_edges), ("provenance", prov_edges)):
        resource_json = resource_jsons.get(resource) or {}
        for e in edges:
            src_pattern = e.source_path.replace("diagnosis[]", "diagnosis[%d]" % diagnosis_index, 1)
            if "relatedDiagnosis[]" in src_pattern:
                for k in range(related_count):
                    src = src_pattern.replace("relatedDiagnosis[]", "relatedDiagnosis[%d]" % k, 1)
                    tgt, ok = resolve_target_path(
                        e.target_path, resource_json, e.rule, related_k=k,
                        related_index_hint=related_index_map.get(k),
                    )
                    if ok:
                        out.append({"source_path": src, "resource": resource, "target_path": tgt,
                                    "rule": e.rule, "kind": e.kind, "conceptmap": e.conceptmap})
            else:
                tgt, ok = resolve_target_path(e.target_path, resource_json, e.rule, related_k=None)
                if ok:
                    out.append({"source_path": src_pattern, "resource": resource, "target_path": tgt,
                                "rule": e.rule, "kind": e.kind, "conceptmap": e.conceptmap})
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
