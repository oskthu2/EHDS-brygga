"""Runs the SAME compiled MaLaC-HD scripts (../../python/compiled/get-diagnosis-to-{condition,
provenance}.py - completely unmodified) IN-PROCESS instead of via subprocess, so this module can
observe the REAL attribute assignments the generated code makes while it runs, rather than
guessing at them from the .map file's text.

WHY this exists (see the task that produced it, and ../README.md's "How the source -> target
lineage is computed"): fml_parser.py's static trace already tells us WHICH named rule maps
WHICH source pattern to WHICH target pattern, and that part genuinely does track .map edits on
reload - Oskar's complaint, and this module's actual job, is narrower: resolving a REPEATING
target container's concrete index (e.g. "which Condition.extension[] entry did the K-th
diagnosisBody.relatedDiagnosis produce?") used to be done by an ORDER-based ASSUMPTION in
lineage.py ("the K-th relatedDiagnosis pairs with the K-th matching extension" - see its old
docstring). This module replaces that assumption, for the Python/MaLaC-HD engine, with the
REAL pairing observed while the actual generated code runs:

  1. The source document is parsed into the SAME SimpleNamespace tree
     ../../python/model/lm_diagnosis_model.py's real parse() builds (same _to_object() shape,
     copied here rather than imported, see _to_tagged_object() below) - but every node is
     additionally tagged with its own concrete JSON path ("diagnosis[0].diagnosisBody.
     relatedDiagnosis[1]", ...). This tagging is VIEWER-ONLY: the shared, production
     lm_diagnosis_model.py is never touched, so the real pipeline's parse() is unaffected.
  2. Condition.extension is replaced with a small list subclass (_TrackingList) BEFORE the
     compiled GetDiagnosisToCondition(src, tgt) function runs - a real function, imported
     straight from the compiled file, called exactly as run_pipeline.py/engines.py call it,
     not reimplemented. The compiled code only ever calls tgt.extension.append(...), never
     reassigns the list wholesale (confirmed by reading every compiled/*.py this repo has), so
     swapping the list once, up front, observes every real append.
  3. Each append() records which concrete tagged source node - if any - the CURRENT Python
     stack frame (the compiled function's own frame; for loops don't open new frames, so the
     "rel" loop variable is still a frame-local at the point of the nested append it produced)
     has bound to the name "rel", and ONLY trusts that hint once the appended object's own
     .url matches the related-condition extension's (so a stale "rel" left over from a
     different, earlier loop in the same call can never be mistaken for a real pairing).

The result is a REAL, per-call {relatedDiagnosis source index -> Condition.extension target
index} map, used by lineage.py INSTEAD of the order-based guess, whenever the Python engine
ran. The Java/HAPI engine has no equivalent (see ../README.md for why, and HapiFmlEngine's
Javadoc) and keeps the order-based fallback, clearly labelled as such.
"""
import importlib.util
import json
import re
import sys
from pathlib import Path
from types import SimpleNamespace

import malac.models.fhir.r4 as malac_r4
import malac.models.fhir.r5 as malac_r5

HERE = Path(__file__).resolve().parent
MODULE_ROOT = HERE.parent.parent
PYTHON_DIR = MODULE_ROOT / "python"
MODEL_DIR = PYTHON_DIR / "model"
COMPILED_CONDITION = PYTHON_DIR / "compiled" / "get-diagnosis-to-condition.py"
COMPILED_PROVENANCE = PYTHON_DIR / "compiled" / "get-diagnosis-to-provenance.py"

for _p in (str(MODEL_DIR), str(PYTHON_DIR)):
    if _p not in sys.path:
        sys.path.insert(0, _p)

# Mirrors lineage.py's RELATED_EXT_URL - see that module's own comment on why this one literal
# necessarily has more than one copy (the .map file creates the extension with this URL; the
# trace needs to recognise it; the driver never touches it at all, so it is not in
# driver-fields.json).
RELATED_EXT_URL = "https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-related-condition"

_RELATED_INDEX_RE = re.compile(r"relatedDiagnosis\[(\d+)\]")

_MODULE_CACHE = {}


def _load_compiled_module(path: Path, name: str):
    if name in _MODULE_CACHE:
        return _MODULE_CACHE[name]
    spec = importlib.util.spec_from_file_location(name, str(path))
    module = importlib.util.module_from_spec(spec)
    sys.modules[name] = module
    spec.loader.exec_module(module)
    _MODULE_CACHE[name] = module
    return module


class _BoolValue:
    """Local copy of lm_diagnosis_model._BoolValue (see that class's own docstring for why it
    exists at all) - duplicated here, deliberately, rather than imported, so this viewer-only
    tracer never shares mutable state with the production model module."""

    def __init__(self, value):
        self.value = value


def _child_path(base: str, key: str) -> str:
    return "%s.%s" % (base, key) if base else key


def _to_tagged_object(value, path: str):
    """Same shape as lm_diagnosis_model._to_object(), plus a "__lineage_path__" tag on every
    dict-derived node (and _BoolValue) giving its own concrete JSON path. Leaf scalars
    (str/int/float/None) cannot carry a tag - not needed, since every ambiguity this module
    resolves lives at a repeating CONTAINER (relatedDiagnosis[K]), never at a leaf."""
    if isinstance(value, dict):
        ns = SimpleNamespace(**{k: _to_tagged_object(v, _child_path(path, k)) for k, v in value.items()})
        ns.__lineage_path__ = path
        return ns
    if isinstance(value, list):
        return [_to_tagged_object(v, "%s[%d]" % (path, i)) for i, v in enumerate(value)]
    if isinstance(value, bool):
        bv = _BoolValue(value)
        bv.__lineage_path__ = path
        return bv
    return value


class _TrackingList(list):
    """Drop-in replacement for a plain list, installed on tgt.extension BEFORE the compiled
    transform runs. Records one (index, candidate "rel" hint) entry per real append() call -
    see this module's docstring, point 3. The compiled code always appends the new Extension
    BEFORE setting its .url (confirmed by reading get-diagnosis-to-condition.py: "tgt.extension.
    append(relatedExt); relatedExt.url = '...'"), so .url cannot be checked AT append() time -
    _export_and_filter_hits() below re-checks it against the FINAL exported JSON instead,
    once every assignment has happened, before trusting any hint."""

    def __init__(self, sink: list):
        super().__init__()
        self._sink = sink

    def append(self, item):
        idx = len(self)
        frame = sys._getframe(1)
        rel = frame.f_locals.get("rel")
        related_path = getattr(rel, "__lineage_path__", None) if rel is not None else None
        self._sink.append({"index": idx, "related_source_path": related_path})
        super().append(item)


def _export_json(tgt, encoder_cls) -> dict:
    """Same round trip run_pipeline.py's _write_target()/_run_compiled() do (export -> json
    text via the model's own encoder -> parse back to a plain dict), just without touching
    disk."""
    return json.loads(json.dumps(tgt.exportJson(), cls=encoder_cls))


def run_traced_python_engine(cond_src: dict, prov_src: dict):
    """Returns (condition, provenance, trace) where trace = {"related_extension_index":
    {k: real Condition.extension[] index}} - built from the REAL append() calls the compiled
    GetDiagnosisToCondition() made for THIS run, not from any ordering assumption."""
    cond_module = _load_compiled_module(COMPILED_CONDITION, "ehds_viewer_traced_condition")
    prov_module = _load_compiled_module(COMPILED_PROVENANCE, "ehds_viewer_traced_provenance")

    cond_source_obj = _to_tagged_object(cond_src, "")
    tgt = malac_r4.Condition()
    related_hits: list = []
    tgt.extension = _TrackingList(related_hits)
    cond_module.GetDiagnosisToCondition(cond_source_obj, tgt)
    condition = _export_json(tgt, malac_r4.GdsJsonEncoder_)

    # Only now, against the FINAL exported extension list, do we know each entry's real .url -
    # see _TrackingList's docstring for why this check could not happen at append() time.
    final_extensions = condition.get("extension") or []
    related_extension_index = {}
    for hit in related_hits:
        idx = hit["index"]
        if idx >= len(final_extensions) or final_extensions[idx].get("url") != RELATED_EXT_URL:
            continue
        m = _RELATED_INDEX_RE.search(hit["related_source_path"] or "")
        if m:
            related_extension_index[int(m.group(1))] = idx

    prov_source_obj = _to_tagged_object(prov_src, "")
    prov_tgt = malac_r5.Provenance()
    prov_module.GetDiagnosisToProvenance(prov_source_obj, prov_tgt)
    provenance = _export_json(prov_tgt, malac_r5.GdsJsonEncoder_)

    return condition, provenance, {"related_extension_index": related_extension_index}
