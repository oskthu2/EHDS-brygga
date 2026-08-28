#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"

echo "==> Running integration tests against EHDS-brygga stack"
echo "    Gateway: $GATEWAY_URL"
echo "    (Ensure docker compose is running: docker compose up -d)"
echo "    Note: Java bridge takes ~30s to start on first run."
echo ""

check() {
  local desc="$1"
  local url="$2"
  local expected_status="${3:-200}"
  local status
  status=$(curl -s -o /dev/null -w "%{http_code}" "$url")
  if [ "$status" -eq "$expected_status" ]; then
    echo "  OK  $desc ($status)"
  else
    echo "  FAIL $desc (expected $expected_status, got $status) [$url]"
    FAILED=1
  fi
}

FAILED=0

echo "--- Gateway ---"
check "Gateway health"                "$GATEWAY_URL/health"
check "SMART configuration"          "$GATEWAY_URL/.well-known/smart-configuration.json"
check "OpenID configuration"         "$GATEWAY_URL/.well-known/openid-configuration.json"

echo ""
echo "--- FHIR API (via gateway) ---"
check "FHIR metadata (CapabilityStatement)" "$GATEWAY_URL/fhir/metadata"
check "Condition – test patient (3 diagnoses × 2 VGs = 6 total)" \
  "$GATEWAY_URL/fhir/Condition?patient.identifier=urn:oid:1.2.752.129.2.1.3.1%7C191212121212"

echo ""
echo "--- Mock health checks (informational — mocks are not exposed on host ports) ---"
info_check() {
  local desc="$1"
  local url="$2"
  local status
  status=$(curl -s -o /dev/null -w "%{http_code}" --max-time 2 "$url" 2>/dev/null || echo "000")
  if [ "$status" = "200" ]; then
    echo "  OK  $desc ($status)"
  else
    echo "  --  $desc (not reachable from host — OK in Docker)"
  fi
}
info_check "Tjänstekatalog mock"      "http://localhost:4001/health"
info_check "EI mock"                  "http://localhost:4002/health"
info_check "Spärr mock"               "http://localhost:4003/health"
info_check "Logg mock"                "http://localhost:4004/health"
info_check "Federationsmedlemskatalog mock" "http://localhost:4006/health"
info_check "Åtkomstintygsutfärdare mock"    "http://localhost:4007/health"

echo ""
if [ "$FAILED" -eq 1 ]; then
  echo "==> SOME TESTS FAILED"
  exit 1
else
  echo "==> All tests passed"
fi
