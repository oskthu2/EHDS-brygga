Instance: ConditionExample
InstanceOf: SEEHDSCondition
Title: "Condition – exempeldiagnos pneumoni"
Description: "Exempel på en SEEHDSCondition mappat från GetDiagnosis (RIVTA), med recorder och asserter"
Usage: #example

* meta.source = "urn:oid:1.2.752.129.2.1.4.1#SE2321000016-39KJ"

* clinicalStatus = http://terminology.hl7.org/CodeSystem/condition-clinical#active "Active"

* verificationStatus = http://terminology.hl7.org/CodeSystem/condition-ver-status#confirmed "Confirmed"

* category[diagnostyp] = https://terminologitjansten.inera.se/inera-kodverksforvaltning/kodverk/kv_diagnostyp#HD "Huvuddiagnos"

* code.coding[ICD10SE].system = "https://www.icd10.se/"
* code.coding[ICD10SE].code = #J18.9
* code.coding[ICD10SE].display = "Pneumoni, ospecificerad"

* subject = Reference(SEEHDSPatientExample)
* subject.identifier.system = "http://electronichealth.se/identifier/personnummer"
* subject.identifier.value = "191212121212"

* onsetDateTime = "2024-01-15"
* recordedDate = "2024-01-15T08:30:00"

* recorder.type = "PractitionerRole"
* recorder.identifier.system = "urn:oid:1.2.752.129.2.1.4.1"
* recorder.identifier.value = "SE2321000016-DOK"

* asserter.type = "PractitionerRole"
* asserter.identifier.system = "urn:oid:1.2.752.129.2.1.4.1"
* asserter.identifier.value = "SE2321000016-AUTH"

* extension[assertedDate].valueDateTime = "2024-01-16"
