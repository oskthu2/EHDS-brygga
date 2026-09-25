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

Extension: ExtBlockComparisonTime
Id: ext-block-comparison-time
Title: "Block Comparison Time"
Description: """
Tidpunkt som Sparr-jämförelsen (spärrkontrollen) utfördes mot.
Mappas från careDocumentation.header.accessControlHeader.blockComparisonTime i
GetCareDocumentation:3 (JoL-header v2.2).
"""
* ^url = "https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-block-comparison-time"
* ^version = "0.1.0"
* ^status = #draft
* ^experimental = true
* ^context[+].type = #element
* ^context[=].expression = "DocumentReference"

* value[x] only dateTime
* valueDateTime ^short = "Tidpunkt för Sparr-jämförelse (YYYYMMDDHHMMSS → ISO 8601)"

Extension: ExtSignatureTime
Id: ext-signature-time
Title: "Signature Time"
Description: """
Signeringstidpunkt. Mappas från careDocumentation.header.signature.timestamp i
GetCareDocumentation:3. Valfritt fält i RIVTA-kontraktet (0..1) — till skillnad från
PatientSummaryHeader-konventionens signatureTime (1..1 inom legalAuthenticator), se DOC-003
i mapping-getcaredocumentation.md.
"""
* ^url = "https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-signature-time"
* ^version = "0.1.0"
* ^status = #draft
* ^experimental = true
* ^context[+].type = #element
* ^context[=].expression = "DocumentReference"

* value[x] only dateTime
* valueDateTime ^short = "Signeringstidpunkt (YYYYMMDDHHMMSS → ISO 8601)"
