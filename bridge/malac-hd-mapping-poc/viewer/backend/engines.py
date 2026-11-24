"""Runs the SAME lm-diagnosis-shaped source JSON (built once, in Python, by
lm_diagnosis_json_builder.py - see its module docstring for why one builder suffices for both
engines) through EITHER the MaLaC-HD-compiled Python transforms, run IN-PROCESS with real
execution tracing (traced_python_engine.py - see its own module docstring for why), or the
HAPI/StructureMapUtilities Java engine (via the HapiLineageCli CLI added in this same PR - see
src/main/java/.../HapiLineageCli.java), returning ("condition": dict, "provenance": dict,
runtime_hints: dict) either way - runtime_hints is non-empty only for the Python engine (the
real {relatedDiagnosis index -> Condition.extension index} pairing observed while the actual
generated code ran; see lineage.py's resolve_edges() for how it is used instead of an
order-based assumption).
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
import driver_fields  # noqa: E402

import traced_python_engine  # noqa: E402


class EngineError(RuntimeError):
    pass


def run_python_engine(cond_src: dict, prov_src: dict):
    """Runs the SAME compiled scripts run_pipeline.py runs (unmodified; see
    traced_python_engine.py), then applies the SAME five driver-set fields run_pipeline.py
    applies - via driver_fields.py, the single source of truth both now read (see its module
    docstring) - reading them straight out of cond_src/prov_src (the per-call lm-diagnosis
    JSON the transform itself just consumed), no separate raw-RIVTA-date extraction needed
    any more."""
    condition, provenance, runtime_hints = traced_python_engine.run_traced_python_engine(cond_src, prov_src)
    condition_id = str(uuid.uuid4())
    condition["id"] = condition_id
    driver_fields.apply_condition_fields(condition, cond_src)

    provenance["id"] = str(uuid.uuid4())
    provenance["target"] = [{"reference": "urn:uuid:" + condition_id}]
    driver_fields.apply_provenance_fields(provenance, prov_src)
    return condition, provenance, runtime_hints


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
    # No runtime_hints for this engine - see HapiFmlEngine.java's Javadoc and ../README.md for
    # why real execution tracing was not reasonably achievable for HAPI's StructureMapUtilities;
    # lineage.py falls back to its order-based assumption whenever hints come back empty.
    return data.get("condition") or {}, data.get("provenance") or {}, {}
