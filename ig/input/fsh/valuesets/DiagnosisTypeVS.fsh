ValueSet: SEDiagnosisTypeVS
Id: SEDiagnosisType
Title: "SE Diagnos Typ"
Description: """
ValueSet som kombinerar standard-FHIR condition-category-koder med svenska diagnosklassifikationer
från DiagnosisTypeCS. Används i Condition.category för att ange diagnostyp enligt
Ineras RIVTA-tjänstekontrakt GetDiagnosis.
"""
* ^url = "https://ehds-brygga.inera.se/fhir/ValueSet/SEDiagnosisType"
* ^version = "0.1.0"
* ^status = #draft
* ^experimental = true

// Standard FHIR condition-category koder
* $conditionCategory#problem-list-item "Problem List Item"
* $conditionCategory#encounter-diagnosis "Encounter Diagnosis"

// Svenska diagnostyper
* include codes from system https://ehds-brygga.inera.se/fhir/CodeSystem/DiagnosisType
