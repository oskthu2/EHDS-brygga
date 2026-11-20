#!/usr/bin/env bash
# Regenerates rivta_getdiagnosis_model.py from the vendored RIVTA GetDiagnosis XSDs using
# generateDS (https://www.davekuhlman.org/generateDS.html, pip package "generateDS").
#
# MaLaC-HD's own built-in FHIR R4/R5 model packages (malac-models-fhir) are themselves
# generateDS output (see malac/hd/core/fhir/base/generator/resource.py: "generateDS globals",
# "GDSClassesMapping", the "supermod" convention). Since the RIVTA service contracts are
# *real* XSD (not a hand-authored logical model), we can generate an equivalent Python model
# directly from the vendored schema and register it with MaLaC-HD as a custom source model,
# instead of first authoring a FHIR logical-model StructureDefinition the way the FML/HAPI
# track (claude/fml-mapping-9ovqyh) had to. See malac-hd-evaluation.md, "Ingen logisk modell
# behövs" for the full writeup of this finding.
#
# Pinned version used when this file was last generated: generateDS 2.44.3 (pip install generateDS).
set -euo pipefail
cd "$(dirname "$0")"

XSD_ROOT="../../../mapping-engine/src/test/resources/riv-schemas"
RESPONDER_XSD="$XSD_ROOT/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/GetDiagnosisResponder_2.0.xsd"

generateDS.py -o rivta_getdiagnosis_model.py "$RESPONDER_XSD"

echo "Generated rivta_getdiagnosis_model.py from $RESPONDER_XSD"
