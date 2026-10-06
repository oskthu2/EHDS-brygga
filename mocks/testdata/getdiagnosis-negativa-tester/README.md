# Negativa testfall – GetDiagnosis → Condition

Testdata för de negativa acceptanskriterierna i Del A (GetDiagnosis → Condition), ett
scenario per fil, namngivet efter testfallet. Används som referens-/QA-underlag vid sidan
av de automatiserade enhetstesterna i
`bridge/mapping-engine/src/test/java/.../getdiagnosis/GetDiagnosisMapperTest.java`
(`@Nested class NegativaAcceptanskriterier`), som är den auktoritativa verifieringen.

Vårdgivar-HSA-id:n är hämtade från `mocks/testdata/test-vardgivare.csv` (Aleris,
Danderyds sjukhus) i enlighet med konventionen att alltid använda riktiga test-HSA-id:n,
aldrig påhittade suffix.

## Format: riktig RIVTA-XML, verifierad mot den officiella XSD:n

Filerna är genererade genom att marshalla objekt av de faktiska Java-domänklasserna
(`se.inera.ehds.mapping.rivta.GetDiagnosisResponse` m.fl.) med JAXB – samma kodväg som
`fhir-server` faktiskt konsumerar.

**Uppdatering 2026-10-05:** den officiella XSD:n för `GetDiagnosisResponder:2` hittades
(Bitbucket-repot `rivta-domains/riv.clinicalprocess.healthcond.description`,
`schemas/interactions/GetDiagnosisInteraction/GetDiagnosisResponder_2.0.xsd` +
`schemas/core_components/clinicalprocess_healthcond_description_2.1.xsd`). GetDiagnosis:2
visade sig ligga i samma RIVTA-domänrepo som GetCareDocumentation
(`healthcond.description`), inte i `clinicalprocess:activity:conditions` som en
tidigare, overifierad version av domänmodellen gissade genom analogi. Alla sex filer
nedan är omarbetade från den gamla, overifierade strukturen till den riktiga, och är nu
XSD-validerade mot den riktiga, bundlade schemafilen
(`bridge/mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/`),
se `GetDiagnosisSchemaValidationTest`.

Fyra av scenarierna nedan är schema-**giltig** XML – de är ogiltiga bara på
affärslogiknivå, vilket är precis vad de ska testa. Två scenarier är (nu bekräftat,
inte bara misstänkt) schema-**ogiltiga** – se tabellen.

| Fil | Testfall | Förväntat beteende | Schema-status |
|---|---|---|---|
| `diagnoskod-fran-kodverk-utan-oid-mappning.xml` | Diagnoskod med `codeSystem`-OID som saknas i `naming-systems.yaml` | `Condition.code.coding.system` = `urn:oid:<oid>` (slask-URI byggd från systemId) | Schema-giltig |
| `diagnostyp-utan-konceptmappning.xml` | `typeOfDiagnosis` saknar post i `concept-maps/diagnosis-type.yaml` | `category[diagnostyp]` fylls **inte** i med en gissad kod – `extension[data-absent-reason] = unknown` sätts i stället, inget undantag (DIAG-003) | **Schema-ogiltig** (`typeOfDiagnosis` är `DiagnosisTypeEnum`, begränsad till exakt `"Huvuddiagnos"`/`"Bidiagnos"` – `xmllint` avvisar `"OKÄND-TYP"`. Mapperns Java-enhetstest i `GetDiagnosisMapperTest` övar fortfarande denna defensiva kodväg direkt mot domänobjekten, men den här XML-filen representerar numera ett scenario som en riktig producent inte kan skicka om den själv validerar mot schemat – kvar som dokumentation av mapperns försvar mot malformat/framtida indata) |
| `personnummer-saknas.xml` | `patientId` saknas helt i `diagnosisHeader` | Hela diagnosposten filtreras bort, ingen Condition produceras (DIAG-001) | **Schema-ogiltig** (`patientId` är obligatoriskt, `minOccurs=1`, i `PatientSummaryHeaderType` – bekräftat av `xmllint` mot den riktiga XSD:n) |
| `personnummer-i-fel-format.xml` | `patientId.id` är inte 12 siffror (giltigt personnummer/samordningsnummer-format) | Hela diagnosposten filtreras bort, på samma sätt som vid saknat personnummer (DIAG-001) | Schema-giltig (`id` är fri text på XSD-nivå; formatkravet är en affärsregel) |
| `icke-ok-resultcode.xml` | `result.resultCode` ≠ `OK` | Mapper returnerar tom lista | Schema-giltig (`ERROR` är ett giltigt värde i RIVTA:s `ResultCodeEnum`) |
| `null-tomt-indata.xml` | Tomt `diagnosis`-fält i TK-svaret | Mapper returnerar tom lista, inget undantag | Schema-giltig (`diagnosis` har `minOccurs=0`) |

Se `ig/input/pagecontent/mapping-getdiagnosis.md` (avsnitt "PoC-begränsningar" samt
"Hantering av diagnosTyp") för de dokumenterade besluten DIAG-001 och DIAG-003.
