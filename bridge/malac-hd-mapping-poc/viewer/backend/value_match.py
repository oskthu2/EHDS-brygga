"""Oskar's "traceability anchor" idea (see the PR task description for his own words),
implemented as VALUE-MATCHING rather than literal marker injection.

A literal, arbitrary marker string cannot be injected into every field: several target
fields are type- or terminology-validated (a `boolean`, a `dateTime`, a ConceptMap-translated
`code` that must exist in the real code system, a syntactically-valid `uri`) - a fake sentinel
value would either fail FHIR type validation or fail to match in a real ConceptMap
`translate()` lookup, so it would stop tracing the REAL mapping behaviour and start tracing a
broken one instead.

What travels with the value instead, for fields this module covers, is simply THE VALUE
ITSELF (after fixture-uniqueness - see ../../src/test/resources/testmessages/*.xml - makes
every leaf distinct within one fixture) or its REAL, known, FHIR-type-safe transform. This
module answers one question: "given the RAW XML leaf value a source_tree.py leaf carries,
what should the REAL, unmodified pipeline's output contain for it?" - lineage.py then
searches the REAL output JSON (from an actually-executed, unmodified run, on EITHER engine)
for that exact value, at the exact candidate position(s) its `target_path` pattern already
narrows the search to (see lineage.py's `_disambiguate`/`resolve_target_path`). A match found
this way is the most trustworthy trace edge this viewer can produce: it never inspects engine
internals, never instruments anything, and is attested by a real, unmodified execution on
whichever engine produced the output.

Every transform below REUSES the exact same conversion code the real pipeline already runs,
rather than re-deriving an independent second guess at what it does:
  - riv_date.riv_to_offset_datetime / riv_to_utc_instant / oid_to_uri / TJANSTEKATALOG_BASE -
    the SAME module lm_diagnosis_json_builder.py (the real source-JSON builder both engines'
    drivers feed into the real transform) already imports and runs against this exact raw
    value - see that module's own docstring.

Deliberately NOT covered here: ConceptMap/translate()-driven fields (currently just
"category-mapped" - typeOfDiagnosis -> category[].coding via the diagnosis-type ConceptMap).
Oskar's own follow-up (relayed via the orchestrating session) asked that these stay on the
existing static-rule-based trace at WHOLE-ELEMENT granularity: the .map file's translate()
call plus the ConceptMap resource IS the authoritative definition of where a coded element
lands, so re-verifying it by searching for a specific sub-field's value would be solving an
already-solved problem with a less reliable technique, not strengthening it. See
lineage.py's resolve_edges(), which skips value-matching for `kind == "translate"` edges
outright for exactly this reason.
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
PYTHON_DIR = os.path.join(HERE, "..", "..", "python")
if PYTHON_DIR not in sys.path:
    sys.path.insert(0, PYTHON_DIR)
import riv_date  # noqa: E402

_UNSET = object()


def _endpoint_url(raw_hsa_id: str):
    """driver-meta-source: Condition.meta.source is the FULL tjanstekatalogen Endpoint URL,
    not the bare HSA-id - mirrors lm_diagnosis_json_builder.py's _build_header() exactly
    (TJANSTEKATALOG_BASE + "/Endpoint/" + raw), the SAME construction the real source-JSON
    builder already does with this exact raw value before either engine ever runs."""
    return riv_date.TJANSTEKATALOG_BASE + "/Endpoint/" + raw_hsa_id


def _bool_value(raw: str):
    """chronicDiagnosis: "true"/"false" RIVTA text -> a real JSON boolean - mirrors
    lm_diagnosis_json_builder.py's _build_body() ("chronic.lower() == 'true'")."""
    return raw.strip().lower() == "true"


# One real transform function per FML/driver RULE NAME whose final output value differs from
# the raw XML leaf text - see this module's docstring for why each one reuses riv_date.py
# rather than reimplementing the conversion a second, independent way. Every rule NOT listed
# here is a plain, unmodified passthrough (documentId, every HSA-id copied as itself, the
# ICD-10 code/display/text fields, relatedDiagnosis's documentId) - identity is the default in
# expected_value() below, not an entry here.
RULE_TRANSFORMS = {
    # Condition.recordedDate / Condition.onsetDateTime / the ext-asserted-date extension's
    # valueDateTime: all three are accountableHealthcareProfessional.authorTime /
    # diagnosisBody.diagnosisTime / legalAuthenticator.signatureTime converted to an explicit-
    # offset ISO-8601 dateTime (GENERAL-001) - see get-diagnosis-to-condition.map's header
    # comment and driver-fields.json.
    "recordedDate": riv_date.riv_to_offset_datetime,
    "driver-onset": riv_date.riv_to_offset_datetime,
    "assertedDate-value": riv_date.riv_to_offset_datetime,
    # Provenance.recorded: the SAME authorTime raw value, but as a TRUE UTC instant instead -
    # a genuinely different conversion of the same source leaf (see driver-fields.json's own
    # description of "driver-recorded").
    "driver-recorded": riv_date.riv_to_utc_instant,
    # The three HAPI-only-bug "uri" fields moved out of FML entirely (see the .map file header
    # comments) - Condition.meta.source is the full Endpoint URL; the other two are a bare
    # OID->URI resolution.
    "driver-meta-source": _endpoint_url,
    "driver-subject-identifier-system": riv_date.oid_to_uri,
    "driver-code-coding-system": riv_date.oid_to_uri,
    # chronicDiagnosis's extension value is a real FHIR boolean, not the RIVTA "true"/"false"
    # string.
    "chronic-value": _bool_value,
}


def expected_value(rule: str, raw_value):
    """Returns the value the REAL, unmodified transform should produce for `raw_value` (a
    source_tree.py leaf's raw XML text) under the FML/driver rule named `rule` - identity
    passthrough unless `rule` has a known, non-identity transform (see RULE_TRANSFORMS above).
    Returns None when no value-match attempt should be made for this edge at all (no raw
    value to begin with, or the transform itself fails on this input) - lineage.py then falls
    back to its existing static/heuristic/runtime-hint resolution unchanged, exactly as if
    this module did not exist."""
    if raw_value is None:
        return None
    fn = RULE_TRANSFORMS.get(rule)
    if fn is None:
        return raw_value
    try:
        return fn(raw_value)
    except Exception:
        return None
