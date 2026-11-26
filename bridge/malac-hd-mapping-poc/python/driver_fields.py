"""Single source of truth for the handful of Condition/Provenance fields that are set
DIRECTLY on a transform() result by DRIVER code - run_pipeline.py's old
_set_onset_and_asserted_date()/_set_uri_fields() (now just thin callers into this module),
the viewer's HapiLineageCli.java on the Java side - rather than by either .map file. See
../driver-fields.json (the actual declarative list this module loads) for WHY each field
cannot be set via FML on either engine.

Before this module existed, the SAME five-field description lived independently in THREE
places: this file's own predecessor code, HapiLineageCli.java's two hardcoded methods, and
the viewer's lineage.py ("_driver_edges()"/"_driver_provenance_edges()", a hand-written
table). A change to which fields are driver-set, or to where they come from/go, had to be
remembered and re-applied in all three by hand - exactly what Oskar objected to. Now:
  - ../driver-fields.json is the ONE declarative list (field id, source_path, target_path,
    rule name, and - where needed - the extension url used to disambiguate a repeating
    container).
  - THIS module reads it and APPLIES it (run_pipeline.py and the viewer's engines.py both
    call apply_condition_fields()/apply_provenance_fields() instead of carrying their own
    copies of the field list).
  - viewer/backend/lineage.py reads the SAME json (via load_driver_fields(), below) to build
    its *trace* edges - see that module's docstring.
  - HapiLineageCli.java reads the SAME json file at runtime (via Gson) for the Java engine,
    and DriverFieldsConfigTest.java fails if the ids it actually applies ever drift from this
    list.

Both engines' "source JSON" (the lm-diagnosis-shaped document FML itself consumes) ALREADY
carries every date-like value in its final, converted form - riv_date.riv_to_offset_datetime()/
riv_to_utc_instant() ran once, in lm_diagnosis_json_builder.py, when that JSON was built (see
its own module docstring). So applying a driver field here never needs its own RIVTA-date
parsing or re-conversion: every entry's source_path is read straight out of that SAME source
JSON the compiled transform already consumed - exactly what HapiLineageCli.java does on the
Java side (see that class's Javadoc), and a genuine simplification over the PREVIOUS Python
driver code, which used to re-extract the raw RIVTA strings from the XML and re-run riv_date
on them a second time, redundantly, alongside FML's own (separate) run over the same raw
values.
"""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
CONFIG_PATH = os.path.join(HERE, "..", "driver-fields.json")

_CONFIG_CACHE = None


def load_driver_fields():
    """Returns the parsed ../driver-fields.json: {"condition": [...], "provenance": [...]}."""
    global _CONFIG_CACHE
    if _CONFIG_CACHE is None:
        with open(CONFIG_PATH, encoding="utf-8") as fh:
            data = json.load(fh)
        data.pop("_comment", None)
        _CONFIG_CACHE = data
    return _CONFIG_CACHE


def _get_source_value(source_json, source_path):
    """Reads `source_path` (e.g. "diagnosis[].diagnosisBody.diagnosisTime") out of a
    PER-CALL lm-diagnosis source document - "[]" always resolves to index 0 here because
    every driver-field source_path's only repeating segment is "diagnosis[]", and both
    engines' drivers run the compiled/HAPI transform ONCE PER <diagnosis> entry (see
    run_pipeline.py's module docstring: "a StructureMap transform() takes one source and one
    target"), so the per-call source document always has exactly one "diagnosis[0]"."""
    cur = source_json
    for token in source_path.split("."):
        name = token[:-2] if token.endswith("[]") else token
        if not isinstance(cur, dict) or name not in cur:
            return None
        cur = cur[name]
        if token.endswith("[]"):
            if not isinstance(cur, list) or not cur:
                return None
            cur = cur[0]
    return cur


# --- Condition: one small setter per field id, dispatched by load_driver_fields()'s list so
# the LIST of fields (and where each comes from) stays config-driven even though "how to
# place a value into this particular JSON shape" still needs one line of Python per shape
# (exactly as true on the Java side - see HapiLineageCli.java's own small dispatch). Deleting
# an entry from driver-fields.json stops it being applied AND stops it being traced; adding
# one without a setter here raises immediately rather than silently doing nothing. ---

def _set_onset(condition, value):
    condition["onsetDateTime"] = value


def _set_asserted_date_value(condition, value, extension_url):
    for ext in condition.get("extension", []):
        if ext.get("url") == extension_url:
            ext["valueDateTime"] = value


def _set_meta_source(condition, value):
    if "meta" in condition:
        condition["meta"]["source"] = value


def _set_subject_identifier_system(condition, value):
    condition.get("subject", {}).get("identifier", {})["system"] = value


def _set_code_coding_system(condition, value):
    coding = condition.get("code", {}).get("coding") or []
    if coding:
        coding[0]["system"] = value


_CONDITION_SETTERS = {
    "driver-onset": lambda condition, value, field: _set_onset(condition, value),
    "assertedDate-value": lambda condition, value, field: _set_asserted_date_value(
        condition, value, field["extension_url"]
    ),
    "driver-meta-source": lambda condition, value, field: _set_meta_source(condition, value),
    "driver-subject-identifier-system": lambda condition, value, field: _set_subject_identifier_system(
        condition, value
    ),
    "driver-code-coding-system": lambda condition, value, field: _set_code_coding_system(condition, value),
}


def _set_recorded(provenance, value):
    provenance["recorded"] = value


_PROVENANCE_SETTERS = {
    "driver-recorded": lambda provenance, value, field: _set_recorded(provenance, value),
}


def apply_condition_fields(condition, cond_source):
    """Mutates `condition` (the Condition JSON dict a compiled transform() just produced) in
    place, applying every driver-fields.json "condition" entry whose source value is present
    in `cond_source` (the SAME per-call lm-diagnosis JSON the transform consumed)."""
    for field in load_driver_fields()["condition"]:
        value = _get_source_value(cond_source, field["source_path"])
        if value is None:
            continue
        setter = _CONDITION_SETTERS.get(field["id"])
        if setter is None:
            raise RuntimeError(
                "driver-fields.json has a 'condition' entry %r with no Python setter wired "
                "in python/driver_fields.py - add one" % field["id"]
            )
        setter(condition, value, field)


def apply_provenance_fields(provenance, prov_source):
    """Same as apply_condition_fields(), for the "provenance" list."""
    for field in load_driver_fields()["provenance"]:
        value = _get_source_value(prov_source, field["source_path"])
        if value is None:
            continue
        setter = _PROVENANCE_SETTERS.get(field["id"])
        if setter is None:
            raise RuntimeError(
                "driver-fields.json has a 'provenance' entry %r with no Python setter wired "
                "in python/driver_fields.py - add one" % field["id"]
            )
        setter(provenance, value, field)
