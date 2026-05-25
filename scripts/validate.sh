#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$SCRIPT_DIR/.."

echo "==> Validating EHDS Brygga"

echo "--- Bridge (Maven) ---"
cd "$ROOT_DIR/bridge"
mvn verify -DskipTests --no-transfer-progress
echo "  bridge: OK"

echo ""
echo "--- Mock services (Node.js) ---"
for dir in mocks/tak mocks/ei mocks/sparr mocks/logg mocks/backend; do
  if [ -f "$ROOT_DIR/$dir/package.json" ]; then
    echo "  -> $dir"
    npm ci --prefix "$ROOT_DIR/$dir" --silent
  fi
done

echo ""
echo "==> Validation complete"
