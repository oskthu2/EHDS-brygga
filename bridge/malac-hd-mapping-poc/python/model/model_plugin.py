# MaLaC-HD model-package plugin (the -mod / MALAC_HD_MODELS mechanism, see
# malac.hd.load_model_packages). Registers the generateDS classes in
# rivta_getdiagnosis_model.py as a source model MaLaC-HD can resolve a `uses "..." as source`
# statement against.
#
# The registry key is "the uses URL up to the last path segment" (malac/hd/core/fhir/r5/
# generator/structuremap.py: `s[:s.rfind('/')]`), and the module must expose a top-level class
# for whatever the URL's last segment names (e.g. ".../rivta/PersonIdType" -> class
# PersonIdType). generateDS puts every complex/simple type of the schema as a top-level class
# in one file, so one prefix covers the whole GetDiagnosis contract.
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import rivta_getdiagnosis_model as supermod  # noqa: E402

RIVTA_GETDIAGNOSIS_PREFIX = "http://ehds-brygga.inera.se/fhir/rivta/getdiagnosis"

list_i_o_modules = {
    RIVTA_GETDIAGNOSIS_PREFIX: supermod,
}
