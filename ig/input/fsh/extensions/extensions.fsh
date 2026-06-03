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

Extension: ExtCareProvider
Id: ext-care-provider
Title: "Care Provider"
Description: """
HSA-identifierare för ansvarig vårdgivare (careProviderHSAId).
Mappas från diagnosisHeader.careProviderHSAId i RIVTA GetDiagnosis.
Används av Sparrtjänsten för spärrkontrollen på organisationsnivå.
"""
* ^url = "https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-care-provider"
* ^version = "0.1.0"
* ^status = #draft
* ^experimental = true
* ^context[+].type = #element
* ^context[=].expression = "Condition"
* ^context[+].type = #element
* ^context[=].expression = "DocumentReference"

* value[x] only Identifier
* valueIdentifier.system ^short = "urn:oid:1.2.752.129.2.1.4.1 (HSA-id Inera) eller urn:oid:1.2.752.29.4.19 (basprofil)"
* valueIdentifier.value 1..1
* valueIdentifier.value ^short = "HSA-id för ansvarig vårdgivare, t.ex. SE2321000016-00001"

Extension: ExtCareUnit
Id: ext-care-unit
Title: "Care Unit"
Description: """
HSA-identifierare för vårdenhet (careUnitHSAId).
Mappas från diagnosisHeader.careUnitHSAId i RIVTA GetDiagnosis.
"""
* ^url = "https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-care-unit"
* ^version = "0.1.0"
* ^status = #draft
* ^experimental = true
* ^context[+].type = #element
* ^context[=].expression = "Condition"

* value[x] only Identifier
* valueIdentifier.system ^short = "urn:oid:1.2.752.129.2.1.4.1 (HSA-id)"
* valueIdentifier.value 1..1
* valueIdentifier.value ^short = "HSA-id för vårdenhet, t.ex. SE2321000016-E000000000001"

Extension: ExtAssertedDate
Id: ext-asserted-date
Title: "Asserted Date"
Description: """
Administrativt datum när diagnosen registrerades kliniskt (author-time).
Mappas från EPS-extensionen extension:assertedDate i GetDiagnosis.
Skiljer sig från recordedDate (systemtidsstämpel) och onsetDateTime (klinisk debut).
"""
* ^url = "https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-asserted-date"
* ^version = "0.1.0"
* ^status = #draft
* ^experimental = true
* ^context[+].type = #element
* ^context[=].expression = "Condition"

* value[x] only dateTime
* valueDateTime ^short = "Administrativt datum för diagnosregistrering (YYYYMMDD → YYYY-MM-DD)"
