"""Driver: parse RIVTA XML -> DIAG-001 guard -> build the SHARED lm-diagnosis-shaped JSON
source (lm_diagnosis_json_builder.py) -> compiled StructureMap transforms (Condition +
Provenance) -> post-process Provenance.target/.recorded, one call per GetDiagnosisResponse
fixture. Mirrors GetDiagnosisMapper.map()/mapDiagnosis() end to end, including the two pieces
that could not live inside FML at all for this engine (see guard.py and
lm_diagnosis_json_builder.py/riv_date.py module docstrings):
  - the record-level guards (DIAG-001, non-OK resultCode, missing header/body)
  - Provenance.target (needs the sibling Condition's own generated id - a real StructureMap
    transform() only ever has ONE target, see get-diagnosis-to-provenance.map's header comment
    and fml-evaluation.md's "en transform() tar bara EN target", confirmed true for MaLaC-HD
    too) and Provenance.recorded (needs authorTime converted to a TRUE UTC instant, a
    different conversion of the SAME raw field than Condition.recordedDate needs - see
    ProvenanceBuilder.build() vs. GetDiagnosisMapper.java).
  - Condition.onset[x] / the ext-asserted-date extension's value[x] (irreconcilable dateTime-
    choice-element conflict) and Condition.meta.source / subject.identifier.system /
    code.coding[0].system (a confirmed HAPI-only bug for these three "uri" fields) - see
    get-diagnosis-to-condition.map's header comment for both.

Only a SINGLE <diagnosis> entry per GetDiagnosisResponse is exercised end to end by each run
of the compiled transform (a StructureMap transform() takes one source and one target - see
the comment above); a response with several <diagnosis> entries is run once per surviving
entry, slicing a single-diagnosis document for each, exactly mirroring how
GetDiagnosisMapper.map() calls mapDiagnosis() once per Diagnosis and collects the results.

The compiled transforms are invoked exactly as before (subprocess, -s/-t file paths), but the
SOURCE file is now JSON (lm-diagnosis-shaped), not RIVTA XML, even though the CLI call still
says "-st xml" - see lm_diagnosis_model.py's module docstring and malac-hd-evaluation.md, "En
tolkningsgenvag: 'xml' ljuger om formatet", for exactly why that is correct: the flag only
selects which _read_source() branch the compiled wrapper takes (it always calls
"<module>.parse(src, silence=True)" for 'xml'), never validates the file's actual content, and
lm_diagnosis_model.parse() reads real JSON regardless of the label.
"""
import copy
import json
import os
import subprocess
import sys
import tempfile
import uuid
from pathlib import Path

from lxml import etree

import guard
import lm_diagnosis_json_builder
import riv_date

HERE = Path(__file__).resolve().parent
COMPILED_CONDITION = HERE / "compiled" / "get-diagnosis-to-condition.py"
COMPILED_PROVENANCE = HERE / "compiled" / "get-diagnosis-to-provenance.py"
MODEL_DIR = HERE / "model"


def _local_tag(elem):
    return etree.QName(elem).localname


def _find_child(elem, tag_name):
    for child in elem:
        if _local_tag(child) == tag_name:
            return child
    return None


def _single_diagnosis_tree(root, diagnosis_index):
    """Clones the response root, keeping only the <diagnosis> element at diagnosis_index among
    root's own <diagnosis> children (plus <result>) - one compiled transform() invocation
    only ever has ONE target, see the module docstring."""
    new_root = copy.deepcopy(root)
    diagnoses = [c for c in new_root if _local_tag(c) == "diagnosis"]
    for i, child in enumerate(diagnoses):
        if i != diagnosis_index:
            new_root.remove(child)
    return new_root


def _run_compiled(script_path, source_json, output_json_path, tmp_dir, tag):
    env = dict(os.environ)
    env["PYTHONPATH"] = str(MODEL_DIR) + os.pathsep + env.get("PYTHONPATH", "")
    source_path = tmp_dir / ("%s_source.json" % tag)
    with open(source_path, "w", encoding="utf-8") as fh:
        json.dump(source_json, fh, ensure_ascii=False)
    result = subprocess.run(
        [sys.executable, str(script_path),
         "-s", str(source_path), "-t", str(output_json_path),
         "-st", "xml", "-tt", "json"],
        cwd=str(HERE),
        env=env,
        capture_output=True, text=True,
    )
    if result.returncode != 0:
        raise RuntimeError(
            "Compiled transform %s failed:\nSTDOUT:\n%s\nSTDERR:\n%s"
            % (script_path, result.stdout, result.stderr)
        )
    with open(output_json_path) as f:
        return json.load(f)


def _extract_author_time_raw(diagnosis_elem):
    header = _find_child(diagnosis_elem, "diagnosisHeader")
    ahp = _find_child(header, "accountableHealthcareProfessional") if header is not None else None
    author_time = _find_child(ahp, "authorTime") if ahp is not None else None
    return author_time.text if author_time is not None else None


def _extract_diagnosis_time_raw(diagnosis_elem):
    body = _find_child(diagnosis_elem, "diagnosisBody")
    diagnosis_time = _find_child(body, "diagnosisTime") if body is not None else None
    return diagnosis_time.text if diagnosis_time is not None else None


def _extract_signature_time_raw(diagnosis_elem):
    header = _find_child(diagnosis_elem, "diagnosisHeader")
    la = _find_child(header, "legalAuthenticator") if header is not None else None
    signature_time = _find_child(la, "signatureTime") if la is not None else None
    return signature_time.text if signature_time is not None else None


ASSERTED_DATE_EXTENSION_URL = "https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-asserted-date"


def _set_onset_and_asserted_date(condition, raw_diagnosis_time, raw_signature_time):
    """Sets Condition.onsetDateTime and the ext-asserted-date extension's valueDateTime
    directly on the transform() RESULT, exactly like Provenance.target/.recorded are set
    outside FML - see get-diagnosis-to-condition.map's header comment for why NEITHER engine
    can be asked to set a dateTime-typed CHOICE element sourced from a custom model: a
    confirmed, irreconcilable conflict between MaLaC-HD and HAPI, found by actually testing
    every combination against both."""
    if raw_diagnosis_time:
        condition["onsetDateTime"] = riv_date.riv_to_offset_datetime(raw_diagnosis_time)
    if raw_signature_time:
        for ext in condition.get("extension", []):
            if ext.get("url") == ASSERTED_DATE_EXTENSION_URL:
                ext["valueDateTime"] = riv_date.riv_to_offset_datetime(raw_signature_time)


def _set_uri_fields(condition, cond_source):
    """Sets Condition.meta.source, Condition.subject.identifier.system and
    Condition.code.coding[0].system directly on the transform() RESULT, reading the already-
    resolved values straight back out of cond_source (the SAME dict the compiled transform
    just consumed - no re-derivation needed). See get-diagnosis-to-condition.map's header
    comment: a confirmed HAPI-only bug corrupts these three "uri" fields when FML sets them
    via create('uri') from a navigated (non-literal) source Element - MaLaC-HD itself has no
    such bug, but the file is shared, so these three moved out of FML on both sides, exactly
    like onset/assertedDate above."""
    diag = cond_source.get("diagnosis", [{}])[0]
    header = diag.get("diagnosisHeader", {})
    body = diag.get("diagnosisBody", {})

    source_hsa = header.get("sourceSystemHSAId")
    if source_hsa and source_hsa.get("value") and "meta" in condition:
        condition["meta"]["source"] = source_hsa["value"]

    patient_id = header.get("patientId")
    if patient_id and patient_id.get("system"):
        condition.get("subject", {}).get("identifier", {})["system"] = patient_id["system"]

    diagnosis_code = body.get("diagnosisCode")
    if diagnosis_code and diagnosis_code.get("coding"):
        coding = diagnosis_code["coding"][0]
        if coding.get("system") and "code" in condition:
            condition["code"]["coding"][0]["system"] = coding["system"]


def map_get_diagnosis_response(xml_path):
    """Returns a list of {"condition": <dict>, "provenance": <dict>} - one per surviving
    <diagnosis> entry, after the DIAG-001 guard (see guard.py). An empty list means either a
    non-OK resultCode, or every entry failed the guard - exactly like
    GetDiagnosisMapper.map()'s List.of()."""
    tree = etree.parse(xml_path)
    root = tree.getroot()

    all_diagnoses = [c for c in root if _local_tag(c) == "diagnosis"]
    kept = guard.valid_diagnoses_in_root(root)
    kept_indices = [all_diagnoses.index(d) for d in kept]
    entries = []

    with tempfile.TemporaryDirectory() as tmp:
        tmp = Path(tmp)
        for i, diag_index in enumerate(kept_indices):
            single_root = _single_diagnosis_tree(root, diag_index)
            single_diag = [c for c in single_root if _local_tag(c) == "diagnosis"][0]
            raw_author_time = _extract_author_time_raw(single_diag)
            raw_diagnosis_time = _extract_diagnosis_time_raw(single_diag)
            raw_signature_time = _extract_signature_time_raw(single_diag)

            # Condition pass: authorTime -> offset dateTime (recordedDate).
            cond_source = lm_diagnosis_json_builder.build_source(single_diag, author_time_as_instant=False)
            cond_out = tmp / ("cond_out_%d.json" % i)
            condition = _run_compiled(COMPILED_CONDITION, cond_source, cond_out, tmp, "cond_%d" % i)

            condition_id = str(uuid.uuid4())
            condition["id"] = condition_id
            _set_onset_and_asserted_date(condition, raw_diagnosis_time, raw_signature_time)
            _set_uri_fields(condition, cond_source)

            # Provenance pass: authorTime -> TRUE UTC instant (Provenance.recorded).
            prov_source = lm_diagnosis_json_builder.build_source(single_diag, author_time_as_instant=True)
            prov_out = tmp / ("prov_out_%d.json" % i)
            provenance = _run_compiled(COMPILED_PROVENANCE, prov_source, prov_out, tmp, "prov_%d" % i)

            provenance["id"] = str(uuid.uuid4())
            provenance["target"] = [{"reference": "urn:uuid:" + condition_id}]
            if raw_author_time:
                provenance["recorded"] = riv_date.riv_to_utc_instant(raw_author_time)

            entries.append({"condition": condition, "provenance": provenance})

    return entries


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("usage: python3 run_pipeline.py <GetDiagnosisResponse.xml>", file=sys.stderr)
        sys.exit(1)
    result = map_get_diagnosis_response(sys.argv[1])
    print(json.dumps(result, indent=2, ensure_ascii=False))
