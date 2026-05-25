#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TOOLS_DIR="$SCRIPT_DIR/../.tools"

mkdir -p "$TOOLS_DIR"

echo "==> Downloading FHIR Validator CLI..."
VALIDATOR_URL="https://github.com/hapifhir/org.hl7.fhir.core/releases/latest/download/validator_cli.jar"
curl -L -o "$TOOLS_DIR/validator_cli.jar" "$VALIDATOR_URL"
echo "    Saved to $TOOLS_DIR/validator_cli.jar"

echo ""
echo "==> Checking for sushi (FSH compiler)..."
if ! command -v sushi &> /dev/null; then
  echo "    sushi not found. Install with:"
  echo "    npm install -g fsh-sushi"
else
  echo "    sushi: $(sushi --version)"
fi

echo ""
echo "==> Tools ready"
