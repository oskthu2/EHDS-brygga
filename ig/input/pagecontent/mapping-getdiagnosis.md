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
| `diagnosisHeader.patientId.extension` | `Condition.subject.identifier.value` | Personnummer eller samordningsnummer – subject är SEEHDSPatient |
| `diagnosisHeader.patientId.root` | `Condition.subject.identifier.system` | OID konverteras till URI, se tabell nedan |
| `diagnosisHeader.sourceSystemHSAId` | `Condition.meta.source` | Källsystemets HSA-id som URI (urn:oid:{OID}#{hsaId}) |
| `diagnosisHeader.documentTime` | `Condition.recordedDate` | Format YYYYMMDDHHMMSS → ISO 8601 |
| `diagnosisBody.diagnosisCode.code` | `Condition.code.coding.code` | ICD-10-SE kod, t.ex. `J18.9` |
| `diagnosisBody.diagnosisCode.codeSystem` | `Condition.code.coding.system` | OID `1.2.752.116.1.1.1.1.3` → `https://www.icd10.se/` |
| `diagnosisBody.diagnosisCode.displayName` | `Condition.code.coding.display` | Kodverkets officiella benämning |
| `diagnosisBody.diagnosisCode.originalText` | `Condition.code.text` | Fritext från källsystemet; om saknad används `displayName` som fallback |
| `diagnosisBody.diagnosisType` (HD) | `Condition.category[diagnostyp]` = `HD` (kv_diagnostyp) | Huvuddiagnos → Ineras kv_diagnostyp-kod |
| `diagnosisBody.diagnosisType` (BY) | `Condition.category[diagnostyp]` = `BY` (kv_diagnostyp) | Bidiagnos → Ineras kv_diagnostyp-kod |
| `diagnosisBody.diagnosisTimePeriod.start` | `Condition.onsetDateTime` | Format YYYYMMDD → YYYY-MM-DD |
| `diagnosisBody.diagnosisTimePeriod.end` | `Condition.abatementDateTime` | Om satt: resolved, annars active |
| `diagnosisHeader.accountableHealthcareProfessional` | `Condition.recorder` (Reference(PractitionerRole)) | Ansvarig hälso- och sjukvårdspersonal – logisk referens via HSA-id |
| `diagnosisHeader.legalAuthenticator` | `Condition.asserter` (Reference(PractitionerRole)) | Rättslig äkthetsintygsgivare – logisk referens via HSA-id |
| `diagnosisHeader.legalAuthenticator` (datum) | `Condition.extension[assertedDate]` | Administrativt intygsgivningsdatum (YYYYMMDD → YYYY-MM-DD) |
| `diagnosisHeader.careProviderHSAId` | `Provenance.agent[custodian]` | Juridiskt ansvarig vårdgivare – används för Sparr |
| `diagnosisHeader.careUnitHSAId` | `Provenance.agent[author]` | Informationsägare vårdenhet |
| `diagnosisHeader.documentTime` | `Provenance.recorded` | Tidsstämpel för Provenance |
| (bryggan självt, `EHDS_BRIDGE_HSA_ID`) | `Provenance.agent[assembler]` | Bryggan som sammansättande aktör |

## healthcareProfessionalType → PractitionerRole

Både `accountableHealthcareProfessional` och `legalAuthenticator` är av RIVTA-typen
`healthcareProfessionalType`, som innehåller uppgifter om en person och deras yrkesroll
vid tidpunkten för dokumentet. Dessa mappas till FHIR `PractitionerRole` som anges
som logisk referens via HSA-identifierare.

| RIVTA-underelement | FHIR PractitionerRole-fält | Kommentar |
|---|---|---|
| `healthcareProfessional.personId` | `PractitionerRole.identifier.value` | HSA-id för personen |
| `healthcareProfessional.personId.root` | `PractitionerRole.identifier.system` | OID→URI via NamingSystemRegistry |
| `roleAtTime` | `PractitionerRole.code` | Yrkeskategori/roll vid tillfället |

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
| `Provenance.agent[custodian]` | `careProviderHSAId` | Juridiskt ansvarig vårdgivare (organisationsnivå, yttre Sparr) |
| `Provenance.agent[author]` | `careUnitHSAId` | Informationsägare vårdenhet (inre Sparr) |
| `Provenance.agent[assembler]` | `EHDS_BRIDGE_HSA_ID` | Bryggan som sammansatte FHIR-svaret |

## Härledning av clinicalStatus

RIVTA-tjänstekontraktet innehåller inte ett explicit statusfält. `Condition.clinicalStatus` härledas
baserat på förekomsten av slutdatum i diagnosperiodens tidsintervall:

| `diagnosisTimePeriod.end` | `Condition.clinicalStatus` | Förklaring |
|---|---|---|
| Inte satt (null) | `active` | Diagnosen anses fortfarande aktiv |
| Satt (datum finns) | `resolved` | Diagnosen har avslutats |

`Condition.verificationStatus` sätts alltid till `confirmed` vid mappning från RIVTA,
eftersom RIVTA-svar representerar bekräftade journaluppgifter.

## Hantering av diagnosTyp

RIVTA-koden för diagnostyp (`diagnosisType`) används för att sätta `Condition.category`.
Se även [ConceptMap DiagnosisTypeToCategoryMap](ConceptMap-DiagnosisTypeToCategoryMap.html)
för den fullständiga mappningen.

| RIVTA diagnosisType | Kod | System | FHIR category[diagnostyp]-kod |
|---|---|---|---|
| `HD` – Huvuddiagnos | `HD` | `https://terminologitjansten.inera.se/inera-kodverksforvaltning/kodverk/kv_diagnostyp` | Ineras kv_diagnostyp |
| `BY` – Bidiagnos | `BY` | `https://terminologitjansten.inera.se/inera-kodverksforvaltning/kodverk/kv_diagnostyp` | Ineras kv_diagnostyp |

Profilen kräver exakt ett `category[diagnostyp]`-snitt med en kod från Ineras kodverk `kv_diagnostyp`.
Ytterligare `category`-poster (t.ex. `encounter-diagnosis` från standard-FHIR) kan läggas till av konsumenten
men hanteras inte av denna profil.

**Fallback:** Om `diagnosisType` saknar en känd mappning i `ConceptMapRegistry` loggas ett varningsmeddelande.
Condition inkluderas alltid i svaret.

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
    <ns1:patientId>
      <ns1:root>1.2.752.129.2.1.3.1</ns1:root>
      <ns1:extension>191212121212</ns1:extension>
    </ns1:patientId>
    <ns1:sourceSystemHSAId>SE2321000016-4HK5</ns1:sourceSystemHSAId>
    <ns1:documentTime>20230601120000</ns1:documentTime>
    <ns1:accountableHealthcareProfessional>
      <ns1:personId>
        <ns1:root>1.2.752.129.2.1.4.1</ns1:root>
        <ns1:extension>SE2321000016-DOK</ns1:extension>
      </ns1:personId>
    </ns1:accountableHealthcareProfessional>
    <ns1:legalAuthenticator>
      <ns1:hcProfessional>
        <ns1:personId>
          <ns1:root>1.2.752.129.2.1.4.1</ns1:root>
          <ns1:extension>SE2321000016-AUTH</ns1:extension>
        </ns1:personId>
      </ns1:hcProfessional>
      <ns1:signatureDate>20230601</ns1:signatureDate>
    </ns1:legalAuthenticator>
  </ns1:diagnosisHeader>
  <ns1:diagnosisBody>
    <ns1:diagnosisCode>
      <ns1:code>J18.9</ns1:code>
      <ns1:codeSystem>1.2.752.116.1.1.1.1.3</ns1:codeSystem>
      <ns1:displayName>Pneumoni, ospecificerad</ns1:displayName>
    </ns1:diagnosisCode>
    <ns1:diagnosisType>HD</ns1:diagnosisType>
    <ns1:diagnosisTimePeriod>
      <ns1:start>20230601</ns1:start>
    </ns1:diagnosisTimePeriod>
  </ns1:diagnosisBody>
</ns1:diagnosis>
```

### Resulterande FHIR Condition (JSON)

```json
{
  "resourceType": "Condition",
  "id": "example-pneumoni",
  "meta": {
    "source": "urn:oid:1.2.752.129.2.1.4.1#SE2321000016-4HK5",
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
  "onsetDateTime": "2023-06-01",
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

- `clinicalStatus = active` – inget slutdatum i `diagnosisTimePeriod`, så diagnosen är fortfarande aktiv
- `verificationStatus = confirmed` – sätts alltid vid mappning från RIVTA
- `category[diagnostyp].coding.code = HD` – `diagnosisType = HD` (Huvuddiagnos) mappas direkt till Ineras kv_diagnostyp-kod
- `code.coding.system = https://www.icd10.se/` – OID `1.2.752.116.1.1.1.1.3` konverteras till ICD-10-SE URI
- `subject.identifier.system = http://electronichealth.se/identifier/personnummer` – OID `1.2.752.129.2.1.3.1` konverteras till kanonisk URI (HL7 Sweden basprofiler)
- `recordedDate` – `20230601120000` konverteras till `2023-06-01T12:00:00`
- `onsetDateTime` – `20230601` konverteras till `2023-06-01`
- `meta.source = urn:oid:1.2.752.129.2.1.4.1#SE2321000016-4HK5` – källsystemets HSA-id som URI
- `recorder` – `accountableHealthcareProfessional.personId` mappas till logisk PractitionerRole-referens
- `asserter` – `legalAuthenticator.hcProfessional.personId` mappas till logisk PractitionerRole-referens
- `extension[assertedDate]` – `legalAuthenticator.signatureDate` konverteras till `2023-06-01`

## Fältvalidering

Profilen [SEEHDSCondition](StructureDefinition-se-ehds-condition.html) kräver följande fält (kardinalitet 1..1 eller 1..*):

- `clinicalStatus` – alltid satt (active eller resolved baserat på slutdatum)
- `verificationStatus` – alltid satt till `confirmed`
- `category[diagnostyp]` – exakt ett diagnostyp-snitt med kod från kv_diagnostyp
- `code` – diagnoskod med minst en coding
- `subject.identifier` – patientidentifierare med system och value

Valfria fält (0..1) som sätts när källdata finns:

- `recorder` – sätts om `accountableHealthcareProfessional` finns i RIVTA-svaret
- `asserter` – sätts om `legalAuthenticator` finns i RIVTA-svaret
- `extension[assertedDate]` – sätts om `legalAuthenticator.signatureDate` finns
- `onsetDateTime` – sätts om `diagnosisTimePeriod.start` finns
- `abatementDateTime` – sätts om `diagnosisTimePeriod.end` finns

## Provenance

För varje Condition skapas en Provenance-resurs som inkluderas i sökbundlen med `Bundle.entry.search.mode = include`. Provenance-resursen bär den fullständiga provenanskedjan:

| Agent-roll | Källa | Syfte |
|---|---|---|
| `custodian` | `diagnosisHeader.careProviderHSAId` | Juridiskt ansvarig vårdgivare – används av Sparrtjänsten |
| `author` | `diagnosisHeader.careUnitHSAId` | Informationsägare vårdenhet |
| `assembler` | `EHDS_BRIDGE_HSA_ID` (env-variabel) | EHDS-bryggan som sammansatte FHIR-bundlen |

`Provenance.recorded` sätts till `diagnosisHeader.documentTime` (konverterad till ISO 8601 + UTC).
Om `documentTime` saknas används aktuell systemtid.

Provenance-resursen refererar Condition via `Provenance.target = urn:uuid:{Condition.id}`.

## PoC-begränsningar

### Spärr: inre och yttre
EHDS-bryggan är avsedd för cross-border och ska applicera alla spärrar. Sparrkontrollen sker mot `careProviderHSAId` (organisationsnivå) i enlighet med Ineras spärrtjänst som beskrivs på [Ineras konfluensida](https://inera.atlassian.net/wiki/spaces/PIS/pages/3435203724/).

**Utanför PoC-scope:** En vårdgivare som tillhör en spärrad enhet men ändå har rätt att ta del av informationen (t.ex. nödsituationer / break-the-glass) hanteras inte. Denna logik kräver kontextinformation om inloggad användares behörighet och är out of scope för PoC:en.
