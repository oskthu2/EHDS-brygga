Instance: ConditionExample
InstanceOf: SEEHDSCondition
Title: "Condition – exempeldiagnos pneumoni"
Description: "Exempel på en SEEHDSCondition mappat från GetDiagnosis (RIVTA)"
Usage: #example

* meta.source = "urn:oid:1.2.752.129.2.1.4.1#SE2321000016-39KJ"

* clinicalStatus = http://terminology.hl7.org/CodeSystem/condition-clinical#active "Active"
* verificationStatus = http://terminology.hl7.org/CodeSystem/condition-ver-status#confirmed "Confirmed"

* category[0] = http://terminology.hl7.org/CodeSystem/condition-category#encounter-diagnosis "Encounter Diagnosis"

* code.coding[0].system = "https://www.icd10.se/"
* code.coding[0].code = #J18.9
* code.coding[0].display = "Pneumoni, ospecificerad"

* subject.identifier.system = "http://electronichealth.se/identifier/personnummer"
* subject.identifier.value = "191212121212"

* onsetDateTime = "2024-01-15"
* recordedDate = "2024-01-15"

* extension[assertedDate].valueDateTime = "2024-01-16"
