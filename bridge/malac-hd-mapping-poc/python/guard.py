"""DIAG-001 "stop the resource" guard, run BEFORE either compiled StructureMap transform.

Mirrors the record-level guards in GetDiagnosisMapper.map()/mapDiagnosis() that have nothing
to do with field-by-field RIVTA->FHIR mapping and therefore do not belong in FML at all (the
same architectural split the HAPI/FML track adopted - see fml-evaluation.md's
"FmlEngine.stopIfFalseFhirPath"):

  1. result.resultCode present and != "OK"              -> the whole response yields no output.
  2. diagnosisHeader or diagnosisBody missing            -> that diagnosis entry is dropped.
  3. patientId missing, or patientId/id does not match
     ^\\d{12}$ (personnummer/samordningsnummer without
     a dash: YYYYMMDD + 4 digits)                        -> that diagnosis entry is dropped,
                                                              because a Condition without a
                                                              trustworthy patient identifier
                                                              cannot be delivered (DIAG-001).

Deliberately NOT reimplemented here: GetDiagnosisMapper.mapDiagnosis()'s VG-scope filter
(ctx.getRequestedVgHsaId()) - that is bridge-deployment context, not a RIVTA/FHIR mapping
concern, and out of scope for this proof of concept (see malac-hd-evaluation.md).
"""
import re
from lxml import etree

PERSONNUMMER_PATTERN = re.compile(r"^\d{12}$")


def _local_tag(elem):
    return etree.QName(elem).localname


def _find_child(elem, tag_name):
    for child in elem:
        if _local_tag(child) == tag_name:
            return child
    return None


def valid_diagnoses_in_root(root):
    """Same as valid_diagnoses(), but takes an ALREADY-parsed lxml root element, so the
    caller's own single parse of the file stays the source of truth (identity-safe for
    further slicing) instead of this module re-parsing the file a second time."""
    result = None
    for elem in root.iter():
        if _local_tag(elem) == "result":
            result = elem
            break
    if result is not None:
        result_code = _find_child(result, "resultCode")
        if result_code is not None and (result_code.text or "").strip() != "OK":
            return []

    kept = []
    for diag in root:
        if _local_tag(diag) != "diagnosis":
            continue
        header = _find_child(diag, "diagnosisHeader")
        body = _find_child(diag, "diagnosisBody")
        if header is None or body is None:
            continue
        patient_id = _find_child(header, "patientId")
        if patient_id is None:
            continue
        id_elem = _find_child(patient_id, "id")
        if id_elem is None or not PERSONNUMMER_PATTERN.match((id_elem.text or "").strip()):
            continue
        kept.append(diag)
    return kept


def valid_diagnoses(xml_path):
    """Returns the list of <diagnosis> elements (lxml) that pass the DIAG-001 guard, or []
    if the whole response is dropped (missing/non-OK resultCode). Parses xml_path itself -
    the elements returned belong to a tree private to this call; a caller that needs to slice
    the SAME tree further (run_pipeline.py) should use valid_diagnoses_in_root() instead."""
    tree = etree.parse(xml_path)
    return valid_diagnoses_in_root(tree.getroot())
