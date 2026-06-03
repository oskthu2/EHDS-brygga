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
