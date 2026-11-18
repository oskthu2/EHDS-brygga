"""Driver: preprocess -> DIAG-001 guard -> compiled StructureMap transforms (Condition +
Provenance), one call per GetDiagnosisResponse fixture. Mirrors
GetDiagnosisMapper.map()/mapDiagnosis() end to end, including the two pieces that could not
live inside FML at all for this engine (see preprocess.py and guard.py module docstrings):
  - the record-level guards (DIAG-001, non-OK resultCode, missing header/body)
  - Provenance.target (needs the sibling Condition's own generated id - a real StructureMap
    transform() only ever has ONE target, see get-diagnosis-to-provenance.map's header comment
    and fml-evaluation.md's "en transform() tar bara EN target", confirmed true for MaLaC-HD
    too) and Provenance.recorded (needs authorTime converted to a TRUE UTC instant, a
    different conversion of the SAME raw field than Condition.recordedDate needs - see
    ProvenanceBuilder.build() vs. GetDiagnosisMapper.java).

Only a SINGLE <diagnosis> entry per GetDiagnosisResponse is exercised end to end by each run
of the compiled transform (a StructureMap transform() takes one source and one target - see
the comment above); a response with several <diagnosis> entries is run once per surviving
entry, slicing a single-diagnosis document for each, exactly mirroring how
GetDiagnosisMapper.map() calls mapDiagnosis() once per Diagnosis and collects the results.
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
import preprocess

HERE = Path(__file__).resolve().parent
COMPILED_CONDITION = HERE / "compiled" / "get-diagnosis-to-condition.py"
COMPILED_PROVENANCE = HERE / "compiled" / "get-diagnosis-to-provenance.py"
MODEL_DIR = HERE / "model"

RESPONSE_TAG_NS = "urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2"


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


def _run_compiled(script_path, input_xml_path, output_json_path):
    env = dict(os.environ)
    env["PYTHONPATH"] = str(MODEL_DIR) + os.pathsep + env.get("PYTHONPATH", "")
    result = subprocess.run(
        [sys.executable, str(script_path),
         "-s", str(input_xml_path), "-t", str(output_json_path),
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

            # Condition pass: authorTime -> offset dateTime (recordedDate).
            cond_tree = preprocess.preprocess_root(single_root, author_time_as_instant=False)
            cond_in = tmp / ("cond_in_%d.xml" % i)
            cond_out = tmp / ("cond_out_%d.json" % i)
            cond_tree.write(str(cond_in), xml_declaration=True, encoding="UTF-8")
            condition = _run_compiled(COMPILED_CONDITION, cond_in, cond_out)

            condition_id = str(uuid.uuid4())
            condition["id"] = condition_id

            # Provenance pass: authorTime -> TRUE UTC instant (Provenance.recorded).
            prov_tree = preprocess.preprocess_root(single_root, author_time_as_instant=True)
            prov_in = tmp / ("prov_in_%d.xml" % i)
            prov_out = tmp / ("prov_out_%d.json" % i)
            prov_tree.write(str(prov_in), xml_declaration=True, encoding="UTF-8")
            provenance = _run_compiled(COMPILED_PROVENANCE, prov_in, prov_out)

            provenance["id"] = str(uuid.uuid4())
            provenance["target"] = [{"reference": "urn:uuid:" + condition_id}]
            if raw_author_time:
                provenance["recorded"] = preprocess.riv_to_utc_instant(raw_author_time)

            entries.append({"condition": condition, "provenance": provenance})

    return entries


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("usage: python3 run_pipeline.py <GetDiagnosisResponse.xml>", file=sys.stderr)
        sys.exit(1)
    result = map_get_diagnosis_response(sys.argv[1])
    print(json.dumps(result, indent=2, ensure_ascii=False))
