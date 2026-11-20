# MaLaC-HD model-package plugin (the -mod / MALAC_HD_MODELS mechanism, see
# malac.hd.load_model_packages). Registers two source models:
#
#   - rivta_getdiagnosis_model.py (generateDS, from the vendored XSD) - kept ONLY for the
#     independently-testable lib-*.map reference files (see malac-hd-evaluation.md, "Det
#     delade biblioteket i praktiken"); no longer used by get-diagnosis-to-condition.map or
#     get-diagnosis-to-provenance.map.
#   - lm_diagnosis_model.py (hand-authored) - the SHARED source model both
#     get-diagnosis-to-condition.map and get-diagnosis-to-provenance.map now declare via
#     `uses "https://fhir.inera.se/ig/ehds-tk/StructureDefinition/inera-ehds-lm-diagnosis" as
#     source` - the SAME EHDS-TK logical model the Java/HAPI track (claude/fml-mapping-9ovqyh)
#     already uses. See lm_diagnosis_model.py's module docstring and malac-hd-evaluation.md,
#     "Den enade kallmodellen".
#
# The registry key is "the uses URL up to the last path segment" (malac/hd/core/fhir/r5/
# generator/structuremap.py: `s[:s.rfind('/')]`), and the module must expose a top-level class
# for whatever the URL's last segment names (e.g. ".../rivta/PersonIdType" -> class
# PersonIdType; ".../StructureDefinition/inera-ehds-lm-diagnosis" -> an attribute literally
# named "inera-ehds-lm-diagnosis", see lm_diagnosis_model.py).
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import rivta_getdiagnosis_model as supermod  # noqa: E402
import lm_diagnosis_model  # noqa: E402

RIVTA_GETDIAGNOSIS_PREFIX = "http://ehds-brygga.inera.se/fhir/rivta/getdiagnosis"
LM_DIAGNOSIS_PREFIX = "https://fhir.inera.se/ig/ehds-tk/StructureDefinition"

list_i_o_modules = {
    RIVTA_GETDIAGNOSIS_PREFIX: supermod,
    LM_DIAGNOSIS_PREFIX: lm_diagnosis_model,
}
