#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$SCRIPT_DIR/.."

echo "==> Running integration tests against local stack"
echo "    (docker compose must be running: docker compose up -d)"
echo ""

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"

# Helper
check() {
  local desc="$1"
  local url="$2"
  local expected_status="${3:-200}"
  local status
  status=$(curl -s -o /dev/null -w "%{http_code}" "$url")
  if [ "$status" -eq "$expected_status" ]; then
    echo "  OK  $desc ($status)"
  else
    echo "  FAIL $desc (expected $expected_status, got $status)"
    FAILED=1
  fi
}

FAILED=0

echo "--- Gateway health ---"
check "Gateway health"              "$GATEWAY_URL/health"
check "SMART configuration"         "$GATEWAY_URL/.well-known/smart-configuration.json"
check "OpenID configuration"        "$GATEWAY_URL/.well-known/openid-configuration.json"

echo ""
echo "--- FHIR API ---"
check "FHIR metadata"               "$GATEWAY_URL/fhir/metadata"
check "Condition – test patient"    "$GATEWAY_URL/fhir/Condition?patient.identifier=urn:oid:1.2.752.129.2.1.3.1%7C191212121212"

echo ""
if [ "$FAILED" -eq 1 ]; then
  echo "==> SOME TESTS FAILED"
  exit 1
else
  echo "==> All tests passed"
fi
