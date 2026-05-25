# Arkitektur

## Översikt

EHDS-bryggan är ett API-gateway-lager som exponerar svenska nationella hälsodata via FHIR R4,
anpassat för EHDS-kraven (European Health Data Space). Bryggan kommunicerar med Ineras nationella
tjänsteplattform och dess bakomliggande journalsystem via etablerade RIVTA SOAP-tjänster.

Systemet är utformat för att vara:

- **Konfigurationsdrivet** – nya tjänstekontrakt läggs till via konfiguration, inte kodändringar
- **Stateless** – bryggan cachelagrar ingen patientdata; alla anrop är genomströmning i realtid
- **Spårbart** – alla anrop loggas till Ineras nationella loggtjänst för åtkomstloggning (PDL)
- **Säkert** – åtkomstkontroll via Ineras spärrtjänst och autentisering via SITHS/eID

## Systemkomponenter

### Gateway (NGINX)

Ingångspunkt för alla externa FHIR-anrop. Hanterar:

- TLS-terminering
- Autentisering av inkommande FHIR-klienter (OAuth 2.0 / SMART on FHIR)
- Routing till Bridge
- Hastighetsbegränsning och skydd mot överbelastning

### Bridge (EHDS-bryggan)

Kärnkomponenten. Ansvarar för:

- Mottagning och validering av FHIR R4-förfrågningar
- Sökning i TAK för tillgängliga källsystem
- Anrop till relevanta källsystem via Ineras nationella tjänsteplattform (NTjP)
- Konvertering av RIVTA-svar till FHIR-resurser via mappningslogik
- Aggregering av svar från flera källsystem
- Kontroll mot spärrtjänsten (patient har ej spärrat uppgifterna)
- Loggning till loggtjänsten (PDL-logg)

### TAK (Tjänsteadresseringskatalog)

Ineras register över vilka producenter (journalsystem) som producerar vilka tjänstekontrakt
för vilka patienter/organisationer. Bryggan frågar TAK för att identifiera relevanta källsystem
innan SOAP-anrop görs.

### EI (Engagemangsindex)

Ineras index över vilka patienter som har data registrerat i vilka system och vid vilka
vårdenheter. Används för att effektivt hitta relevanta källsystem för en given patient
utan att fråga alla möjliga producenter.

### Spärrtjänst

Ineras nationella spärrtjänst för åtkomstkontroll. Bryggan kontrollerar mot spärrtjänsten
att en patient inte har spärrat sina uppgifter för den begärande aktören.
Spärrade uppgifter returneras inte.

### Loggtjänst

Ineras nationella loggtjänst för åtkomstloggning enligt Patientdatalagen (PDL).
Alla åtkomster till patientdata loggas med information om:

- Vem som begärde data (användare/system)
- Vilken patient som berördes
- Vilka uppgifter som lämnades ut
- Tidpunkt för åtkomsten
- Ändamål med åtkomsten

## Flödesbeskrivning

### Normalt flöde: FHIR GET → SOAP → FHIR-svar

```
FHIR-klient                  Gateway (NGINX)       Bridge                  NTjP / Källor
    |                              |                  |                         |
    |--- GET /fhir/Condition ------>|                  |                         |
    |                              |--- Auth verify -->|                         |
    |                              |                  |                         |
    |                              |--- Route req ---->|                         |
    |                              |                  |                         |
    |                              |                  |--- Query EI ----------->|
    |                              |                  |<-- Källsystem-lista ----|
    |                              |                  |                         |
    |                              |                  |--- Check spärr -------->|
    |                              |                  |<-- Spärrstatus ---------|
    |                              |                  |                         |
    |                              |                  |--- GetDiagnosis:2 ----->| (per källsystem)
    |                              |                  |<-- DiagnosisResponse ---|
    |                              |                  |                         |
    |                              |                  |--- Log access --------->|
    |                              |                  |<-- Log ack -------------|
    |                              |                  |                         |
    |                              |                  | [Mappa RIVTA → FHIR]    |
    |                              |                  | [Aggregera Condition[]] |
    |                              |                  |                         |
    |                              |<-- FHIR Bundle --|                         |
    |<-- 200 OK FHIR Bundle --------|                  |                         |
```

### Felhantering

Om ett källsystem inte svarar (timeout) eller returnerar fel:

- Bryggan loggar felet och fortsätter med övriga källsystem
- Partiella svar returneras med en `OperationOutcome` som informerar om eventuella fel
- Klienten kan identifiera om svaret är komplett via `Bundle.meta.tag`

## ASCII-diagram: Systemöversikt

```
┌─────────────────────────────────────────────────────────────────────┐
│                          EHDS-nätverket / Internet                  │
│                                                                     │
│    ┌──────────────┐                                                  │
│    │  FHIR-klient │  (EHR-system, EHDS-portal, forskningstjänst)    │
│    └──────┬───────┘                                                  │
│           │ HTTPS / FHIR R4                                         │
└───────────┼─────────────────────────────────────────────────────────┘
            │
┌───────────┼─────────────────────────────────────────────────────────┐
│  EHDS-bryggan (DMZ)                                                 │
│           │                                                         │
│    ┌──────▼───────────────────┐                                     │
│    │    Gateway (NGINX)       │  TLS-terminering, OAuth 2.0/SMART   │
│    └──────┬───────────────────┘                                     │
│           │ Intern HTTP                                             │
│    ┌──────▼───────────────────┐                                     │
│    │    Bridge                │  FHIR→SOAP, SOAP→FHIR mappning      │
│    │    (Spring Boot / Go)    │                                     │
│    └──────┬───────────────────┘                                     │
│           │                                                         │
└───────────┼─────────────────────────────────────────────────────────┘
            │ HTTPS / SOAP (RIVTA)
┌───────────┼─────────────────────────────────────────────────────────┐
│  Inera Nationell Infrastruktur                                      │
│           │                                                         │
│    ┌──────▼──────────────────────────────────────────────────┐      │
│    │           Nationell Tjänsteplattform (NTjP)             │      │
│    └──┬─────────────┬──────────────┬──────────────┬──────────┘      │
│       │             │              │              │                 │
│  ┌────▼────┐  ┌─────▼────┐  ┌─────▼────┐  ┌─────▼────┐           │
│  │   TAK   │  │    EI    │  │ Spärr-   │  │ Logg-    │           │
│  │         │  │(Engage-  │  │ tjänst   │  │ tjänst   │           │
│  │ Tjänste-│  │ mangs-   │  │          │  │  (PDL)   │           │
│  │ adress- │  │ index)   │  │          │  │          │           │
│  │ katalog │  │          │  │          │  │          │           │
│  └─────────┘  └──────────┘  └──────────┘  └──────────┘           │
│                                                                     │
│    ┌────────────────────────────────────────────────────────┐      │
│    │           Källsystem (Journalsystem)                   │      │
│    │                                                        │      │
│    │  ┌─────────────┐  ┌─────────────┐  ┌──────────────┐  │      │
│    │  │  System A   │  │  System B   │  │  System C    │  │      │
│    │  │ (Region XY) │  │ (Region AB) │  │ (Privat vård)│  │      │
│    │  └─────────────┘  └─────────────┘  └──────────────┘  │      │
│    └────────────────────────────────────────────────────────┘      │
└─────────────────────────────────────────────────────────────────────┘
```

## Säkerhet och dataskydd

### Autentisering och auktorisation

- Inkommande FHIR-klienter autentiseras via OAuth 2.0 med SMART on FHIR-scopes
- Anrop till NTjP autentiseras med SITHS-certifikat (ömsesidig TLS)
- Bryggan agerar som teknisk aktör gentemot NTjP

### Åtkomstkontroll

- Spärrtjänsten kontrolleras vid varje patientdatafråga
- Patienter kan spärra sina uppgifter från specifika aktörer
- Spärrade uppgifter returneras aldrig, även om källsystem svarar

### Loggning (PDL)

Alla åtkomster till patientdata loggas i enlighet med Patientdatalagen:

- Loggposter innehåller patientidentifierare (krypterat), aktör, tidpunkt och ändamål
- Patienter har rätt att begära utdrag ur loggen via sin vårdenhet
- Loggar lagras hos Ineras loggtjänst

### Dataminimering

- Inga patientdata lagras i bryggan eller gateway
- Alla anrop är genomströmning i realtid
- Cache används inte för patientdata

## Lägga till nya tjänstekontrakt

EHDS-bryggan är utformad för att vara konfigurationsdrivet. Att lägga till stöd för ett nytt
RIVTA-tjänstekontrakt kräver:

1. **Mappningskonfiguration** – Definiera hur RIVTA-element mappas till FHIR-element i en YAML-fil
2. **FHIR-profil** – Skapa en ny FSH-profil i denna Implementation Guide
3. **ConceptMaps** – Definiera kodsystemmappningar för eventuella kodsystem
4. **Sidor i IG** – Dokumentera mappningen i en ny mappningssida

Ingen kodändring i bryggan behövs för enkla mappningar. Komplexa transformationer
(t.ex. aggregering av data från flera tjänstekontrakt) kan kräva en plugin-implementation.

### Mappningskonfigurationsexempel (YAML)

```yaml
serviceContract:
  namespace: clinicalprocess:activity:conditions
  name: GetDiagnosis
  version: 2

fhirResource: Condition
profile: https://ehds-brygga.inera.se/fhir/StructureDefinition/se-ehds-condition

mappings:
  - rivta: diagnosisHeader.patientId.extension
    fhir: subject.identifier.value
  - rivta: diagnosisHeader.patientId.root
    fhir: subject.identifier.system
    transform: oidToUrn
  - rivta: diagnosisHeader.documentTime
    fhir: recordedDate
    transform: rivaDateTimeToIso8601
  - rivta: diagnosisBody.diagnosisCode.code
    fhir: code.coding[system=https://www.icd10.se/].code
  - rivta: diagnosisBody.diagnosisType
    fhir: category.coding.code
    conceptMap: DiagnosisTypeToCategoryMap
```

## Driftsättning

Bryggan driftsätts som containerbaserade tjänster (Docker/Kubernetes):

- `gateway` – NGINX-container med konfiguration och TLS-certifikat
- `bridge` – Applikationscontainer med mappningslogik
- `mocks` – Lokala SOAP-mocks för utveckling och test

Se `docker-compose.yml` i projektets rot för lokal utvecklingskonfiguration.
