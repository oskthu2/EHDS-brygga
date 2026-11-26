"""Builds ONE JSON instance of the EHDS-TK logical model inera-ehds-lm-diagnosis
(../fhir/lm-diagnosis.json) from a RIVTA GetDiagnosis <diagnosis> element - the Python mirror
of se.inera.ehds.malac.hapi.LmDiagnosisJsonSourceBuilder.java (this module's src/main/java),
which builds the SAME shape for the Java/HAPI engine. See malac-hd-evaluation.md, "Den enade
kallmodellen", for why this replaced preprocess.py's in-place XML-text-rewriting approach: the
old model's fields were flat strings (PersonIdType.id/.type, CVType.code/.codeSystem/...), so
rewriting a few <codeSystem>/<type> text nodes in place was enough; inera-ehds-lm-diagnosis
reshapes those same values into nested Identifier ({value, system}) and CodeableConcept
({coding: [{system, code, display}], text}) structures, which an in-place text rewrite cannot
produce - a real tree-to-tree builder is needed instead, exactly like the Java side already
has one.

Two conversions still happen here, in Python, rather than in either .map file - not because of
a MaLaC-HD-only engine bug this time (the old malac-hd-evaluation.md reasons still apply, see
module riv_date.py), but because this is now a SHARED-FILE constraint: the "&" string-
concatenation bug (malac-hd-evaluation.md, point 6 in the original list) turns out to be keyed
on "value sourced from a non-built-in-FHIR model", not on the specific old RivtaResponse model
- confirmed unchanged after the model switch (see riv_date.py's module docstring and
malac-hd-evaluation.md, "Vilka buggar overlevde modellbytet"). Since
inera-ehds-lm-diagnosis is ALSO a non-built-in-FHIR (logical) model, "&" with a model-sourced
variable is just as broken as before. HAPI's own lm-diagnosis-sourced map (PR #39,
fml-mapping-poc) has no such bug and DOES build these two values with FML "&" directly - but a
file meant to compile byte-for-byte identically on BOTH engines cannot rely on that, so both
JSON builders (this one and the Java one) now do it up front instead:
  - patientId.system / diagnosisCode.coding[].system: resolved from a bare RIVTA OID to its
    URI (urn:oid:<oid> fallback for an unknown OID) - previously only patientId.type and
    diagnosisCode.codeSystem were pre-resolved; diagnosisCode's OID is now ALSO pre-resolved
    (it used to go through the codesystem-oid ConceptMap/translate() for the mapped case,
    with "&" only for the fallback) - see compile.sh/python/fml/get-diagnosis-to-condition.map
    for why that ConceptMap is no longer referenced by the shared map.
  - diagnosisHeader.sourceSystemHSAId.value: rewritten to the full tjanstekatalogen Endpoint
    URL (meta.source's final value) - unchanged from before, just moved from an XML text
    rewrite to a JSON field.

GENERAL-001 (Europe/Stockholm -> explicit-offset dateTime / true UTC instant) is unchanged in
substance - see riv_date.py - and applies regardless of engine or source model either way:
FML/FHIRPath has no timezone database, full stop.
"""
from lxml import etree

from riv_date import oid_to_uri, riv_to_offset_datetime, riv_to_utc_instant, TJANSTEKATALOG_BASE


def _local_tag(elem):
    return etree.QName(elem).localname


def _find_child(elem, tag_name):
    if elem is None:
        return None
    for child in elem:
        if _local_tag(child) == tag_name:
            return child
    return None


def _text(elem, tag_name):
    child = _find_child(elem, tag_name)
    return child.text.strip() if child is not None and child.text else None


def _identifier(value, system=None):
    """Identifier-shaped dict ({value, system}) - mirrors
    LmDiagnosisJsonSourceBuilder.identifier(...). None fields are simply omitted, same
    convention the Java builder uses (Gson JsonObject only gets properties actually added)."""
    if value is None:
        return None
    out = {"value": value}
    if system is not None:
        out["system"] = system
    return out


def _codeable_concept(system, code, display, text):
    """CodeableConcept-shaped dict ({coding: [{system, code, display}], text}) - mirrors
    LmDiagnosisJsonSourceBuilder.codeableConcept(...). A coding is only added when there is a
    code to carry (typeOfDiagnosis always has one; diagnosisCode's own absence is handled by
    the caller)."""
    if code is None and system is None and display is None:
        return None
    coding = {}
    if system is not None:
        coding["system"] = system
    if code is not None:
        coding["code"] = code
    if display is not None:
        coding["display"] = display
    cc = {"coding": [coding]}
    if text is not None:
        cc["text"] = text
    return cc


def _build_header(header_elem, *, author_time_as_instant):
    h = {}
    document_id = _text(header_elem, "documentId")
    if document_id is not None:
        h["documentId"] = document_id

    source_hsa = _text(header_elem, "sourceSystemHSAId")
    if source_hsa is not None:
        h["sourceSystemHSAId"] = _identifier(TJANSTEKATALOG_BASE + "/Endpoint/" + source_hsa)

    patient_id_elem = _find_child(header_elem, "patientId")
    if patient_id_elem is not None:
        pid_value = _text(patient_id_elem, "id")
        pid_type = _text(patient_id_elem, "type")
        if pid_value is not None:
            h["patientId"] = _identifier(pid_value, oid_to_uri(pid_type) if pid_type else None)

    ahp_elem = _find_child(header_elem, "accountableHealthcareProfessional")
    if ahp_elem is not None:
        ahp = {}
        author_time = _text(ahp_elem, "authorTime")
        if author_time is not None:
            ahp["authorTime"] = (
                riv_to_utc_instant(author_time) if author_time_as_instant
                else riv_to_offset_datetime(author_time)
            )
        for field, tag in (
            ("healthcareProfessionalHSAId", "healthcareProfessionalHSAId"),
            ("healthcareProfessionalCareUnitHSAId", "healthcareProfessionalCareUnitHSAId"),
            ("healthcareProfessionalCareGiverHSAId", "healthcareProfessionalCareGiverHSAId"),
        ):
            value = _text(ahp_elem, tag)
            if value is not None:
                ahp[field] = _identifier(value)
        name = _text(ahp_elem, "healthcareProfessionalName")
        if name is not None:
            ahp["healthcareProfessionalName"] = name
        h["accountableHealthcareProfessional"] = ahp

    la_elem = _find_child(header_elem, "legalAuthenticator")
    if la_elem is not None:
        la = {}
        signature_time = _text(la_elem, "signatureTime")
        if signature_time is not None:
            la["signatureTime"] = riv_to_offset_datetime(signature_time)
        la_hsa = _text(la_elem, "legalAuthenticatorHSAId")
        if la_hsa is not None:
            la["legalAuthenticatorHSAId"] = _identifier(la_hsa)
        la_name = _text(la_elem, "legalAuthenticatorName")
        if la_name is not None:
            la["legalAuthenticatorName"] = la_name
        h["legalAuthenticator"] = la

    return h


def _build_body(body_elem):
    b = {}
    type_of_diagnosis = _text(body_elem, "typeOfDiagnosis")
    if type_of_diagnosis is not None:
        b["typeOfDiagnosis"] = _codeable_concept(None, type_of_diagnosis, None, None)

    chronic = _text(body_elem, "chronicDiagnosis")
    if chronic is not None:
        b["chronicDiagnosis"] = chronic.lower() == "true"

    diagnosis_time = _text(body_elem, "diagnosisTime")
    if diagnosis_time is not None:
        b["diagnosisTime"] = riv_to_offset_datetime(diagnosis_time)

    dc_elem = _find_child(body_elem, "diagnosisCode")
    if dc_elem is not None:
        code = _text(dc_elem, "code")
        code_system = _text(dc_elem, "codeSystem")
        display = _text(dc_elem, "displayName")
        original_text = _text(dc_elem, "originalText")
        b["diagnosisCode"] = _codeable_concept(
            oid_to_uri(code_system) if code_system else None, code, display, original_text,
        )

    related = []
    for rel_elem in body_elem:
        if _local_tag(rel_elem) != "relatedDiagnosis":
            continue
        rel_doc_id = _text(rel_elem, "documentId")
        if rel_doc_id is not None:
            related.append({"documentId": rel_doc_id})
    if related:
        b["relatedDiagnosis"] = related

    return b


def build_diagnosis(diag_elem, *, author_time_as_instant=False):
    """One lm-diagnosis "diagnosis[]" entry (a dict, JSON-serializable) for the given RIVTA
    <diagnosis> lxml element. author_time_as_instant: Provenance.recorded needs the true UTC
    instant form of accountableHealthcareProfessional.authorTime, while Condition.recordedDate
    needs the local-offset dateTime form of that SAME raw field - see
    LmDiagnosisJsonSourceBuilder.toJson(..., recordedAsInstant) and ProvenanceBuilder.build()
    vs. GetDiagnosisMapper.java."""
    d = {}
    header_elem = _find_child(diag_elem, "diagnosisHeader")
    body_elem = _find_child(diag_elem, "diagnosisBody")
    if header_elem is not None:
        d["diagnosisHeader"] = _build_header(header_elem, author_time_as_instant=author_time_as_instant)
    if body_elem is not None:
        d["diagnosisBody"] = _build_body(body_elem)
    return d


def build_source(diag_elem, *, author_time_as_instant=False):
    """The full lm-diagnosis source document ({"diagnosis": [...]})  - one entry, matching how
    run_pipeline.py slices a single <diagnosis> per compiled-transform call (a StructureMap
    transform() only ever has one target; see get-diagnosis-to-provenance.map's header
    comment)."""
    return {"diagnosis": [build_diagnosis(diag_elem, author_time_as_instant=author_time_as_instant)]}
