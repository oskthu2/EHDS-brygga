Instance: ExtAssertedDateExample
InstanceOf: SEEHDSCondition
Title: "Condition – bidiagnos med asserted-date"
Description: "Exempel på SEEHDSCondition med ext-asserted-date och resolved bidiagnos (BY)"
Usage: #example

* meta.source = "urn:oid:1.2.752.129.2.1.4.1#SE2321000016-ABCD"

* clinicalStatus = http://terminology.hl7.org/CodeSystem/condition-clinical#resolved "Resolved"

* verificationStatus = http://terminology.hl7.org/CodeSystem/condition-ver-status#confirmed "Confirmed"

* category[diagnostyp] = https://terminologitjansten.inera.se/inera-kodverksforvaltning/kodverk/kv_diagnostyp#BY "Bidiagnos"

* code.coding[ICD10SE].system = "https://www.icd10.se/"
* code.coding[ICD10SE].code = #I10
* code.coding[ICD10SE].display = "Essentiell (primär) hypertoni"

* subject = Reference(SEEHDSPatientExample)
* subject.identifier.system = "http://electronichealth.se/identifier/personnummer"
* subject.identifier.value = "191212121212"

* onsetDateTime = "2020-03-01"
* abatementDateTime = "2021-06-30"
* recordedDate = "2020-03-02T09:00:00"

* extension[assertedDate].valueDateTime = "2020-03-02"
