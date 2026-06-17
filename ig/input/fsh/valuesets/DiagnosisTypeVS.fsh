ValueSet: SEDiagnosisTypeVS
Id: SEDiagnosisType
Title: "SE Diagnos Typ"
Description: """
ValueSet med Ineras diagnostyper (HD/BY) från kv_diagnostyp på terminologitjänsten.
Används i Condition.category[diagnostyp] för att ange diagnostyp enligt
Ineras RIVTA-tjänstekontrakt GetDiagnosis:2.
"""
* ^url = "https://ehds-brygga.inera.se/fhir/ValueSet/SEDiagnosisType"
* ^version = "0.1.0"
* ^status = #draft
* ^experimental = true

* include codes from system $diagnosisTypeCS
