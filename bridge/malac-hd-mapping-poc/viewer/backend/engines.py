"""Runs the SAME lm-diagnosis-shaped source JSON (built once, in Python, by
lm_diagnosis_json_builder.py - see its module docstring for why one builder suffices for both
engines) through EITHER the MaLaC-HD-compiled Python transforms (run_pipeline.py's own
helpers, reused as-is) or the HAPI/StructureMapUtilities Java engine (via the HapiLineageCli
CLI added in this same PR - see src/main/java/.../HapiLineageCli.java), returning a plain
{"condition": dict, "provenance": dict} either way.
"""
import json
import os
import subprocess
import sys
import tempfile
import uuid
from pathlib import Path

HERE = Path(__file__).resolve().parent
MODULE_ROOT = HERE.parent.parent  # bridge/malac-hd-mapping-poc
PYTHON_DIR = MODULE_ROOT / "python"

sys.path.insert(0, str(PYTHON_DIR))
import run_pipeline  # noqa: E402  (needs PYTHON_DIR on sys.path first)
import riv_date  # noqa: E402


class EngineError(RuntimeError):
    pass


def run_python_engine(cond_src: dict, prov_src: dict, raw_diagnosis_time, raw_signature_time, raw_author_time):
    with tempfile.TemporaryDirectory() as tmp:
        tmp = Path(tmp)
        cond_out = tmp / "cond_out.json"
        condition = run_pipeline._run_compiled(run_pipeline.COMPILED_CONDITION, cond_src, cond_out, tmp, "cond")
        condition_id = str(uuid.uuid4())
        condition["id"] = condition_id
        run_pipeline._set_onset_and_asserted_date(condition, raw_diagnosis_time, raw_signature_time)
        run_pipeline._set_uri_fields(condition, cond_src)

        prov_out = tmp / "prov_out.json"
        provenance = run_pipeline._run_compiled(run_pipeline.COMPILED_PROVENANCE, prov_src, prov_out, tmp, "prov")
        provenance["id"] = str(uuid.uuid4())
        provenance["target"] = [{"reference": "urn:uuid:" + condition_id}]
        if raw_author_time:
            provenance["recorded"] = riv_date.riv_to_utc_instant(raw_author_time)
    return condition, provenance


_CLASSPATH_FILE = MODULE_ROOT / "target" / "classpath.txt"
_CLASSES_DIR = MODULE_ROOT / "target" / "classes"


def _ensure_java_classpath():
    """Builds target/classpath.txt (and target/classes) once via Maven, if missing - see
    viewer/README.md for the one-time setup command Oskar can also run by hand up front."""
    if _CLASSPATH_FILE.exists() and _CLASSES_DIR.exists():
        return
    result = subprocess.run(
        ["mvn", "-q", "compile", "dependency:build-classpath",
         "-Dmdep.outputFile=target/classpath.txt"],
        cwd=str(MODULE_ROOT), capture_output=True, text=True,
    )
    if result.returncode != 0:
        raise EngineError(
            "Could not build the Java classpath (ran 'mvn compile dependency:build-classpath' "
            "in %s):\n%s\n%s" % (MODULE_ROOT, result.stdout, result.stderr)
        )


def run_java_engine(cond_src: dict, prov_src: dict):
    _ensure_java_classpath()
    classpath = _CLASSES_DIR.as_posix() + os.pathsep + _CLASSPATH_FILE.read_text().strip()
    with tempfile.TemporaryDirectory() as tmp:
        tmp = Path(tmp)
        cond_path = tmp / "cond_src.json"
        prov_path = tmp / "prov_src.json"
        cond_path.write_text(json.dumps(cond_src), encoding="utf-8")
        prov_path.write_text(json.dumps(prov_src), encoding="utf-8")
        result = subprocess.run(
            ["java", "-cp", classpath, "se.inera.ehds.malac.hapi.HapiLineageCli",
             str(cond_path), str(prov_path)],
            cwd=str(MODULE_ROOT), capture_output=True, text=True,
        )
        if result.returncode != 0:
            raise EngineError("HapiLineageCli failed:\n%s\n%s" % (result.stdout, result.stderr))
        try:
            data = json.loads(result.stdout)
        except json.JSONDecodeError as exc:
            raise EngineError("HapiLineageCli did not print valid JSON:\n%s\n(stderr: %s)"
                               % (result.stdout, result.stderr)) from exc
    return data.get("condition") or {}, data.get("provenance") or {}
