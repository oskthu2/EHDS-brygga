#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$SCRIPT_DIR/.."

echo "==> Validerar EHDS Brygga"

echo "--- Bridge (Maven) ---"
cd "$ROOT_DIR/bridge"
mvn verify -DskipTests --no-transfer-progress
echo "  bridge: OK"

echo ""
echo "--- Mock-tjänster (Node.js) ---"
for dir in mocks/tjanstekatalog mocks/fedkatalog mocks/token-issuer mocks/ei mocks/sparr mocks/logg mocks/backend; do
  if [ -f "$ROOT_DIR/$dir/package.json" ]; then
    echo "  -> $dir"
    npm ci --prefix "$ROOT_DIR/$dir" --silent
  fi
done

echo ""
echo "--- FHIR-validering (IPS + EURIDICE-alignment) ---"
VALIDATOR_JAR="$ROOT_DIR/.tools/validator_cli.jar"
if [ -f "$VALIDATOR_JAR" ]; then
  echo "  Validerar mot IPS-profil (hl7.fhir.uv.ips) ..."
  # Validera genererade IG-resurser om de finns
  FHIR_RESOURCES_DIR="$ROOT_DIR/ig/fsh-generated/resources"
  if [ -d "$FHIR_RESOURCES_DIR" ]; then
    find "$FHIR_RESOURCES_DIR" -name "Condition-*.json" | while read -r f; do
      echo "    -> $f"
      java -jar "$VALIDATOR_JAR" "$f" \
        -version 4.0.1 \
        -ig hl7.fhir.uv.ips \
        -profile http://hl7.org/fhir/uv/ips/StructureDefinition/Condition-uv-ips \
        -tx n/a 2>/dev/null && echo "       OK" || echo "       WARN: validering misslyckades (icke-blockerande)"
    done
    find "$FHIR_RESOURCES_DIR" -name "DocumentReference-*.json" | while read -r f; do
      echo "    -> $f"
      java -jar "$VALIDATOR_JAR" "$f" \
        -version 4.0.1 \
        -tx n/a 2>/dev/null && echo "       OK" || echo "       WARN: validering misslyckades (icke-blockerande)"
    done
  else
    echo "  WARN: ig/fsh-generated/resources saknas – kör 'sushi .' i ig/ först"
  fi
else
  echo "  INFO: FHIR Validator ej tillgänglig på $VALIDATOR_JAR"
  echo "        Ladda ner med: scripts/download-tools.sh"
fi

echo ""
echo "==> Validering klar"
