"""Builds a navigable tree of the RIVTA GetDiagnosis XML's actual content (field paths +
values, not raw XML text) for ONE <diagnosis> entry, annotating every leaf with its resolved
lm-diagnosis JSON path (the SAME shape lm_diagnosis_json_builder.py/
LmDiagnosisJsonSourceBuilder.java build) when the field survives into that shared source model,
or None when it is dropped before FML ever sees it (see XML_TO_LM_LEAF below) - a genuinely
unmapped field, shown in the tree but never connected to anything on the target side.

Hand-transcribed from lm_diagnosis_json_builder.py's _build_header/_build_body (not re-derived
generically): that module is the single source of truth for which RIVTA field becomes which
lm-diagnosis field, and it is short and fully deterministic, so mirroring it by hand here is
more direct - and no less trustworthy - than writing a second small parser for it.
"""
from lxml import etree


def _local_tag(elem):
    return etree.QName(elem).localname


def _find_child(elem, tag):
    if elem is None:
        return None
    for c in elem:
        if _local_tag(c) == tag:
            return c
    return None


def _text(elem, tag):
    c = _find_child(elem, tag)
    return c.text.strip() if c is not None and c.text else None


def _leaf(path, label, value, lm_path):
    return {"kind": "leaf", "name": label, "path": path, "value": value, "lm_path": lm_path, "children": []}


def _obj(path, label, children):
    return {"kind": "object", "name": label, "path": path, "value": None, "lm_path": None, "children": children}


def _p(base, seg):
    return "%s.%s" % (base, seg) if base else seg


def build_header_node(header_elem, diag_path, lm_diag_path):
    path = _p(diag_path, "diagnosisHeader")
    lm = _p(lm_diag_path, "diagnosisHeader")
    children = []

    doc_id = _text(header_elem, "documentId")
    if doc_id is not None:
        children.append(_leaf(_p(path, "documentId"), "documentId", doc_id, _p(lm, "documentId")))

    src_hsa = _text(header_elem, "sourceSystemHSAId")
    if src_hsa is not None:
        children.append(_leaf(_p(path, "sourceSystemHSAId"), "sourceSystemHSAId", src_hsa,
                               _p(lm, "sourceSystemHSAId.value")))

    pid_elem = _find_child(header_elem, "patientId")
    if pid_elem is not None:
        pid_path, pid_lm = _p(path, "patientId"), _p(lm, "patientId")
        pid_children = []
        v = _text(pid_elem, "id")
        if v is not None:
            pid_children.append(_leaf(_p(pid_path, "id"), "id", v, _p(pid_lm, "value")))
        t = _text(pid_elem, "type")
        if t is not None:
            pid_children.append(_leaf(_p(pid_path, "type"), "type", t, _p(pid_lm, "system")))
        children.append(_obj(pid_path, "patientId", pid_children))

    ahp_elem = _find_child(header_elem, "accountableHealthcareProfessional")
    if ahp_elem is not None:
        ahp_path, ahp_lm = _p(path, "accountableHealthcareProfessional"), _p(lm, "accountableHealthcareProfessional")
        ahp_children = []
        v = _text(ahp_elem, "authorTime")
        if v is not None:
            ahp_children.append(_leaf(_p(ahp_path, "authorTime"), "authorTime", v, _p(ahp_lm, "authorTime")))
        for field in ("healthcareProfessionalHSAId", "healthcareProfessionalCareUnitHSAId",
                      "healthcareProfessionalCareGiverHSAId"):
            v = _text(ahp_elem, field)
            if v is not None:
                ahp_children.append(_leaf(_p(ahp_path, field), field, v, _p(ahp_lm, field + ".value")))
        v = _text(ahp_elem, "healthcareProfessionalName")
        if v is not None:
            # Built into the lm-diagnosis JSON too, but never consumed by either .map file -
            # a genuine dead end, shown with lm_path=None so the UI can mark it "unmapped".
            ahp_children.append(_leaf(_p(ahp_path, "healthcareProfessionalName"), "healthcareProfessionalName", v, None))
        children.append(_obj(ahp_path, "accountableHealthcareProfessional", ahp_children))

    la_elem = _find_child(header_elem, "legalAuthenticator")
    if la_elem is not None:
        la_path, la_lm = _p(path, "legalAuthenticator"), _p(lm, "legalAuthenticator")
        la_children = []
        v = _text(la_elem, "signatureTime")
        if v is not None:
            la_children.append(_leaf(_p(la_path, "signatureTime"), "signatureTime", v, _p(la_lm, "signatureTime")))
        v = _text(la_elem, "legalAuthenticatorHSAId")
        if v is not None:
            la_children.append(_leaf(_p(la_path, "legalAuthenticatorHSAId"), "legalAuthenticatorHSAId", v,
                                      _p(la_lm, "legalAuthenticatorHSAId.value")))
        v = _text(la_elem, "legalAuthenticatorName")
        if v is not None:
            la_children.append(_leaf(_p(la_path, "legalAuthenticatorName"), "legalAuthenticatorName", v, None))
        children.append(_obj(la_path, "legalAuthenticator", la_children))

    return _obj(path, "diagnosisHeader", children)


def build_body_node(body_elem, diag_path, lm_diag_path):
    path = _p(diag_path, "diagnosisBody")
    lm = _p(lm_diag_path, "diagnosisBody")
    children = []

    v = _text(body_elem, "typeOfDiagnosis")
    if v is not None:
        children.append(_leaf(_p(path, "typeOfDiagnosis"), "typeOfDiagnosis", v,
                               _p(lm, "typeOfDiagnosis.coding[0].code")))

    v = _text(body_elem, "chronicDiagnosis")
    if v is not None:
        children.append(_leaf(_p(path, "chronicDiagnosis"), "chronicDiagnosis", v, _p(lm, "chronicDiagnosis.value")))

    v = _text(body_elem, "diagnosisTime")
    if v is not None:
        children.append(_leaf(_p(path, "diagnosisTime"), "diagnosisTime", v, _p(lm, "diagnosisTime")))

    dc_elem = _find_child(body_elem, "diagnosisCode")
    if dc_elem is not None:
        dc_path, dc_lm = _p(path, "diagnosisCode"), _p(lm, "diagnosisCode")
        dc_children = []
        v = _text(dc_elem, "code")
        if v is not None:
            dc_children.append(_leaf(_p(dc_path, "code"), "code", v, _p(dc_lm, "coding[0].code")))
        v = _text(dc_elem, "codeSystem")
        if v is not None:
            dc_children.append(_leaf(_p(dc_path, "codeSystem"), "codeSystem", v, _p(dc_lm, "coding[0].system")))
        v = _text(dc_elem, "codeSystemName")
        if v is not None:
            # Read from the XML but never even carried into the lm-diagnosis JSON (see
            # lm_diagnosis_json_builder.py's _build_body) - a dead end one step earlier than
            # the Name fields above.
            dc_children.append(_leaf(_p(dc_path, "codeSystemName"), "codeSystemName", v, None))
        v = _text(dc_elem, "displayName")
        if v is not None:
            dc_children.append(_leaf(_p(dc_path, "displayName"), "displayName", v, _p(dc_lm, "coding[0].display")))
        v = _text(dc_elem, "originalText")
        if v is not None:
            dc_children.append(_leaf(_p(dc_path, "originalText"), "originalText", v, _p(dc_lm, "text")))
        children.append(_obj(dc_path, "diagnosisCode", dc_children))

    rel_idx = 0
    for rel_elem in body_elem:
        if _local_tag(rel_elem) != "relatedDiagnosis":
            continue
        rel_path = "%s[%d]" % (_p(path, "relatedDiagnosis"), rel_idx)
        rel_lm = "%s[%d]" % (_p(lm, "relatedDiagnosis"), rel_idx)
        rel_children = []
        v = _text(rel_elem, "documentId")
        if v is not None:
            rel_children.append(_leaf(_p(rel_path, "documentId"), "documentId", v, _p(rel_lm, "documentId")))
        children.append(_obj(rel_path, "relatedDiagnosis[%d]" % rel_idx, rel_children))
        rel_idx += 1

    return _obj(path, "diagnosisBody", children)


def flatten_lm_values(tree):
    """Flattens a diagnosis tree (see build_diagnosis_tree) into {lm_path: raw_xml_value} for
    every leaf that survives into the shared lm-diagnosis source model (lm_path is not None -
    a genuinely unmapped field has none and is skipped). Used by lineage.py's value-matching
    layer (value_match.py) to look up the RAW XML value a given static/driver edge's
    source_path leaf actually carried for THIS record, so it can compute that rule's known
    transform (if any) and search the REAL output JSON for the result - see value_match.py's
    module docstring."""
    out = {}

    def walk(node):
        if node["kind"] == "leaf":
            if node["lm_path"] is not None:
                out[node["lm_path"]] = node["value"]
        else:
            for child in node["children"]:
                walk(child)

    walk(tree)
    return out


def build_diagnosis_tree(diag_elem, index):
    """One <diagnosis> entry's full tree, rooted at "diagnosis[<index>]" (the SAME index the
    chosen fixture's <diagnosis> entry has among its siblings - see server.py)."""
    diag_path = "diagnosis[%d]" % index
    children = []
    header_elem = _find_child(diag_elem, "diagnosisHeader")
    if header_elem is not None:
        children.append(build_header_node(header_elem, diag_path, diag_path))
    body_elem = _find_child(diag_elem, "diagnosisBody")
    if body_elem is not None:
        children.append(build_body_node(body_elem, diag_path, diag_path))
    return _obj(diag_path, "diagnosis[%d]" % index, children)
