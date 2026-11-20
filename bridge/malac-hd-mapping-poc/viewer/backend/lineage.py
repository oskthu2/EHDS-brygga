"""Combines the STATIC trace parsed out of the shared .map files (fml_parser.py) with a small,
hand-written table of the fields that are deliberately NOT set via FML - see
get-diagnosis-to-condition.map / get-diagnosis-to-provenance.map's header comments and
malac-hd-evaluation.md for exactly why each one is set by the driver
(run_pipeline.py / HapiMapperDump.java / HapiLineageCli.java) directly on the transform()
result instead - then resolves every edge's "[]" placeholders against ONE concrete record
(a chosen fixture's single <diagnosis> entry + its actual Condition/Provenance JSON) into the
exact dotted/bracketed paths source_tree.py and target_tree.py use, so the frontend can match
a hover on one tree's node to the connected row(s) on the other side by plain string equality.

KNOWN LIMITATION (documented, not hidden - see the task's own note on this): the ONLY
genuinely repeating element either fixture exercises is diagnosisBody.relatedDiagnosis (0 or 1
entries in the two existing fixtures). Resolving which concrete Condition.extension[] entry a
given relatedDiagnosis[K] produced is done by ORDER: the K-th relatedDiagnosis in the source
XML is paired with the K-th Condition.extension entry whose .url is the related-condition
extension's - i.e. list-order-preserving is ASSUMED, not independently proven for more than
one entry (both existing fixtures have at most one). If a future fixture's relatedDiagnosis
entries ever came out of order on one engine and not the other, this pairing would silently
mismatch; a real fix would need runtime instrumentation of the generated Python/Java setter
calls (see this PR's task description) rather than this order-based heuristic.
"""
import os
import re

import fml_parser

HERE = os.path.dirname(os.path.abspath(__file__))
FML_DIR = os.path.join(HERE, "..", "..", "python", "fml")

CHRONIC_EXT_URL = "https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-chronic-condition"
RELATED_EXT_URL = "https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-related-condition"
ASSERTED_DATE_EXT_URL = "https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-asserted-date"

EXTENSION_URL_BY_RULE_PREFIX = {
    "chronic": CHRONIC_EXT_URL,
    "related": RELATED_EXT_URL,
    "assertedDate": ASSERTED_DATE_EXT_URL,
}
AGENT_CODE_BY_RULE_PREFIX = {"custodian": "custodian", "author": "author", "assembler": "assembler"}


def _driver_edges():
    """Fields set by the driver (run_pipeline.py's _set_onset_and_asserted_date/_set_uri_fields,
    HapiLineageCli's equivalents), not by FML - see this module's docstring. Source paths use
    the SAME lm-diagnosis path shape fml_parser's static edges do, with "diagnosis[]" left as a
    placeholder like every other static edge (resolved alongside them, below)."""
    return [
        fml_parser.Edge("diagnosis[].diagnosisBody.diagnosisTime", "onsetDateTime", "driver-onset", "driver"),
        fml_parser.Edge(
            "diagnosis[].diagnosisHeader.legalAuthenticator.signatureTime",
            "extension[].valueDateTime", "assertedDate-value", "driver",
        ),
        fml_parser.Edge(
            "diagnosis[].diagnosisHeader.sourceSystemHSAId.value", "meta.source", "driver-meta-source", "driver"
        ),
        fml_parser.Edge(
            "diagnosis[].diagnosisHeader.patientId.system", "subject.identifier.system",
            "driver-subject-identifier-system", "driver",
        ),
        fml_parser.Edge(
            "diagnosis[].diagnosisBody.diagnosisCode.coding[].system", "code.coding[].system",
            "driver-code-coding-system", "driver",
        ),
    ]


def _driver_provenance_edges():
    return [
        fml_parser.Edge(
            "diagnosis[].diagnosisHeader.accountableHealthcareProfessional.authorTime",
            "recorded", "driver-recorded", "driver",
        ),
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


def resolve_target_path(pattern: str, resource_json: dict, rule: str, related_k):
    """Walks `pattern` (e.g. "extension[].valueReference.identifier.value") against the
    ACTUAL resource JSON, replacing every "name[]" token with a concrete "name[idx]" - see
    the module docstring for how each ambiguous array field is disambiguated. Returns
    (concrete_path, ok) - ok=False means this record simply has no matching element (e.g. no
    chronicDiagnosis extension was created at all), so the caller should drop the edge."""
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
        idx = _disambiguate(name, lst, rule, related_k)
        if idx is None or idx >= len(lst):
            return None, False
        out.append("%s[%d]" % (name, idx))
        cur = lst[idx]
    return ".".join(out), True


def _disambiguate(name, lst, rule, related_k):
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
            k = related_k or 0
            return matches[k] if k < len(matches) else None
        return matches[0]
    return 0


def resolve_edges(diagnosis_index: int, related_count: int, resource_jsons: dict):
    """resource_jsons: {"condition": <dict>, "provenance": <dict>}. Returns a flat list of
    {source_path, resource, target_path, rule, kind, conceptmap} - the "diagnosis[]" and
    "relatedDiagnosis[]" placeholders resolved using diagnosis_index/related_count (known
    directly from the chosen fixture's XML, see server.py), everything else resolved against
    the actual resource JSON (see resolve_target_path)."""
    cond_edges, prov_edges = load_static_edges()
    out = []
    for resource, edges in (("condition", cond_edges), ("provenance", prov_edges)):
        resource_json = resource_jsons.get(resource) or {}
        for e in edges:
            src_pattern = e.source_path.replace("diagnosis[]", "diagnosis[%d]" % diagnosis_index, 1)
            if "relatedDiagnosis[]" in src_pattern:
                for k in range(related_count):
                    src = src_pattern.replace("relatedDiagnosis[]", "relatedDiagnosis[%d]" % k, 1)
                    tgt, ok = resolve_target_path(e.target_path, resource_json, e.rule, related_k=k)
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
