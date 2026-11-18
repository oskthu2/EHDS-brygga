"""Hand-authored MaLaC-HD "model" for the SHARED source: EHDS-TK's published
inera-ehds-lm-diagnosis logical model (../../fhir/lm-diagnosis.json), the SAME
StructureDefinition the Java/HAPI track (claude/fml-mapping-9ovqyh, bridge/fml-mapping-poc)
already parses via org.hl7.fhir.r4.elementmodel.JsonParser - see
se.inera.ehds.malac.hapi.LmDiagnosisJsonSourceBuilder in this module's src/main/java and
malac-hd-evaluation.md, "Den enade kallmodellen", for the full account.

Unlike rivta_getdiagnosis_model.py (generateDS output from the vendored XSD, still used by the
untouched lib-*.map reference files), inera-ehds-lm-diagnosis has no XSD of its own - it is a
hand-authored FHIR StructureDefinition (kind=logical) - so there is nothing to run generateDS
against. This module does not need anything close to generateDS's output either, confirmed by
actually compiling get-diagnosis-to-condition.map and reading both the compiler's own errors
and its generated code:
  - At COMPILE time, MaLaC-HD's _handle_group() (malac/hd/core/fhir/r5/generator/
    structuremap.py) DOES resolve the group's declared "source src : LmDiagnosis" type to a
    real attribute on this module - getattr(i_module, "inera_ehds_lm_diagnosis") (the uses
    URL's last path segment, alias-resolved, with "-" normalized to "_") - so a real top-level
    class with that exact name is required (see IneraEhdsLmDiagnosis below), even though
    nothing about its own shape/fields is ever introspected.
  - EVERY field navigation inside the group body, by contrast, stays genuinely untyped at
    compile time (confirmed by the "source type is unknown" list-coercion warning MaLaC-HD
    emits for every single one) - the compiler never checks a navigated field against any
    class's real attributes.
  - At RUNTIME, the compiled script only ever calls
    "<registered module>.parse(source, silence=True)" once, to build the whole source object
    tree (see _read_source() in any compiled/*.py); everything after that is plain
    getattr(obj, 'fieldName', None) access against whatever parse() returned.
So the "model" here is just enough Python to satisfy those two contracts: one marker class
named after the uses URL (compile time), and a parse() function that turns a JSON instance of
the shared shape into plain objects with matching attribute names, via json.load's
object_hook (runtime) - no field-by-field class hierarchy, no XML parser, needed at all.

parse() is deliberately JSON-only even though it is registered (and called by the compiled
wrapper) under a source_type of 'xml' - see python/run_pipeline.py's module docstring and
malac-hd-evaluation.md, "En tolkningsgenvag: 'xml' ljuger om formatet", for why: the compiled
CLI wrapper hardcodes _SOURCE_JSON_SUPPORTED = False for any non-built-in-FHIR source model
(malac/hd/core/fhir/r5/generator/structuremap.py: i_json = self.i_is_fhir), regardless of what
this module's own parse() actually does - that flag only gates the wrapper's own --source-type
validation, never the call it eventually makes to THIS function, so passing '-st xml' (as
run_pipeline.py already always did, even before this change) and having parse() read real JSON
is the simplest way to keep the SAME compiled-script call signature while switching the wire
format to the one format the Java/HAPI track also consumes.
"""
import json
from types import SimpleNamespace


class _BoolValue:
    """Thin Element-like wrapper for a JSON boolean LEAF value (chronicDiagnosis is the only
    one in this contract), giving it a ".value" attribute. HAPI's generic Element model
    represents EVERY field in a logical-model instance - including bare primitives - as an
    object of its own, so get-diagnosis-to-condition.map navigates ".value" to reach the real
    boolean (see that file's chronicDiagnosis rule and its comment - confirmed necessary by
    actually running this map against HAPI: without it, HAPI's PrimitiveType.setProperty()
    stringifies the WRAPPER object itself, "chronicDiagnosis=boolean[true]", not "true").
    MaLaC-HD's own custom-model values have no such wrapping by default (a bare JSON bool
    stays a bare Python bool after _to_object()) - this class exists ONLY so that SAME
    ".value" navigation the shared .map file needs for HAPI is also a safe, correct no-op
    here (chronic.value simply returns the real bool back), rather than forcing two different
    navigation paths in the one map file both engines run. See malac-hd-evaluation.md, "Den
    portabla delmangden av FML"."""
    __slots__ = ("value",)

    def __init__(self, value):
        self.value = value


def _to_object(value):
    if isinstance(value, dict):
        return SimpleNamespace(**{k: _to_object(v) for k, v in value.items()})
    if isinstance(value, list):
        return [_to_object(v) for v in value]
    if isinstance(value, bool):
        return _BoolValue(value)
    return value


def parse(source, silence=True):
    """source: a file path (str), or a readable binary/text stream (stdin), containing one
    lm-diagnosis-shaped JSON instance (see ../lm_diagnosis_json_builder.py). Returns a tree of
    types.SimpleNamespace objects whose attribute names mirror inera-ehds-lm-diagnosis 1:1, so
    the compiled transform's getattr(obj, 'fieldName', None) navigation (see any compiled/*.py)
    works unchanged - MaLaC-HD never needs a "real" generateDS/FHIR class here, just matching
    attribute names at runtime."""
    if hasattr(source, "read"):
        data = json.load(source)
    else:
        with open(source, encoding="utf-8") as fh:
            data = json.load(fh)
    return _to_object(data)


class IneraEhdsLmDiagnosis:
    """Marker class MaLaC-HD's compiler resolves the group's "source src : LmDiagnosis"
    declaration to at COMPILE time (confirmed by actually compiling: _handle_group(), in
    malac/hd/core/fhir/r5/generator/structuremap.py, does
    getattr(self.i_module, type_name.replace("-", "_")) for the group's declared source/target
    types - unlike every FIELD navigation inside the group body, which stays untyped, see the
    "source type is unknown" list-coercion warnings). Never actually instantiated or
    introspected beyond that one isinstance-free attribute lookup, so its contents do not
    matter - only its name does."""


# inera-ehds-lm-diagnosis contains hyphens, not a legal Python identifier via dot-syntax.
# MaLaC-HD's own lookup normalizes "-" -> "_" before calling getattr() (see _handle_group()
# above), so the underscored name is the one that actually gets resolved; the hyphenated name
# is kept too, matching model_plugin.py's documented convention literally.
globals()["inera_ehds_lm_diagnosis"] = IneraEhdsLmDiagnosis
globals()["inera-ehds-lm-diagnosis"] = IneraEhdsLmDiagnosis
