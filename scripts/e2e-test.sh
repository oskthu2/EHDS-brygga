#!/usr/bin/env bash
# =============================================================================
# EHDS-brygga – Comprehensive E2E test suite
#
# Requires: curl, jq
# Usage:    bash scripts/e2e-test.sh
#           GATEWAY_URL=http://host:8080 bash scripts/e2e-test.sh
# =============================================================================
set -euo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
PATIENT_ID="191212121212"
PATIENT_SYSTEM="urn:oid:1.2.752.129.2.1.3.1"
VGR_HSA="SE2321000016-4HK5"
SLL_HSA="SE2321000098-7XYZ"

PASS=0
FAIL=0

# Colour codes (suppressed when not a TTY)
if [ -t 1 ]; then
  GRN="\033[32m"; RED="\033[31m"; YEL="\033[33m"; RST="\033[0m"
else
  GRN=""; RED=""; YEL=""; RST=""
fi

ok()   { echo -e "  ${GRN}PASS${RST}  $1"; ((PASS++)); }
fail() { echo -e "  ${RED}FAIL${RST}  $1"; ((FAIL++)); }
info() { echo -e "  ${YEL}INFO${RST}  $1"; }

# --------------------------------------------------------------------------
# check_status <desc> <url> [expected_status=200]
# --------------------------------------------------------------------------
check_status() {
  local desc="$1" url="$2" expected="${3:-200}"
  local status
  status=$(curl -s -o /dev/null -w "%{http_code}" "$url")
  if [ "$status" -eq "$expected" ]; then
    ok "$desc → HTTP $status"
  else
    fail "$desc → expected $expected, got $status [$url]"
  fi
}

# --------------------------------------------------------------------------
# check_json <desc> <url> <jq_filter> <expected_value>
#   Fetches the URL, runs jq_filter, compares output to expected_value.
#   jq_filter output is compared as a string (numbers become "6" etc.).
# --------------------------------------------------------------------------
check_json() {
  local desc="$1" url="$2" jq_filter="$3" expected="$4"
  local body actual
  body=$(curl -s "$url")
  actual=$(echo "$body" | jq -r "$jq_filter" 2>/dev/null || echo "__JQ_ERROR__")
  if [ "$actual" = "$expected" ]; then
    ok "$desc → $actual"
  else
    fail "$desc → expected '$expected', got '$actual'"
  fi
}

# --------------------------------------------------------------------------
# check_json_contains <desc> <url> <jq_filter> <substring>
#   Like check_json but checks that the output *contains* the substring.
# --------------------------------------------------------------------------
check_json_contains() {
  local desc="$1" url="$2" jq_filter="$3" substring="$4"
  local body actual
  body=$(curl -s "$url")
  actual=$(echo "$body" | jq -r "$jq_filter" 2>/dev/null || echo "__JQ_ERROR__")
  if [[ "$actual" == *"$substring"* ]]; then
    ok "$desc → contains '$substring'"
  else
    fail "$desc → expected to contain '$substring', got '$actual'"
  fi
}

# --------------------------------------------------------------------------
echo "=========================================================="
echo " EHDS-brygga – E2E tests"
echo " Gateway: $GATEWAY_URL"
echo "=========================================================="
echo ""

# --------------------------------------------------------------------------
echo "--- 1. Gateway endpoints ---"
check_status "Health check"                       "$GATEWAY_URL/health"
check_json   "Health body"                        "$GATEWAY_URL/health" '.status' "ok"
check_status "SMART configuration"                "$GATEWAY_URL/.well-known/smart-configuration.json"
check_json_contains "SMART has token_endpoint"    "$GATEWAY_URL/.well-known/smart-configuration.json" '.token_endpoint' "token"
check_status "OpenID configuration"               "$GATEWAY_URL/.well-known/openid-configuration.json"
check_json_contains "OpenID has issuer"           "$GATEWAY_URL/.well-known/openid-configuration.json" '.issuer' "http"

echo ""
echo "--- 2. FHIR CapabilityStatement ---"
check_status "GET /fhir/metadata"                 "$GATEWAY_URL/fhir/metadata"
check_json   "metadata resourceType"              "$GATEWAY_URL/fhir/metadata" '.resourceType' "CapabilityStatement"
check_json_contains "metadata fhirVersion=4"      "$GATEWAY_URL/fhir/metadata" '.fhirVersion' "4."

echo ""
echo "--- 3. Condition – aggregerad sökning (alla VG) ---"
COND_URL="$GATEWAY_URL/fhir/Condition?patient.identifier=${PATIENT_SYSTEM}%7C${PATIENT_ID}"
check_status "GET Condition (alla VG)"            "$COND_URL"
check_json   "Condition total = 6 (3 diagnoser × 2 VG)" "$COND_URL" '.total' "6"
check_json   "Condition resourceType = Bundle"    "$COND_URL" '.resourceType' "Bundle"
check_json   "Condition type = searchset"         "$COND_URL" '.type' "searchset"

# Check that specific ICD codes appear among the entries
check_json   "Kod J18.9 (Pneumoni) förekommer"   "$COND_URL" \
  '[.entry[].resource | select(.resourceType=="Condition") | .code.coding[0].code] | map(select(. == "J18.9")) | length' "2"
check_json   "Kod E11.9 (Diabetes typ 2) förekommer" "$COND_URL" \
  '[.entry[].resource | select(.resourceType=="Condition") | .code.coding[0].code] | map(select(. == "E11.9")) | length' "2"
check_json   "Kod I10 (Essentiell hypertoni) förekommer" "$COND_URL" \
  '[.entry[].resource | select(.resourceType=="Condition") | .code.coding[0].code] | map(select(. == "I10")) | length' "2"

# Check coding system is ICD-10-SE
check_json   "Diagnoskodsystem = ICD-10-SE URI"  "$COND_URL" \
  '.entry[0].resource.code.coding[0].system' "https://www.icd10.se/"

# Check patient subject is set
check_json   "Patient identifier.value = $PATIENT_ID" "$COND_URL" \
  '.entry[0].resource.subject.identifier.value' "$PATIENT_ID"

# Check Provenance entries are included (one per Condition for audit trail)
check_json   "Provenance-resurser finns med (sökkvalitet)" "$COND_URL" \
  '[.entry[].resource | select(.resourceType=="Provenance")] | length >= 1' "true"

echo ""
echo "--- 4. Condition – VG-scopad sökning ---"
COND_VGR_URL="$GATEWAY_URL/fhir/${VGR_HSA}/Condition?patient.identifier=${PATIENT_SYSTEM}%7C${PATIENT_ID}"
COND_SLL_URL="$GATEWAY_URL/fhir/${SLL_HSA}/Condition?patient.identifier=${PATIENT_SYSTEM}%7C${PATIENT_ID}"

check_status "GET Condition VGR-scope"             "$COND_VGR_URL"
check_json   "Condition VGR total = 3"             "$COND_VGR_URL" '.total' "3"
check_status "GET Condition SLL-scope"             "$COND_SLL_URL"
check_json   "Condition SLL total = 3"             "$COND_SLL_URL" '.total' "3"

echo ""
echo "--- 5. DocumentReference – aggregerad sökning ---"
DOCREF_URL="$GATEWAY_URL/fhir/DocumentReference?patient.identifier=${PATIENT_SYSTEM}%7C${PATIENT_ID}"
check_status "GET DocumentReference (alla VG)"    "$DOCREF_URL"
check_json   "DocumentReference total = 4 (2 dok × 2 VG)" "$DOCREF_URL" '.total' "4"
check_json   "DocumentReference resourceType = Bundle" "$DOCREF_URL" '.resourceType' "Bundle"

# VG-scoped DocumentReference
DOCREF_VGR_URL="$GATEWAY_URL/fhir/${VGR_HSA}/DocumentReference?patient.identifier=${PATIENT_SYSTEM}%7C${PATIENT_ID}"
check_status "GET DocumentReference VGR-scope"    "$DOCREF_VGR_URL"
check_json   "DocumentReference VGR total = 2"    "$DOCREF_VGR_URL" '.total' "2"

echo ""
echo "--- 6. Edge cases ---"
UNKNOWN_URL="$GATEWAY_URL/fhir/Condition?patient.identifier=${PATIENT_SYSTEM}%7C000000000000"
check_status "Okänd patient returnerar 200"       "$UNKNOWN_URL"
check_json   "Okänd patient total = 0"            "$UNKNOWN_URL" '.total' "0"

echo ""
echo "=========================================================="
if [ "$FAIL" -eq 0 ]; then
  echo -e " ${GRN}Alla $PASS tester godkända.${RST}"
  exit 0
else
  echo -e " ${RED}$FAIL av $((PASS+FAIL)) tester MISSLYCKADES.${RST}"
  exit 1
fi
