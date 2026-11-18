#!/usr/bin/env bash
# Regenerates compiled/*.py from the FML sources under fml/ using MaLaC-HD 1.10.0.
# Requires malac-hd + generateDS pinned per requirements.txt, and model/rivta_getdiagnosis_model.py
# already generated (see model/generate-model.sh).
#
# Usage: ./compile.sh   (run from this directory, python/)
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p compiled

malac-hd -m fml/get-diagnosis-to-condition.map -co compiled/get-diagnosis-to-condition.py \
  -mod model/model_plugin.py \
  -sr conceptmaps/diagnosis-type.json conceptmaps/codesystem-oid.json -s

malac-hd -m fml/get-diagnosis-to-provenance.map -co compiled/get-diagnosis-to-provenance.py \
  -mod model/model_plugin.py -s

echo "Compiled fml/*.map -> compiled/*.py"
