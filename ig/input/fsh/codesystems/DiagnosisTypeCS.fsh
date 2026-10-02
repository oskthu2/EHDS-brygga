CodeSystem: DiagnosisTypeCS
Id: DiagnosisType
Title: "Diagnos Typ (kv_diagnostyp)"
Description: """
Fragment av Ineras kv_diagnostyp-kodverk på terminologitjänsten: de två koder
(HD/BY) som RIVTA-tjänstekontraktet GetDiagnosis faktiskt kan returnera i
diagnosisBody.diagnosisType. Kodsystemet ägs av Inera; detta är enbart den
delmängd EHDS-bryggan behöver för att kunna lösa upp sin egen required binding
lokalt (^content = #fragment, inte en fullständig kopia av kv_diagnostyp).
"""
* ^url = "https://terminologitjansten.inera.se/inera-kodverksforvaltning/kodverk/kv_diagnostyp"
* ^version = "0.1.0"
* ^status = #draft
* ^experimental = true
* ^caseSensitive = true
* ^content = #fragment

* #HD "Huvuddiagnos" "Huvuddiagnos (HD) – primär diagnos satt vid ett vårdtillfälle (RIVTA diagnosType HD)"
* #BY "Bidiagnos" "Bidiagnos (BY) – sekundär diagnos satt vid ett vårdtillfälle (RIVTA diagnosType BY)"
