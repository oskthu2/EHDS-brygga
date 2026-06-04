CodeSystem: DiagnosisTypeCS
Id: DiagnosisType
Title: "Diagnos Typ"
Description: """
Kodsystem för svenska diagnosklassifikationer som inte täcks av FHIR-standarden.
Används för att ange om en diagnos är huvud- eller bidiagnos enligt svenska journalsystem
och Ineras RIVTA-tjänstekontrakt.
"""
* ^url = "https://ehds-brygga.inera.se/fhir/CodeSystem/DiagnosisType"
* ^version = "0.1.0"
* ^status = #draft
* ^experimental = true
* ^caseSensitive = true
* ^content = #complete

* #HD "Huvuddiagnos" "Huvuddiagnos (HD) – primär diagnos satt vid ett vårdtillfälle (RIVTA diagnosType HD)"
* #BY "Bidiagnos" "Bidiagnos (BY) – sekundär diagnos satt vid ett vårdtillfälle (RIVTA diagnosType BY)"
* #huvud-diagnos "Huvuddiagnos (FHIR)" "FHIR-alias för RIVTA Huvuddiagnos, mappas till encounter-diagnosis"
* #bi-diagnos "Bidiagnos (FHIR)" "FHIR-alias för RIVTA Bidiagnos"
