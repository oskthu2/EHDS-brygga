Instance: ExtAssertedDateExample
InstanceOf: SEEHDSCondition
Title: "Condition – exempel med asserted-date-extension"
Description: "Exempel på SEEHDSCondition med ext-asserted-date och resolved bidiagnos"
Usage: #example

* meta.source = "urn:oid:1.2.752.129.2.1.4.1#SE2321000016-ABCD"

* clinicalStatus = http://terminology.hl7.org/CodeSystem/condition-clinical#resolved "Resolved"

* category[0] = https://ehds-brygga.inera.se/fhir/CodeSystem/DiagnosisType#bi-diagnos "Bidiagnos (FHIR)"

* code.coding[0].system = "https://www.icd10.se/"
* code.coding[0].code = #I10
* code.coding[0].display = "Essentiell (primär) hypertoni"

* subject.reference = "Patient/example"
* subject.identifier.system = "http://electronichealth.se/identifier/personnummer"
* subject.identifier.value = "195001011234"

* onsetDateTime = "2020-03-01"
* abatementDateTime = "2021-06-30"
* recordedDate = "2020-03-02"

* extension[assertedDate].valueDateTime = "2020-03-02"
