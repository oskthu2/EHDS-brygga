Instance: DiagnosisTypeToCategoryMap
InstanceOf: ConceptMap
Title: "DiagnosisType → Condition.category"
Description: "Mappning av RIVTA diagnosisType (HD/BY) från kv_diagnostyp till FHIR Condition.category[diagnostyp]"
Usage: #definition

* url = "https://ehds-brygga.inera.se/fhir/ConceptMap/DiagnosisTypeToCategoryMap"
* version = "0.1.0"
* name = "DiagnosisTypeToCategoryMap"
* status = #draft
* experimental = true
* sourceUri = "https://ehds-brygga.inera.se/fhir/ValueSet/RIVTADiagnosisType"
* targetUri = "https://ehds-brygga.inera.se/fhir/ValueSet/SEDiagnosisType"

* group[0].source = "https://terminologitjansten.inera.se/inera-kodverksforvaltning/kodverk/kv_diagnostyp"
* group[0].target = "https://terminologitjansten.inera.se/inera-kodverksforvaltning/kodverk/kv_diagnostyp"
* group[0].element[0].code = #HD
* group[0].element[0].display = "Huvuddiagnos"
* group[0].element[0].target[0].code = #HD
* group[0].element[0].target[0].display = "Huvuddiagnos"
* group[0].element[0].target[0].equivalence = #equal

* group[0].element[1].code = #BY
* group[0].element[1].display = "Bidiagnos"
* group[0].element[1].target[0].code = #BY
* group[0].element[1].target[0].display = "Bidiagnos"
* group[0].element[1].target[0].equivalence = #equal
