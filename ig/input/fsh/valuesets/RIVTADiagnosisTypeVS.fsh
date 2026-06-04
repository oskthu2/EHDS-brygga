ValueSet: RIVTADiagnosisTypeVS
Id: RIVTADiagnosisType
Title: "RIVTA Diagnos Typ"
Description: """
ValueSet för de RIVTA diagnosType-koder som förekommer i GetDiagnosis-svar (HD och BY).
Används som source scope i ConceptMap DiagnosisTypeToCategoryMap.
"""
* ^url = "https://ehds-brygga.inera.se/fhir/ValueSet/RIVTADiagnosisType"
* ^version = "0.1.0"
* ^status = #draft
* ^experimental = true

* https://ehds-brygga.inera.se/fhir/CodeSystem/DiagnosisType#HD "Huvuddiagnos"
* https://ehds-brygga.inera.se/fhir/CodeSystem/DiagnosisType#BY "Bidiagnos"
