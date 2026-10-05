# Negativa testfall – GetDiagnosis → Condition

Testdata för de negativa acceptanskriterierna i Del A (GetDiagnosis → Condition), ett
scenario per fil, namngivet efter testfallet. Används som referens-/QA-underlag vid sidan
av de automatiserade enhetstesterna i
`bridge/mapping-engine/src/test/java/.../getdiagnosis/GetDiagnosisMapperTest.java`
(`@Nested class NegativaAcceptanskriterier`), som är den auktoritativa verifieringen.

Vårdgivar-HSA-id:n är hämtade från `mocks/testdata/test-vardgivare.csv` (Aleris,
Danderyds sjukhus) i enlighet med konventionen att alltid använda riktiga test-HSA-id:n,
aldrig påhittade suffix.

## Format: riktig RIVTA-XML, inte påhittade JSON-stubbar

Filerna är omarbetade från JSON-stubbar (fram till 2026-10-05) till riktig XML, genererad
genom att marshalla objekt av de faktiska Java-domänklasserna
(`se.inera.ehds.mapping.rivta.GetDiagnosisResponse` m.fl.) med JAXB – samma kodväg som
`fhir-server` faktiskt konsumerar. Namnrymdsuppdelning (responder- vs core-namnrymd) och
elementordning följer samma mönster som är bekräftat korrekt för GetCareDocumentation mot
dess officiella XSD (se `GetCareDocumentationSchemaValidationTest`).

**Känd begränsning:** den officiella XSD:n för
`GetDiagnosisResponder:2`/`clinicalprocess:activity:conditions:2` kunde inte hittas i den
här miljön (se `CoreNamespace.java` i `rivta`-paketet för detaljer om vad som söktes).
Strukturen nedan är därför en välgrundad men **overifierad** tillämpning av det bekräftade
RIVTA-mönstret, inte en XSD-validerad garanti som för GetCareDocumentation. Hör av dig om
du har tillgång till den riktiga tjänstekontraktsbeskrivningen.

Fem av scenarierna nedan är (så vitt vi kan bedöma utan den riktiga XSD:n) schema-**giltig**
XML – de är ogiltiga bara på affärslogiknivå, vilket är precis vad de ska testa.
`personnummer-saknas.xml` är ett undantag: att utelämna `patientId` helt är sannolikt
schema-**OGILTIGT** (varje bekräftad RIVTA-header vi sett kräver sin huvud-identifierare),
så den filen är medvetet schema-ogiltig och hålls isär från de övriga – se tabellen.

| Fil | Testfall | Förväntat beteende | Schema-status |
|---|---|---|---|
| `diagnoskod-fran-kodverk-utan-oid-mappning.xml` | Diagnoskod med `codeSystem`-OID som saknas i `naming-systems.yaml` | `Condition.code.coding.system` = `urn:oid:<oid>` (slask-URI byggd från systemId) | Schema-giltig |
| `diagnostyp-utan-konceptmappning.xml` | `diagnosisType` saknar post i `concept-maps/diagnosis-type.yaml` | `category[diagnostyp]` fylls **inte** i med en gissad kod – `extension[data-absent-reason] = unknown` sätts i stället, inget undantag (DIAG-003) | Schema-giltig |
| `personnummer-saknas.xml` | `patientId` saknas helt i `diagnosisHeader` | Hela diagnosposten filtreras bort, ingen Condition produceras (DIAG-001) | **Medvetet schema-ogiltig** (saknar sannolikt obligatoriskt fält) |
| `personnummer-i-fel-format.xml` | `patientId.extension` är inte 12 siffror (giltigt personnummer/samordningsnummer-format) | Hela diagnosposten filtreras bort, på samma sätt som vid saknat personnummer (DIAG-001) | Schema-giltig (`extension` är fri text på XSD-nivå; formatkravet är en affärsregel) |
| `icke-ok-resultcode.xml` | `result.resultCode` ≠ `OK` | Mapper returnerar tom lista | Schema-giltig (`ERROR` är ett giltigt värde i RIVTA:s ResultCodeEnum) |
| `null-tomt-indata.xml` | Tomt `diagnosis`-fält i TK-svaret | Mapper returnerar tom lista, inget undantag | Schema-giltig (`diagnosis` har `minOccurs=0`) |

Se `ig/input/pagecontent/mapping-getdiagnosis.md` (avsnitt "PoC-begränsningar" samt
"Hantering av diagnosTyp") för de dokumenterade besluten DIAG-001 och DIAG-003.
