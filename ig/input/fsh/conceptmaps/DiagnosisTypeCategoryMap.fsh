Instance: DiagnosisTypeToCategoryMap
InstanceOf: ConceptMap
Title: "DiagnosisType → Condition.category"
Description: "Mappning av RIVTA diagnosisType (HD/BY) till FHIR Condition.category"
Usage: #definition

* url = "https://ehds-brygga.inera.se/fhir/ConceptMap/DiagnosisTypeToCategoryMap"
* version = "0.1.0"
* name = "DiagnosisTypeToCategoryMap"
* status = #draft
* experimental = true
* sourceUri = "https://ehds-brygga.inera.se/fhir/CodeSystem/DiagnosisType"
* targetUri = "http://terminology.hl7.org/CodeSystem/condition-category"

* group[0].source = "https://ehds-brygga.inera.se/fhir/CodeSystem/DiagnosisType"
* group[0].target = "http://terminology.hl7.org/CodeSystem/condition-category"
* group[0].element[0].code = #HD
* group[0].element[0].display = "Huvuddiagnos"
* group[0].element[0].target[0].code = #encounter-diagnosis
* group[0].element[0].target[0].display = "Encounter Diagnosis"
* group[0].element[0].target[0].equivalence = #equivalent

* group[1].source = "https://ehds-brygga.inera.se/fhir/CodeSystem/DiagnosisType"
* group[1].target = "https://ehds-brygga.inera.se/fhir/CodeSystem/DiagnosisType"
* group[1].element[0].code = #BY
* group[1].element[0].display = "Bidiagnos"
* group[1].element[0].target[0].code = #bi-diagnos
* group[1].element[0].target[0].display = "Bidiagnos"
* group[1].element[0].target[0].equivalence = #equivalent
