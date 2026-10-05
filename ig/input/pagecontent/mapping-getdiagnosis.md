# Mappning: GetDiagnosis → Condition

## Bakgrund

RIVTA-tjänstekontraktet `clinicalprocess:activity:conditions:GetDiagnosis:2` (förkortat `GetDiagnosis:2`)
används för att hämta diagnosinformation från svenska journalsystem via Ineras nationella tjänsteplattform.

EHDS-bryggan mappar svarsmeddelandet från detta tjänstekontrakt till FHIR R4-resursen
[Condition](https://hl7.org/fhir/R4/condition.html), profilerad som
[SEEHDSCondition](StructureDefinition-se-ehds-condition.html).

## Mappningstabell

| RIVTA-element | FHIR-element | Kommentar |
|---|---|---|
| `diagnosisHeader.patientId.id` | `Condition.subject.identifier.value` | Personnummer eller samordningsnummer – subject är SEEHDSPatient |
| `diagnosisHeader.patientId.type` | `Condition.subject.identifier.system` | OID konverteras till URI, se tabell nedan |
| `diagnosisHeader.sourceSystemHSAId` | `Condition.meta.source` | Källsystemets Endpoint i tjänstekatalogen (https://tjanstekatalogen.inera.se/Endpoint/{hsaId}) |
| `diagnosisHeader.accountableHealthcareProfessional.authorTime` | `Condition.recordedDate` | Format YYYYMMDDHHMMSS → ISO 8601 |
| `diagnosisBody.diagnosisCode.code` | `Condition.code.coding.code` | ICD-10-SE kod, t.ex. `J18.9` |
| `diagnosisBody.diagnosisCode.codeSystem` | `Condition.code.coding.system` | OID `1.2.752.116.1.1.1.1.3` → `https://www.icd10.se/` |
| `diagnosisBody.diagnosisCode.displayName` | `Condition.code.coding.display` | Kodverkets officiella benämning |
| `diagnosisBody.diagnosisCode.originalText` | `Condition.code.text` | Fritext från källsystemet; om saknad används `displayName` som fallback |
| `diagnosisBody.typeOfDiagnosis` (Huvuddiagnos) | `Condition.category[diagnostyp]` = `HD` (kv_diagnostyp) | Huvuddiagnos → Ineras kv_diagnostyp-kod |
| `diagnosisBody.typeOfDiagnosis` (Bidiagnos) | `Condition.category[diagnostyp]` = `BY` (kv_diagnostyp) | Bidiagnos → Ineras kv_diagnostyp-kod |
| `diagnosisBody.diagnosisTime` | `Condition.onsetDateTime` | Format YYYYMMDDHHMMSS → ISO 8601 |
| `diagnosisHeader.accountableHealthcareProfessional` | `Condition.recorder` (Reference(PractitionerRole)) | Ansvarig hälso- och sjukvårdspersonal – logisk referens via HSA-id |
| `diagnosisHeader.legalAuthenticator` | `Condition.asserter` (Reference(PractitionerRole)) | Rättslig äkthetsintygsgivare – logisk referens via HSA-id |
| `diagnosisHeader.legalAuthenticator.signatureTime` | `Condition.extension[assertedDate]` | Administrativt intygsgivningsdatum (YYYYMMDDHHMMSS → ISO 8601) |
| `diagnosisBody.chronicDiagnosis` | `Condition.extension[chronicDiagnosis]` | Boolean – om diagnosen är klassad som kronisk |
| `diagnosisBody.relatedDiagnosis[].documentId` | `Condition.extension[relatedCondition]` | Logisk referens (Identifier) till relaterad diagnos via källsystemets dokumentid – en extension per post (`relatedDiagnosis` är 0..* i det riktiga schemat) |
| `diagnosisHeader.accountableHealthcareProfessional.healthcareProfessionalCareGiverHSAId` | `Provenance.agent[custodian]` | Juridiskt ansvarig vårdgivare – används för Sparr |
| `diagnosisHeader.accountableHealthcareProfessional.healthcareProfessionalCareUnitHSAId` | `Provenance.agent[author]` | Informationsägare vårdenhet |
| `diagnosisHeader.accountableHealthcareProfessional.authorTime` | `Provenance.recorded` | Tidsstämpel för Provenance |
| (bryggan självt, `EHDS_BRIDGE_HSA_ID`) | `Provenance.agent[assembler]` | Bryggan som sammansättande aktör |

**Not:** den riktiga `PatientSummaryHeaderType` (verifierad mot den officiella RIVTA-XSD:n,
se "Härledning av clinicalStatus" nedan) har inga egna `careProviderHSAId`/`careUnitHSAId`-fält
på headern – vårdgivare och vårdenhet hämtas i stället från
`accountableHealthcareProfessional`.

## healthcareProfessionalType → PractitionerRole

Både `accountableHealthcareProfessional` och `legalAuthenticator` är av RIVTA-typen
`healthcareProfessionalType`, som innehåller uppgifter om en person och deras yrkesroll
vid tidpunkten för dokumentet. Dessa mappas till FHIR `PractitionerRole` som anges
som logisk referens via HSA-identifierare.

| RIVTA-underelement | FHIR PractitionerRole-fält | Kommentar |
|---|---|---|
| `healthcareProfessionalHSAId` / `legalAuthenticatorHSAId` | `PractitionerRole.identifier.value` | Flata HSA-id-strängar (inte en nästlad personId-struktur) |
| (fast HSA-OID, `1.2.752.129.2.1.4.1`) | `PractitionerRole.identifier.system` | OID→URI via NamingSystemRegistry – det riktiga schemat har bara en HSA-id-sträng per roll, inget eget OID-fält att läsa av |
| `healthcareProfessionalRoleCode` | `PractitionerRole.code` | Yrkeskategori/roll vid tillfället |

`accountableHealthcareProfessional` mappas till `Condition.recorder` och
`legalAuthenticator` mappas till `Condition.asserter`. Datum för `legalAuthenticator`
placeras i `Condition.extension[assertedDate]`.

## EU-profiler i meta.profile

Varje producerad Condition bär **två profiler** i `meta.profile`:

| Profil | URL | Syfte |
|---|---|---|
| SEEHDSCondition | `https://ehds-brygga.inera.se/fhir/StructureDefinition/se-ehds-condition` | Bryggornas nationella RIVTA-mappningsprofil |
| condition-obl-eu-eps | `http://hl7.eu/fhir/eps/StructureDefinition/condition-obl-eu-eps` | EU EPS obligations-profil (hl7.fhir.eu.eps) |

EU EPS-profilen (`condition-obl-eu-eps`) är en *obligations*-profil på toppen av IPS Condition
som specificerar EU EHDS-krav för klinisk status, verifieringsstatus och kod.
Bryggan sätter båda profilerna ovillkorligt på varje Condition den producerar.

## OID till URI-mappningar

RIVTA använder OID-identifierare (Object Identifiers) för kodsystem och personidentifierare.
FHIR föredrar URI:er. EHDS-bryggan utför följande konverteringar via `NamingSystemRegistry`.
Se [OID-till-URI-mappningar](naming-systems.html) för den kompletta tabellen med alla 12 registrerade OID:er.

De OID:er som förekommer i GetDiagnosis:2-svar:

| OID | URI | Beskrivning |
|---|---|---|
| `1.2.752.129.2.1.3.1` | `http://electronichealth.se/identifier/personnummer` | Personnummer |
| `1.2.752.129.2.1.3.3` | `http://electronichealth.se/identifier/samordningsnummer` | Samordningsnummer |
| `1.2.752.129.2.1.4.1` | `urn:oid:1.2.752.129.2.1.4.1` | HSA-id (Inera NTjP) |
| `1.2.752.29.4.19` | `urn:oid:1.2.752.29.4.19` | HSA-id (HL7 Sweden basprofiler) |
| `1.2.752.116.1.1.1.1.3` | `https://www.icd10.se/` | ICD-10-SE (primärt diagnoskodsystem) |
| `2.16.840.1.113883.6.3` | `http://hl7.org/fhir/sid/icd-10` | ICD-10 (internationell, WHO) |
| `2.16.840.1.113883.6.96` | `http://snomed.info/sct` | SNOMED CT |

OID:er som inte har en känd URI-mappning bevaras som `urn:oid:{oid}`.

Profilen deklarerar ett namngivet snitt `code.coding[ICD10SE]` för ICD-10-SE-koder
(`system = https://www.icd10.se/`). Övriga kodsystem (ICD-10 international, SNOMED CT) placeras
i ej namngivna snitt (`code.coding` utan slice-begränsning) — profilen är öppen (`rules = #open`).

Provenance-resurser inkluderas i sökbundlen med `searchMode = include` och refererar till sitt Condition via `Provenance.target`.

| FHIR-resurs | Koppling | Beskrivning |
|---|---|---|
| `Provenance.target` | `urn:uuid:{Condition.id}` | Provenance beskriver denna Condition |
| `Provenance.agent[custodian]` | `accountableHealthcareProfessional.healthcareProfessionalCareGiverHSAId` | Juridiskt ansvarig vårdgivare (organisationsnivå, yttre Sparr) |
| `Provenance.agent[author]` | `accountableHealthcareProfessional.healthcareProfessionalCareUnitHSAId` | Informationsägare vårdenhet (inre Sparr) |
| `Provenance.agent[assembler]` | `EHDS_BRIDGE_HSA_ID` | Bryggan som sammansatte FHIR-svaret |

## Härledning av clinicalStatus

Det riktiga `GetDiagnosis:2`-kontraktet (verifierat mot den officiella RIVTA-XSD:n,
`clinicalprocess_healthcond_description_2.1.xsd`) har inget period- eller
slutdatumskoncept för en diagnos – `diagnosisTime` är en enda tidpunkt, inte ett
intervall. `Condition.clinicalStatus` sätts därför **alltid** till `active`:

| `diagnosisBody` | `Condition.clinicalStatus` | Förklaring |
|---|---|---|
| (alltid) | `active` | Schemat har inget fält som kan uttrycka att en diagnos är avslutad/resolved |

`Condition.verificationStatus` sätts alltid till `confirmed` vid mappning från RIVTA,
eftersom RIVTA-svar representerar bekräftade journaluppgifter.

Eftersom `clinicalStatus` inte härleds från något källfält alls (till skillnad från en
tidigare, overifierad version av denna mappning som uppfann ett `diagnosisTimePeriod.end`
som inte finns i det riktiga schemat) kan `clinicalStatus` aldrig bli tvetydig eller
omappbar – fallet "statusvärde som inte kan mappas entydigt" kan inte uppstå med dagens
`GetDiagnosis:2`-kontrakt. Om en framtida kontraktsversion inför ett riktigt
status-/slutdatumfält bör denna härledning uppdateras.

## Hantering av diagnosTyp

RIVTA-koden för diagnostyp (`typeOfDiagnosis`) används för att sätta `Condition.category`.
Den riktiga `DiagnosisTypeEnum` (XSD-verifierad) begränsar `typeOfDiagnosis` till exakt de
svenska textvärdena `"Huvuddiagnos"`/`"Bidiagnos"` – **inte** förkortningarna `HD`/`BY` som
en tidigare, overifierad version av denna mappning antog. Se även
[ConceptMap DiagnosisTypeToCategoryMap](ConceptMap-DiagnosisTypeToCategoryMap.html)
för den fullständiga mappningen.

| RIVTA typeOfDiagnosis | FHIR category[diagnostyp]-kod | System |
|---|---|---|
| `Huvuddiagnos` | `HD` | `https://terminologitjansten.inera.se/inera-kodverksforvaltning/kodverk/kv_diagnostyp` |
| `Bidiagnos` | `BY` | `https://terminologitjansten.inera.se/inera-kodverksforvaltning/kodverk/kv_diagnostyp` |

Profilen kräver exakt ett `category[diagnostyp]`-snitt med en kod från Ineras kodverk `kv_diagnostyp`.
Ytterligare `category`-poster (t.ex. `encounter-diagnosis` från standard-FHIR) kan läggas till av konsumenten
men hanteras inte av denna profil.

**Fallback (DIAG-003):** Om `typeOfDiagnosis` saknar en känd mappning i `ConceptMapRegistry` fylls
`category[diagnostyp]` **inte** i med en gissad kod – att återanvända den råa RIVTA-koden som om
den vore en giltig `kv_diagnostyp`-kod skulle ge en felaktig kodning. I stället sätts
`category[diagnostyp]` till en `CodeableConcept` utan `coding`, med extensionen

```json
{
  "url": "http://hl7.org/fhir/StructureDefinition/data-absent-reason",
  "valueCode": "unknown"
}
```

Condition inkluderas ändå i svaret – mappningen kastar aldrig undantag på grund av en okänd
diagnostyp, men category[diagnostyp] saknar en bärande kod i detta fall.

## Datumsformat

RIVTA använder heltalssträngar för datum och tidsstämplar. FHIR kräver ISO 8601-format.

| RIVTA-format | Exempel | FHIR-format | Exempel |
|---|---|---|---|
| `YYYYMMDDHHMMSS` | `20230601120000` | `YYYY-MM-DDTHH:MM:SS` | `2023-06-01T12:00:00` |
| `YYYYMMDD` | `20230601` | `YYYY-MM-DD` | `2023-06-01` |

Tidszoner anges inte explicit i RIVTA-kontraktet. EHDS-bryggan tolkar alla tider som lokal
svensk tid (Europe/Stockholm) och konverterar till UTC vid behov.

## Exempel: GetDiagnosis-svar och resulterande FHIR Condition

### RIVTA XML-svar (GetDiagnosis:2)

```xml
<ns1:diagnosis>
  <ns1:diagnosisHeader>
    <ns1:documentId>doc-example-1</ns1:documentId>
    <ns1:sourceSystemHSAId>SE2321000016-4HK5</ns1:sourceSystemHSAId>
    <ns1:patientId>
      <ns1:id>191212121212</ns1:id>
      <ns1:type>1.2.752.129.2.1.3.1</ns1:type>
    </ns1:patientId>
    <ns1:accountableHealthcareProfessional>
      <ns1:authorTime>20230601120000</ns1:authorTime>
      <ns1:healthcareProfessionalHSAId>SE2321000016-DOK</ns1:healthcareProfessionalHSAId>
      <ns1:healthcareProfessionalCareGiverHSAId>SE2321000016-4HK5</ns1:healthcareProfessionalCareGiverHSAId>
    </ns1:accountableHealthcareProfessional>
    <ns1:legalAuthenticator>
      <ns1:signatureTime>20230601000000</ns1:signatureTime>
      <ns1:legalAuthenticatorHSAId>SE2321000016-AUTH</ns1:legalAuthenticatorHSAId>
    </ns1:legalAuthenticator>
    <ns1:approvedForPatient>true</ns1:approvedForPatient>
  </ns1:diagnosisHeader>
  <ns1:diagnosisBody>
    <ns1:typeOfDiagnosis>Huvuddiagnos</ns1:typeOfDiagnosis>
    <ns1:diagnosisTime>20230601000000</ns1:diagnosisTime>
    <ns1:diagnosisCode>
      <ns1:code>J18.9</ns1:code>
      <ns1:codeSystem>1.2.752.116.1.1.1.1.3</ns1:codeSystem>
      <ns1:displayName>Pneumoni, ospecificerad</ns1:displayName>
    </ns1:diagnosisCode>
  </ns1:diagnosisBody>
</ns1:diagnosis>
```

### Resulterande FHIR Condition (JSON)

```json
{
  "resourceType": "Condition",
  "id": "example-pneumoni",
  "meta": {
    "source": "https://tjanstekatalogen.inera.se/Endpoint/SE2321000016-4HK5",
    "profile": [
      "https://ehds-brygga.inera.se/fhir/StructureDefinition/se-ehds-condition",
      "http://hl7.eu/fhir/eps/StructureDefinition/condition-obl-eu-eps"
    ]
  },
  "extension": [
    {
      "url": "https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-asserted-date",
      "valueDateTime": "2023-06-01"
    }
  ],
  "clinicalStatus": {
    "coding": [
      {
        "system": "http://terminology.hl7.org/CodeSystem/condition-clinical",
        "code": "active"
      }
    ]
  },
  "verificationStatus": {
    "coding": [
      {
        "system": "http://terminology.hl7.org/CodeSystem/condition-ver-status",
        "code": "confirmed"
      }
    ]
  },
  "category": [
    {
      "coding": [
        {
          "system": "https://terminologitjansten.inera.se/inera-kodverksforvaltning/kodverk/kv_diagnostyp",
          "code": "HD",
          "display": "Huvuddiagnos"
        }
      ]
    }
  ],
  "code": {
    "coding": [
      {
        "system": "https://www.icd10.se/",
        "code": "J18.9",
        "display": "Pneumoni, ospecificerad"
      }
    ]
  },
  "subject": {
    "identifier": {
      "system": "http://electronichealth.se/identifier/personnummer",
      "value": "191212121212"
    }
  },
  "onsetDateTime": "2023-06-01T00:00:00",
  "recordedDate": "2023-06-01T12:00:00",
  "recorder": {
    "type": "PractitionerRole",
    "identifier": {
      "system": "urn:oid:1.2.752.129.2.1.4.1",
      "value": "SE2321000016-DOK"
    }
  },
  "asserter": {
    "type": "PractitionerRole",
    "identifier": {
      "system": "urn:oid:1.2.752.129.2.1.4.1",
      "value": "SE2321000016-AUTH"
    }
  }
}
```

### Förklaring av mappningen i exemplet

- `clinicalStatus = active` – det riktiga schemat har inget slutdatum/period-koncept för en diagnos, så clinicalStatus sätts alltid till active
- `verificationStatus = confirmed` – sätts alltid vid mappning från RIVTA
- `category[diagnostyp].coding.code = HD` – `typeOfDiagnosis = Huvuddiagnos` mappas via ConceptMap till Ineras kv_diagnostyp-kod `HD`
- `code.coding.system = https://www.icd10.se/` – OID `1.2.752.116.1.1.1.1.3` konverteras till ICD-10-SE URI
- `subject.identifier.system = http://electronichealth.se/identifier/personnummer` – OID `1.2.752.129.2.1.3.1` (från `patientId.type`) konverteras till kanonisk URI (HL7 Sweden basprofiler)
- `recordedDate` – `accountableHealthcareProfessional.authorTime` = `20230601120000` konverteras till `2023-06-01T12:00:00`
- `onsetDateTime` – `diagnosisTime` = `20230601000000` konverteras till `2023-06-01T00:00:00`
- `meta.source = https://tjanstekatalogen.inera.se/Endpoint/SE2321000016-4HK5` – källsystemets Endpoint i tjänstekatalogen
- `recorder` – `accountableHealthcareProfessional.healthcareProfessionalHSAId` mappas till logisk PractitionerRole-referens
- `asserter` – `legalAuthenticator.legalAuthenticatorHSAId` mappas till logisk PractitionerRole-referens
- `extension[assertedDate]` – `legalAuthenticator.signatureTime` konverteras till `2023-06-01T00:00:00`

## Fältvalidering

Profilen [SEEHDSCondition](StructureDefinition-se-ehds-condition.html) kräver följande fält (kardinalitet 1..1 eller 1..*):

- `clinicalStatus` – alltid satt till `active` (det riktiga schemat har inget slutdatum/period-koncept att härleda `resolved` från)
- `verificationStatus` – alltid satt till `confirmed`
- `category[diagnostyp]` – alltid satt; innehåller en kod från kv_diagnostyp om `typeOfDiagnosis`
  gick att konceptmappa, annars `extension[data-absent-reason] = unknown` utan `coding` (DIAG-003)
- `code` – diagnoskod med minst en coding
- `subject.identifier` – patientidentifierare med system och value; posten filtreras bort
  (ingen Condition produceras) om denna inte går att sätta med ett giltigt format (DIAG-001)

Valfria fält (0..1) som sätts när källdata finns:

- `recorder` – sätts om `accountableHealthcareProfessional.healthcareProfessionalHSAId` finns i RIVTA-svaret
- `recordedDate` – sätts om `accountableHealthcareProfessional.authorTime` finns i RIVTA-svaret
- `asserter` – sätts om `legalAuthenticator.legalAuthenticatorHSAId` finns i RIVTA-svaret
- `extension[assertedDate]` – sätts om `legalAuthenticator.signatureTime` finns
- `onsetDateTime` – sätts om `diagnosisBody.diagnosisTime` finns
- `extension[chronicDiagnosis]` – sätts om `diagnosisBody.chronicDiagnosis` finns (true eller false)
- `extension[relatedCondition]` – en extension per post i `diagnosisBody.relatedDiagnosis` (0..*) med satt `documentId`

## Must Support-element som inte kan mappas

`SEEHDSCondition` ärver Must Support-flaggor från `$ipsCondition` (IPS Condition, version 2.0.0)
och lägger dessutom till egna. Nedanstående element är Must Support men fylls **aldrig** i av
GetDiagnosis-mappningen, eftersom tjänstekontraktet inte bär motsvarande uppgift:

| Element | Must Support-källa | Varför GetDiagnosis inte kan fylla i det |
|---|---|---|
| `Condition.bodySite` | `SEEHDSCondition` (egen MS-flagga, `0..1`, SNOMED CT `preferred`) | `diagnosisBody` har inget fält för kroppslokalisation – varken kodat eller som fritext. |
| `Condition.severity` | Ärvd från IPS Condition 2.0.0 (`0..1`) | `diagnosisBody` har inget svårighetsgradsfält. |
| `Condition.note` | `SEEHDSCondition` (egen MS-flagga, `0..*`) | GetDiagnosis har ingen fritext kopplad till den enskilda diagnosen – `diagnosisBody` innehåller bara `diagnosisCode`, `diagnosisType`, `diagnosisTimePeriod`, `chronicCondition` och `relatedDiagnosis`. |
| `Condition.subject.reference` | Ärvd från IPS Condition 2.0.0, där elementet är `1..1` MS (obligatoriskt) | Medvetet avsteg – se GENERAL-002 nedan. `subject` sätts alltid som logisk referens via `subject.identifier`; bryggan bundlar aldrig en `Patient`-resurs och sätter därför aldrig `subject.reference`. |

Att dessa saknas i testdatan är alltså förväntat, inte ett mappningsfel.

**Två punkter som ofta missuppfattas som saknade Must Support-element:**

- **`Condition.abatement[x]` är redan mappat** (`diagnosisBody.diagnosisTimePeriod.end` →
  `Condition.abatementDateTime`, se `abatementDateTime` under "Fältvalidering" ovan).
  Det är ett valfritt fält (`0..1`) som bara sätts när RIVTA-svaret har ett slutdatum på
  diagnosperioden – att det saknas i ett specifikt testfall betyder bara att den diagnosen
  fortfarande är aktiv (inget slutdatum), inte att mappningen saknar stöd för fältet.
- **`Condition.category.text` är inte ett Must Support-element.** Varken `$ipsCondition` eller
  `SEEHDSCondition` sätter en MS-flagga på `category.text` (kontrollerat mot den upplösta
  snapshot:en för `Condition-uv-ips` 2.0.0) – endast själva `category[diagnostyp]`-kodningen är
  Must Support och obligatorisk. Om avsaknaden av fritext i `category` ändå var det som
  observerades: `Condition.code.text` sätts redan idag från `diagnosisCode.originalText`
  (med `displayName` som fallback), se mappningstabellen ovan.

## Provenance

För varje Condition skapas en Provenance-resurs som inkluderas i sökbundlen med `Bundle.entry.search.mode = include`. Provenance-resursen bär den fullständiga provenanskedjan:

| Agent-roll | Källa | Syfte |
|---|---|---|
| `custodian` | `diagnosisHeader.accountableHealthcareProfessional.healthcareProfessionalCareGiverHSAId` | Juridiskt ansvarig vårdgivare – används av Sparrtjänsten |
| `author` | `diagnosisHeader.accountableHealthcareProfessional.healthcareProfessionalCareUnitHSAId` | Informationsägare vårdenhet |
| `assembler` | `EHDS_BRIDGE_HSA_ID` (env-variabel) | EHDS-bryggan som sammansatte FHIR-bundlen |

`Provenance.recorded` sätts till `diagnosisHeader.accountableHealthcareProfessional.authorTime`
(konverterad till ISO 8601 + UTC) – samma källa som `Condition.recordedDate`. Om `authorTime`
saknas används aktuell systemtid.

`Provenance.recorded` återanvänds även som jämförelsetidpunkt (`comparisonTime`, "CheckBlocks-tid")
i anropet till spärrtjänsten, se [Säkerhetstjänsten (Spärr)](architecture.html#sakerhetstjansten-sparr)
i arkitekturdokumentationen.

Provenance-resursen refererar Condition via `Provenance.target = urn:uuid:{Condition.id}`.

## PoC-begränsningar

### GENERAL-002: Patientreferens – logisk referens istället för bundlad Patient

`Condition.subject` är en **logisk referens** (`subject.identifier` med personnummer/
samordningsnummer) — bryggan sätter aldrig `subject.reference` och bundlar aldrig en
`Patient`-resurs tillsammans med `Condition`en. `SEEHDSCondition` ärver från
`$ipsCondition`, och IPS:s normalfall förutsätter att dokumentet är en självständig
`Bundle` där `Patient` är en riktig, medskickad resurs som andra resurser pekar på via en
literal `subject.reference`.

**Medvetet avsteg (PoC-scope):** Varje TK-anrop (`GetDiagnosis`/`GetCareDocumentation`)
mappas idag till fristående resurser, inte en komplett IPS-`Bundle` med patient inkluderad.
Att börja bygga och bundla en fullständig `Patient`-resurs per anrop är en större ändring
(ny mapper-logik + Bundle-hantering) som inte är gjord i denna PoC. `SEEHDSPatient`-profilen
finns och används som typ för referensen (`Reference($seEhdsPatient)`), men själva
instansen bundlas inte — endast identifieraren bärs vidare. Samma avsteg gäller
`DocumentReference.subject` i `mapping-getcaredocumentation.md` (GENERAL-002 där).

### DIAG-001: Saknat eller felaktigt personnummer stoppar posten

`SEEHDSCondition` kräver `subject.identifier` (kardinalitet 1..1) – en Condition utan en
tillförlitlig patientidentifierare är inte bara avvikande, den är oanvändbar och riskerar att
hamna fel om den ändå levereras. Bryggan validerar därför att `diagnosisHeader.patientId.id`
matchar ett personnummer/samordningsnummer utan bindestreck (`^\d{12}$`, dvs. ÅÅÅÅMMDD + 4 siffror).

Om `patientId` saknas helt, eller `id` inte matchar detta format, **filtreras hela
diagnosposten bort** – `mapDiagnosis` returnerar `null` för just den posten, på samma sätt som vid
ett saknat `diagnosisHeader`/`diagnosisBody`. Övriga poster i samma TK-svar påverkas inte.
Mappningen kastar inget undantag; det är bara den enskilda posten som uteblir ur resultatet.

Valideringen är ett formatkrav, inte en PU-slagning – MVP 3 gör fortfarande ingen kontroll mot
personuppgiftsregistret (inget facit att validera mot utöver formatet). En framtida
PU-integration skulle kunna ersätta eller komplettera formatkontrollen med en riktig uppslagning.

### Spärr: inre och yttre
EHDS-bryggan är avsedd för cross-border och ska applicera alla spärrar. Sparrkontrollen sker mot `careProviderHSAId` (organisationsnivå) i enlighet med Ineras spärrtjänst som beskrivs på [Ineras konfluensida](https://inera.atlassian.net/wiki/spaces/PIS/pages/3435203724/).

**Utanför PoC-scope:** En vårdgivare som tillhör en spärrad enhet men ändå har rätt att ta del av informationen (t.ex. nödsituationer / break-the-glass) hanteras inte. Denna logik kräver kontextinformation om inloggad användares behörighet och är out of scope för PoC:en.
