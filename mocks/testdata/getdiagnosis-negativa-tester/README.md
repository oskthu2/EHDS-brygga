# Negativa testfall – GetDiagnosis → Condition

Testdata för de negativa acceptanskriterierna i Del A (GetDiagnosis → Condition), ett
scenario per fil, namngivet efter testfallet. Används som referens-/QA-underlag vid sidan
av de automatiserade enhetstesterna i
`bridge/mapping-engine/src/test/java/.../getdiagnosis/GetDiagnosisMapperTest.java`
(`@Nested class NegativaAcceptanskriterier`), som är den auktoritativa verifieringen.

Vårdgivar-HSA-id:n är hämtade från `mocks/testdata/test-vardgivare.csv` (Aleris,
Danderyds sjukhus) i enlighet med konventionen att alltid använda riktiga test-HSA-id:n,
aldrig påhittade suffix.

| Fil | Testfall | Förväntat beteende |
|---|---|---|
| `diagnoskod-fran-kodverk-utan-oid-mappning.json` | Diagnoskod med `codeSystem`-OID som saknas i `naming-systems.yaml` | `Condition.code.coding.system` = `urn:oid:<oid>` (fallback) |
| `diagnostyp-utan-konceptmappning.json` | `diagnosisType` saknar post i `concept-maps/diagnosis-type.yaml` | Rå RIVTA-kod återanvänds som `category.coding.code`/`display`, inget undantag |
| `personnummer-saknas.json` | `patientId` saknas helt i `diagnosisHeader` | Condition mappas utan `subject`, inget undantag (DIAG-001) |
| `personnummer-i-fel-format.json` | `patientId.extension` är inte ett giltigt personnummer | Värdet skickas vidare oförändrat, ingen validering (DIAG-002) |
| `icke-ok-resultcode.json` | `result.resultCode` ≠ `OK` | Mapper returnerar tom lista |
| `null-tomt-indata.json` | Tomt `diagnosis`-fält i TK-svaret | Mapper returnerar tom lista, inget undantag |

Se `ig/input/pagecontent/mapping-getdiagnosis.md` (avsnitt "PoC-begränsningar") för
dokumenterade beslut om DIAG-001 och DIAG-002.
