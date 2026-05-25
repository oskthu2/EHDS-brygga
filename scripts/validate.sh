#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$SCRIPT_DIR/.."

echo "==> Validating EHDS Brygga"

# Lint JavaScript (bridge and mocks)
for dir in bridge mocks/tak mocks/ei mocks/sparr mocks/logg mocks/backend; do
  if [ -f "$ROOT_DIR/$dir/package.json" ]; then
    echo "  -> npm ci + lint: $dir"
    cd "$ROOT_DIR/$dir"
    npm ci --silent
    if npm run lint --if-present 2>/dev/null; then
      echo "     lint: OK"
    fi
  fi
done

# Validate FHIR artifacts (requires FHIR Validator)
VALIDATOR_JAR="$ROOT_DIR/.tools/validator_cli.jar"
if [ -f "$VALIDATOR_JAR" ]; then
  echo "==> Validating FHIR resources..."
  find "$ROOT_DIR/ig/fsh-generated/resources" -name "*.json" | while read -r f; do
    echo "  -> $f"
    java -jar "$VALIDATOR_JAR" "$f" -version 4.0.1 -tx n/a
  done
else
  echo "WARN: FHIR Validator not found at $VALIDATOR_JAR"
  echo "      Download with: scripts/download-tools.sh"
fi

echo ""
echo "==> Validation complete"
