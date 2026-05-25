#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IG_DIR="$SCRIPT_DIR/../ig"

echo "==> Building FHIR Implementation Guide"
echo "    IG directory: $IG_DIR"

# Check dependencies
if ! command -v sushi &> /dev/null; then
  echo "ERROR: sushi not found. Install with: npm install -g fsh-sushi"
  exit 1
fi

if [ ! -f "$IG_DIR/ig.ini" ]; then
  echo "ERROR: ig.ini not found at $IG_DIR/ig.ini"
  exit 1
fi

# Run SUSHI to compile FSH → FHIR JSON
echo "==> Running SUSHI..."
cd "$IG_DIR"
sushi .

echo ""
echo "==> SUSHI completed. FSH compiled to FHIR artifacts in ig/fsh-generated/"
echo ""
echo "To run the full IG Publisher (requires Java):"
echo "  java -jar publisher.jar -ig ig.ini"
echo ""
echo "Or use the IG Publisher script:"
echo "  cd ig && ./_genonce.sh"
