Extension: ExtSourceSystem
Id: ext-source-system
Title: "Source System"
Description: """
HSA-identifierare för källsystemet som angav diagnosen.
Mappas från RIVTA-elementet diagnosisHeader.sourceSystemHSAId i tjänstekontraktet GetDiagnosis.
Identifieraren är ett HSA-id på formatet SE{organisationsnummer}-{enhetskod}.
"""
* ^url = "https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-source-system"
* ^version = "0.1.0"
* ^status = #draft
* ^experimental = true
* ^context[+].type = #element
* ^context[=].expression = "Condition"
* ^context[+].type = #element
* ^context[=].expression = "DocumentReference"

* value[x] only Identifier
* valueIdentifier.system ^short = "urn:oid:1.2.752.129.2.1.4.1 (Inera NTjP) eller urn:oid:1.2.752.29.4.19 (basprofil)"
* valueIdentifier.value 1..1
* valueIdentifier.value ^short = "HSA-id för källsystemet, t.ex. SE2321000016-4HK5"
