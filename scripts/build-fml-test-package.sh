#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
POC_DIR="$SCRIPT_DIR/../bridge/fml-mapping-poc"
RESOURCES_DIR="$POC_DIR/src/main/resources/fhir"
OUTPUT_DIR="$POC_DIR/output"
STAGE_DIR="$OUTPUT_DIR/.package-stage"

echo "==> Building a minimal FHIR package for the fhir-mapbuilder VS Code extension"
echo "    (lm-diagnosis + lm-caredocumentation + the two ConceptMaps the .map files 'uses')"

for f in lm-diagnosis.json lm-caredocumentation.json conceptmap-diagnosis-type.json conceptmap-codesystem-oid.json; do
  if [ ! -f "$RESOURCES_DIR/$f" ]; then
    echo "ERROR: missing $RESOURCES_DIR/$f - run this from a checkout of claude/fml-mapping-9ovqyh (or later)"
    exit 1
  fi
done

rm -rf "$STAGE_DIR"
mkdir -p "$STAGE_DIR/package"

cp "$RESOURCES_DIR/lm-diagnosis.json" "$STAGE_DIR/package/StructureDefinition-inera-ehds-lm-diagnosis.json"
cp "$RESOURCES_DIR/lm-caredocumentation.json" "$STAGE_DIR/package/StructureDefinition-inera-ehds-lm-care-documentation.json"
cp "$RESOURCES_DIR/conceptmap-diagnosis-type.json" "$STAGE_DIR/package/ConceptMap-diagnosis-type.json"
cp "$RESOURCES_DIR/conceptmap-codesystem-oid.json" "$STAGE_DIR/package/ConceptMap-codesystem-oid.json"

cat > "$STAGE_DIR/package/package.json" <<'EOF'
{
  "name": "ehds.brygga.fmltest",
  "version": "0.0.1",
  "fhirVersions": ["4.0.1"],
  "dependencies": {
    "hl7.fhir.r4.core": "4.0.1"
  },
  "canonical": "https://fhir.inera.se/ig/ehds-tk",
  "description": "Scratch package for the fhir-mapbuilder VS Code extension - NOT the real EHDS-brygga IG. Carries only the logical models and ConceptMaps the fml-mapping-poc .map files 'uses', so the extension's StructureMap validation has something to resolve them against.",
  "author": "ehds-brygga (fml-mapping-poc)"
}
EOF

mkdir -p "$OUTPUT_DIR"
tar -czf "$OUTPUT_DIR/package.tgz" -C "$STAGE_DIR" package
rm -rf "$STAGE_DIR"

echo "==> Wrote $OUTPUT_DIR/package.tgz"
echo ""
echo "Open '$POC_DIR' as the VS Code workspace folder (the extension looks for"
echo "output/package.tgz relative to the first workspace folder), then use the"
echo "'Load current package and Validate StructureMap' command from a .map file."
echo ""
echo "Re-run this script whenever lm-diagnosis.json, lm-caredocumentation.json or"
echo "either ConceptMap changes."
