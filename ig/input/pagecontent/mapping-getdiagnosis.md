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
| `diagnosisHeader.patientId.extension` | `Condition.subject.identifier.value` | Personnummer eller samordningsnummer |
| `diagnosisHeader.patientId.root` | `Condition.subject.identifier.system` | OID konverteras till URN, se tabell nedan |
| `diagnosisHeader.sourceSystemHSAId` | `Condition.recorder.identifier.value` | HSA-id för källsystem |
| `diagnosisHeader.sourceSystemHSAId` | `Condition.extension[sourceSystem]` | Källsystemets HSA-id som extension |
| `diagnosisHeader.documentTime` | `Condition.recordedDate` | Format YYYYMMDDHHMMSS → ISO 8601 |
| `diagnosisBody.diagnosisCode.code` | `Condition.code.coding.code` | ICD-10-SE kod, t.ex. `J18.9` |
| `diagnosisBody.diagnosisCode.codeSystem` | `Condition.code.coding.system` | OID `1.2.752.116.1.1.1.1.3` → `https://www.icd10.se/` |
| `diagnosisBody.diagnosisCode.displayName` | `Condition.code.coding.display` | Diagnosbenämning på svenska |
| `diagnosisBody.diagnosisType` (HD) | `Condition.category` = `encounter-diagnosis` | Huvuddiagnos → FHIR standard-kod |
| `diagnosisBody.diagnosisType` (BY) | `Condition.category` = `bi-diagnos` | Bidiagnos → svensk tilläggskod |
| `diagnosisBody.diagnosisTimePeriod.start` | `Condition.onsetDateTime` | Format YYYYMMDD → YYYY-MM-DD |
| `diagnosisBody.diagnosisTimePeriod.end` | `Condition.abatementDateTime` | Om satt: resolved, annars active |

## OID till URI-mappningar

RIVTA använder OID-identifierare (Object Identifiers) för kodsystem och personidentifierare.
FHIR föredrar URI:er. EHDS-bryggan utför följande konverteringar:

| OID | URI | Beskrivning |
|---|---|---|
| `1.2.752.129.2.1.3.1` | `urn:oid:1.2.752.129.2.1.3.1` | Personnummer |
| `1.2.752.129.2.1.3.3` | `urn:oid:1.2.752.129.2.1.3.3` | Samordningsnummer |
| `1.2.752.129.2.1.4.1` | `urn:oid:1.2.752.129.2.1.4.1` | HSA-id |
| `1.2.752.116.1.1.1.1.3` | `https://www.icd10.se/` | ICD-10-SE |

OID:er som inte har en känd URI-mappning bevaras som `urn:oid:{oid}`.

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

| RIVTA diagnosisType | Kod | System | FHIR category-kod |
|---|---|---|---|
| `HD` – Huvuddiagnos | `encounter-diagnosis` | `http://terminology.hl7.org/CodeSystem/condition-category` | Standard FHIR-kod |
| `BY` – Bidiagnos | `bi-diagnos` | `https://ehds-brygga.inera.se/fhir/CodeSystem/DiagnosisType` | Svensk tilläggskod |

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
    "profile": [
      "https://ehds-brygga.inera.se/fhir/StructureDefinition/se-ehds-condition"
    ]
  },
  "extension": [
    {
      "url": "https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-source-system",
      "valueIdentifier": {
        "system": "urn:oid:1.2.752.129.2.1.4.1",
        "value": "SE2321000016-4HK5"
      }
    }
  ],
  "clinicalStatus": {
    "coding": [
      {
        "system": "http://terminology.hl7.org/CodeSystem/condition-clinical",
        "code": "active",
        "display": "Active"
      }
    ]
  },
  "verificationStatus": {
    "coding": [
      {
        "system": "http://terminology.hl7.org/CodeSystem/condition-ver-status",
        "code": "confirmed",
        "display": "Confirmed"
      }
    ]
  },
  "category": [
    {
      "coding": [
        {
          "system": "http://terminology.hl7.org/CodeSystem/condition-category",
          "code": "encounter-diagnosis",
          "display": "Encounter Diagnosis"
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
      "system": "urn:oid:1.2.752.129.2.1.3.1",
      "value": "191212121212"
    }
  },
  "onsetDateTime": "2023-06-01",
  "recordedDate": "2023-06-01T12:00:00",
  "recorder": {
    "identifier": {
      "system": "urn:oid:1.2.752.129.2.1.4.1",
      "value": "SE2321000016-4HK5"
    }
  }
}
```

### Förklaring av mappningen i exemplet

- `clinicalStatus = active` – inget slutdatum i `diagnosisTimePeriod`, så diagnosen är fortfarande aktiv
- `category = encounter-diagnosis` – `diagnosisType = HD` (Huvuddiagnos) mappas till standard-FHIR-koden
- `code.coding.system = https://www.icd10.se/` – OID `1.2.752.116.1.1.1.1.3` konverteras till ICD-10-SE URI
- `subject.identifier.system = urn:oid:1.2.752.129.2.1.3.1` – OID för personnummer bevaras som URN
- `recordedDate` – `20230601120000` konverteras till `2023-06-01T12:00:00`
- `onsetDateTime` – `20230601` konverteras till `2023-06-01`
- `extension[sourceSystem]` och `recorder` – båda pekar på `SE2321000016-4HK5` (källsystemets HSA-id)

## Fältvalidering

Profilen [SEEHDSCondition](StructureDefinition-se-ehds-condition.html) kräver följande fält (kardinalitet 1..1 eller 1..*):

- `clinicalStatus` – alltid satt
- `verificationStatus` – alltid satt till `confirmed`
- `category` – minst en diagnostyp
- `code` – diagnoskod med minst en coding
- `subject.identifier` – patientidentifierare med system och value

Bryggan avvisar RIVTA-svar som saknar obligatoriska fält och loggar valideringsfel.
