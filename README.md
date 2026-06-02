# EHDS-brygga

FHIR R4-brygga som exponerar svenska journaldata via ett EURIDICE/IPS-alignat API. Bryggan
översätter RIVTA SOAP-tjänstekontrakt (NTjP) till FHIR-resurser i realtid, utan persistent
lagring av patientdata.

**Status:** proof-of-concept. Se [PoC-begränsningar](#poc-begränsningar-och-produktionsgap) för
vad som saknas innan produktionsdriftsättning.

---

## Innehåll

1. [Arkitekturella vägval](#arkitekturella-vägval)
2. [Snabbstart](#snabbstart)
3. [Systemöversikt](#systemöversikt)
4. [Konfiguration](#konfiguration)
5. [Mappningsmotor](#mappningsmotor)
6. [Lägga till ett nytt tjänstekontrakt](#lägga-till-ett-nytt-tjänstekontrakt)
7. [FHIR IG och profiler](#fhir-ig-och-profiler)
8. [PoC-begränsningar och produktionsgap](#poc-begränsningar-och-produktionsgap)
9. [Projektstruktur](#projektstruktur)

---

## Arkitekturella vägval

Dessa beslut är de viktigaste att förstå för att rätt tolka designen och justera en SAD.

### En VG per URL-segment — ingen aggregering

```
/fhir/{vg-hsa-id}/Condition?patient.identifier=http://electronichealth.se/identifier/personnummer|191212121212
```

Ett anrop till en VG-scopad URL resulterar i **exakt ett FHIR-anrop** till den VG:ns endpoint,
vilket i sin tur normalt resulterar i ett SOAP-anrop till NTjP. Bryggan aggregerar inte data
över VG-gränser. Det är den konsumerande applikationen (t.ex. EHDS-portal) som anropar flera
VG-URLer parallellt vid behov.

**Varför:** EURIDICE-specifikationen kräver att varje VG:s datakälla är tydligt identifierad i svaret.
Aggregering döljer ursprung och försvårar spårbarhet och spärrtillämpning.

### NTjP-proxy som separat deploybar enhet

All SOAP-logik, RIVTA-mappning och NTjP-kommunikation finns i en separat tjänst — **ntjp-proxy**.
Bryggan (`fhir-server`) kommunicerar med alla VG-endpoints via ett gemensamt FHIR-gränssnitt och
vet inte om bakomliggande system är en proxy, ett nativt FHIR-API eller något annat.

**Varför:** Proxyn kan driftsättas centralt (defaultläge) eller nära VG:ns datacenter (lägre latens,
lokal nätverkspolicies). Bryggan påverkas inte — bara `fhirEndpointUrl` i `vg-config.yaml` ändras.

### Post-query spärr på organisationsnivå

Spärrkontrollen körs **efter** att svar mappats till FHIR-resurser. Ineras spärrtjänst spärrar
på **organisationsnivå** — kontrollen sker mot `careProviderHSAId` (ansvarig vårdgivare) via
`ext-care-provider`-extensionen på varje Condition.

Varje svar innehåller en `Provenance`-resurs per Condition med tre agenter:
- `author` = ansvarig vårdgivare (`careProviderHSAId`) — Sparr-nyckeln
- `custodian` = vårdenhet (`careUnitHSAId`)
- `assembler` = bryggan (`EHDS_BRIDGE_HSA_ID`)

Vid fel mot Säkerhetstjänsten gäller *fail-open* — bryggan döljer aldrig data på grund av
infrastrukturfel.

### `fhirEndpointUrl` styr driftsättningsgränsen

Varje VG konfigureras med en `fhirEndpointUrl` i `vg-config.yaml`. Alla VG-endpoints behandlas
identiskt av bryggan oavsett om de pekar på:
- den centralt driftsatta ntjp-proxyn
- en proxyn deployad nära VG
- ett nativt FHIR-API hos VG

---

## Snabbstart

**Krav:** Docker, Docker Compose, Java 21, Maven 3.9+

```bash
# Klona och starta hela stacken med mock-tjänster
git clone https://github.com/oskthu2/EHDS-brygga
cd EHDS-brygga
cp .env.example .env
make up          # startar gateway + bridge + ntjp-proxy + 5 mock-containers

# Kör E2E-tester mot lokal stack (30 tester, PowerShell)
.\scripts\e2e-test.ps1
# Eller via Make (kör e2e-test.sh på Linux/Mac):
make e2e

# Enklare smoke-tester (HTTP-statuskodkontroller)
make test

# Bygg utan Docker
cd bridge && mvn package -DskipTests

# Bygg IG (kräver SUSHI och FHIR Validator — se scripts/download-tools.sh)
make ig
```

### Exempelanrop mot lokal stack

```bash
# CapabilityStatement
curl http://localhost:8080/fhir/metadata

# Diagnoser för testpatient 191212121212 via VGR
curl "http://localhost:8080/fhir/SE2321000016-4HK5/Condition?patient.identifier=http://electronichealth.se/identifier/personnummer|191212121212"

# Dokumentlista för samma patient
curl "http://localhost:8080/fhir/SE2321000016-4HK5/DocumentReference?patient.identifier=http://electronichealth.se/identifier/personnummer|191212121212"

# Oscopat anrop (frågar alla konfigurerade VG:er via EI)
curl "http://localhost:8080/fhir/Condition?patient.identifier=http://electronichealth.se/identifier/personnummer|191212121212"
```

Svaret innehåller både `Condition`-resurser (`searchMode=match`) och matchande
`Provenance`-resurser (`searchMode=include`) i samma Bundle.

---

## Systemöversikt

### Containers i produktion

```
Konsument
    │ HTTPS/FHIR R4
    ▼
┌──────────────────────────┐
│  gateway (nginx)         │  TLS-terminering, multi-tenant URL-routing,
│                          │  extraherar {vg-hsa-id} → X-VG-HSA-ID header
└────────────┬─────────────┘
             │ HTTP (intern)
┌────────────▼─────────────┐
│  fhir-server (Spring)    │  HAPI FHIR, providers, orkestrering
│                          │  EI → parallella FHIR-anrop → Sparr → Logg
└────────────┬─────────────┘
             │ HTTP/FHIR (intern)
┌────────────▼─────────────┐
│  ntjp-proxy (Spring)     │  All SOAP/RIVTA-logik
│  ┌──────────────────────┐│  • mapping-engine – semantisk översättning
│  │ mapping-engine       ││  • soap-client    – Apache CXF mot NTjP/VP
│  │ soap-client          ││
│  └──────────────────────┘│  Deploybar centralt eller nära VG.
└────────────┬─────────────┘  fhirEndpointUrl i vg-config.yaml styr pekning.
             │ SOAP/HTTPS (mTLS, RIVTA BP 2.1)
             ▼
      NTjP / VP → Regionens producent
```

I lokal utveckling: fem extra mock-containers (TAK, EI, Spärr, Logg, Backend-SOAP).

### Anropsflöde (VG-scopat)

```
Konsument       Gateway        fhir-server             ntjp-proxy          Inera / VG
    │               │               │                       │                   │
    │─GET /fhir/────►               │                       │                   │
    │  {vg-hsa-id}/ │               │                       │                   │
    │  Condition?   │               │                       │                   │
    │  patient=...  │               │                       │                   │
    │               │─X-VG-HSA-ID──►│                       │                   │
    │               │               │                       │                   │
    │          [JWT-validering — planerat, se §PoC]          │                   │
    │               │               │                       │                   │
    │               │               │─EI: finns data?───────────────────────────►
    │               │               │◄──ja/nej──────────────────────────────────│
    │               │               │                       │                   │
    │               │               │─GET /fhir/{vg}/───────►                   │
    │               │               │  Condition?patient=..  │                   │
    │               │               │                       │─TAK: fysisk URL?──►
    │               │               │                       │◄──URL─────────────│
    │               │               │                       │                   │
    │               │               │                       │─GetDiagnosis:2────►(NTjP/VP)
    │               │               │                       │                   │→ Producent
    │               │               │                       │◄──SOAP-svar───────│
    │               │               │                       │                   │
    │               │               │                       │  [SOAP→FHIR        │
    │               │               │                       │   mappning +       │
    │               │               │                       │   Provenance]      │
    │               │               │◄──Bundle (Condition   │                   │
    │               │               │   + Provenance)────────                   │
    │               │               │                       │                   │
    │               │         [Post-query Sparr:             │                   │
    │               │          kontroll mot careProviderHSAId│                   │
    │               │          via ext-care-provider]        │                   │
    │               │               │                       │                   │
    │               │         [Åtkomstlogg ATNA/BALP]        │                   │
    │               │               │                       │                   │
    │◄─200 OK Bundle─────────────────│                       │                   │
```

---

## Konfiguration

### `bridge/fhir-server/src/main/resources/config/vg-config.yaml`

Definierar en rad per VG som bryggan känner till.

```yaml
vgConfigs:
  - vgHsaId: SE2321000016-4HK5
    description: "VGR – Västra Götalandsregionen"
    # Pekar på ntjp-proxy i defaultläge.
    # Flytta proxyn nära VG: byt till https://proxy.vgr.se/fhir
    # VG med eget FHIR-API: byt till https://fhir.vgr.se/r4
    fhirEndpointUrl: http://ntjp-proxy:8091/fhir/SE2321000016-4HK5

  - vgHsaId: SE2321000098-7XYZ
    description: "SLL – Stockholms läns landsting"
    fhirEndpointUrl: http://ntjp-proxy:8091/fhir/SE2321000098-7XYZ
```

| Fält | Typ | Beskrivning |
|---|---|---|
| `vgHsaId` | String | VG:ns HSA-id. Används som tenant-diskriminator i URL. |
| `fhirEndpointUrl` | String | Bas-URL för VG:ns FHIR-endpoint. Normalt ntjp-proxy, kan vara extern proxy eller nativt FHIR. |

### `bridge/ntjp-proxy/src/main/resources/application.properties`

Konfigurerar proxyn mot NTjP och för autentisering.

```properties
ntjp.tak-url=http://mock-tak:4001
ntjp.bridge-hsa-id=SE2321000999-EHDS
server.port=8091
```

| Egenskap | Beskrivning |
|---|---|
| `ntjp.tak-url` | TAK-tjänstens URL för fysisk adressuppslag |
| `ntjp.bridge-hsa-id` | Bryggans HSA-id som skickas i `x-rivta-original-serviceconsumer-hsaid` och används som `assembler` i Provenance |

---

## Mappningsmotor

Mappningsmotorn finns i `ntjp-proxy` och är uppdelad i tre oberoende lager.

```
Fas: SOAP-svar → FHIR-resurser
  Lager 3 (TK-specifik Java-klass)
      └── Lager 1 (RIVTA JAXB-typer)
      └── Lager 2a (NamingSystem: OID → URI)
      └── Lager 2b (ConceptMap: kod → kod)
```

### Lager 1 — RIVTA JAXB-typer

**Plats:** `bridge/mapping-engine/src/main/java/se/inera/ehds/mapping/rivta/`

Handskrivna JAXB-annoterade Java-klasser som modellerar RIVTA XML-strukturer. En separat
Java-subpackage per XML-namespace (eftersom JAXB-namespace är paketglobalt via `package-info.java`).

| Package | XML-namespace | Kontrakt |
|---|---|---|
| `mapping/rivta/` | `urn:riv:clinicalprocess:activity:conditions:GetDiagnosisResponder:2` | GetDiagnosis |
| `mapping/rivta/doclist/` | `urn:riv:clinicalprocess:healthrecord:GetDocumentListResponder:1` | GetDocumentList |

Nyckeltyper (gemensamma för alla kontrakt):

| Java-klass | RIVTA-typ | Fält |
|---|---|---|
| `PersonIdType` | PersonId | `root` (OID), `extension` (identifierarvärde) |
| `CVType` | CV (Coded Value) | `code`, `codeSystem` (OID), `displayName` |
| `DatePeriodType` | DatePeriod | `start`, `end` (YYYYMMDD eller YYYYMMDDHHmmss) |
| `DiagnosisHeader` | DiagnosisHeader | `patientId`, `sourceSystemHSAId`, `documentTime`, `careUnitHSAId`, `careProviderHSAId` |
| `DiagnosisBody` | DiagnosisBody | `diagnosisCode`, `diagnosisType`, `diagnosisTimePeriod`, `chronicCondition`, `assertedDate` |

**Datumsformat:** RIVTA använder heltalssträngar. Mapparen konverterar:
- `YYYYMMDD` → `YYYY-MM-DD`
- `YYYYMMDDHHmmss` → `YYYY-MM-DDTHH:mm:ss`

**I produktion:** JAXB-klasserna bör genereras automatiskt från RIVTA:s WSDL/XSD via
`maven-jaxb2-plugin`. Handskrivna klasser är tillräckliga för PoC men riskerar
divergens från specifikationen vid framtida TK-versionsuppdateringar.

### Lager 2a — NamingSystem (OID ↔ FHIR URI)

**Konfigurationsfil:** `bridge/mapping-engine/src/main/resources/naming-systems.yaml`
**Java-klass:** `NamingSystemRegistry`

Mappningstabellen följer [HL7 Sweden basprofiler-r4](https://github.com/HL7Sweden/basprofiler-r4).

```yaml
entries:
  - oid: "1.2.752.129.2.1.3.1"
    uri: "http://electronichealth.se/identifier/personnummer"
    description: "Personnummer"
  - oid: "1.2.752.129.2.1.3.3"
    uri: "http://electronichealth.se/identifier/samordningsnummer"
    description: "Samordningsnummer"
  - oid: "1.2.752.116.1.1.1.1.3"
    uri: "https://www.icd10.se/"
    description: "ICD-10-SE"
```

**Fallback:** OID:er utan matchning returneras som `urn:oid:{oid}`.

`NamingSystemRegistry` stödjer även **omvänd sökning** via `uriToOid(uri)` — konverterar
inkommande FHIR-URI tillbaka till rå OID för RIVTA/SOAP-anrop.

**Implementerade OID-mappningar:**

| OID | FHIR URI | Beskrivning |
|---|---|---|
| `1.2.752.129.2.1.3.1` | `http://electronichealth.se/identifier/personnummer` | Personnummer (HL7 SE basprofil) |
| `1.2.752.129.2.1.3.3` | `http://electronichealth.se/identifier/samordningsnummer` | Samordningsnummer (HL7 SE basprofil) |
| `1.2.752.129.2.1.4.1` | `urn:oid:1.2.752.129.2.1.4.1` | HSA-id (Inera NTjP) |
| `1.2.752.29.4.19` | `urn:oid:1.2.752.29.4.19` | HSA-id (HL7 SE basprofil) |
| `1.2.752.116.1.1.1.1.3` | `https://www.icd10.se/` | ICD-10-SE |
| `2.16.840.1.113883.6.3` | `http://hl7.org/fhir/sid/icd-10` | ICD-10 (internationell) |
| `2.16.840.1.113883.6.96` | `http://snomed.info/sct` | SNOMED CT |
| `1.2.752.116.1.1.1.1.1` | `https://www.socialstyrelsen.se/statistik-och-data/klassifikationer-och-koder/kva/` | KVÅ |
| `1.2.752.116.3.1.1` | `urn:oid:1.2.752.116.3.1.1` | Legitimationsnummer |
| `1.2.752.116.3.1.2` | `urn:oid:1.2.752.116.3.1.2` | Förskrivarkod |
| `1.2.752.116.3.1.3` | `urn:oid:1.2.752.116.3.1.3` | HOSP-yrken |
| `1.2.752.116.1.3.6` | `urn:oid:1.2.752.116.1.3.6` | SOSNYK yrkeskategorier |

### Lager 2b — ConceptMap (kod → kod)

**Konfigurationsfiler:** `bridge/mapping-engine/src/main/resources/concept-maps/*.yaml`
**Java-klass:** `ConceptMapRegistry`

En YAML-fil per konceptuell mappning. Varje fil har ett `name` och en lista av `entries`.

```yaml
# concept-maps/diagnosis-type.yaml
name: "DiagnosisType"
entries:
  - sourceCode: "HD"
    targetSystem: "http://terminology.hl7.org/CodeSystem/condition-category"
    targetCode: "encounter-diagnosis"
    display: "Encounter Diagnosis"
  - sourceCode: "BY"
    targetSystem: "https://ehds-brygga.inera.se/fhir/CodeSystem/DiagnosisType"
    targetCode: "bi-diagnos"
    display: "Bidiagnos"
```

**Hantering av okartlagda koder:** Om källkoden inte finns i tabellen används källkoden och
källsystemets OID direkt — konsumenten ser alltid originalkoden, aldrig ett tyst bortfall.

### Lager 3 — TK-specifik mappningsklass

**Plats:** `bridge/mapping-engine/src/main/java/se/inera/ehds/mapping/tk/`

En konkret klass per tjänstekontrakt:

| Klass | Intyp | Uttyp | TK |
|---|---|---|---|
| `GetDiagnosisMapper` | `GetDiagnosisResponse` | `List<MappedDiagnosisEntry>` | GetDiagnosis:2 |
| `GetDocumentListMapper` | `GetDocumentListResponse` | `List<DocumentReference>` | GetDocumentList:1 |

`MapperContext` skickas med vid varje mappning och innehåller:
- `patientSystem` — FHIR-URI för patientidentifierarsystemet
- `patientValue` — patientens identifierarvärde
- `bridgeHsaId` — bryggans HSA-id (sätts som `assembler`-agent i Provenance)

**`GetDiagnosisMapper` — producerar `MappedDiagnosisEntry(Condition, Provenance)` per diagnos:**

| RIVTA-element | FHIR-element | Logik |
|---|---|---|
| `diagnosisHeader.patientId.root/extension` | `subject.identifier.system/value` | OID→URI via lager 2a |
| `diagnosisHeader.sourceSystemHSAId` | `recorder.identifier` + `ext-source-system` | HSA-id på båda ställena |
| `diagnosisHeader.careProviderHSAId` | `ext-care-provider` + `Provenance.agent[author]` | Ansvarig vårdgivare — Sparr-nyckel |
| `diagnosisHeader.careUnitHSAId` | `ext-care-unit` + `Provenance.agent[custodian]` | Vårdenhet |
| `diagnosisHeader.documentTime` | `recordedDate` + `Provenance.recorded` | Datumkonvertering |
| `diagnosisBody.diagnosisCode` | `code.coding` | OID→URI via lager 2a |
| `diagnosisBody.diagnosisType` | `category` | Kod→kod via lager 2b (DiagnosisType-karta) |
| `diagnosisBody.diagnosisTimePeriod.start` | `onsetDateTime` | Datumkonvertering |
| `diagnosisBody.diagnosisTimePeriod.end` | `abatementDateTime` | Om satt → `clinicalStatus=resolved`, annars `active` |
| `diagnosisBody.assertedDate` *(EPS)* | `ext-asserted-date` | Administrativt datum, skiljer sig från `recordedDate` |
| *(härledd)* | `clinicalStatus` | `active` om inget slutdatum, `resolved` om slutdatum finns |
| *(konstant)* | `verificationStatus` | Alltid `confirmed` |
| `bridgeHsaId` från `MapperContext` | `Provenance.agent[assembler]` | Bryggan som sammansättande aktör |

**Sparr-integrationen:** `ext-care-provider`-extensionen på varje Condition bär
`careProviderHSAId`. `SparrFilterService` i `fhir-server` läser denna extension för att
utföra spärrkontrollen på rätt organisationsnivå. Utan extensionen passerar resursen
alltid (fail-open).

---

## Lägga till ett nytt tjänstekontrakt

Fyra steg — ingen ändring i gateway, fhir-server eller befintliga orchestrare behövs
(förutsatt att det nya TK:et returnerar resurser av samma FHIR-typ som ett befintligt).

### Steg 1: Lägg till JAXB-typer (lager 1)

Skapa ett nytt subpackage om TK:et har ett annat XML-namespace:

```
bridge/mapping-engine/src/main/java/se/inera/ehds/mapping/rivta/medication/
  package-info.java          ← @XmlSchema(namespace="urn:riv:...", elementFormDefault=QUALIFIED)
  GetMedicationHistory.java
  GetMedicationHistoryResponse.java
  MedicationHistoryType.java
```

### Steg 2: Implementera mappningsklass (lager 3)

```java
// bridge/mapping-engine/src/main/java/se/inera/ehds/mapping/tk/getmedication/GetMedicationMapper.java
public class GetMedicationMapper
        implements TkMapper<GetMedicationHistoryResponse, MedicationStatement> {

    @Override
    public List<MedicationStatement> map(GetMedicationHistoryResponse response, MapperContext ctx) {
        // 1. Nollkontroll och resultatkodkontroll
        // 2. Iterera svar-element
        // 3. Sätt ext-care-provider (careProviderHSAId) — krävs för Sparr-kontroll
        // 4. Sätt ext-source-system (sourceSystemHSAId) — spårbarhet
        // 5. Använd namingSystem.oidToUri() för alla OID-värden
        // 6. Använd conceptMaps.translate...() för kodmappningar
    }
}
```

### Steg 3: Lägg till SOAP SEI och klient

```java
// soap-client/src/main/java/se/inera/ehds/soap/sei/GetMedicationResponderInterface.java
@WebService(targetNamespace = "urn:riv:clinicalprocess:actoutcome:medication:GetMedicationHistoryResponder:2")
public interface GetMedicationResponderInterface {
    GetMedicationHistoryResponse getMedicationHistory(
            @WebParam(name = "LogicalAddress", ...) String logicalAddress,
            @WebParam(name = "GetMedicationHistoryRequest", ...) GetMedicationHistory request);
}
```

Kopiera mönstret från `GetDiagnosisClient`. De enda delarna som varierar är SEI-interface,
request-klass och SOAP-action-sträng.

### Steg 4: Registrera i ntjp-proxy

I `bridge/ntjp-proxy/src/main/java/se/inera/ehds/proxy/config/ProxyBeanConfig.java`:

```java
@Bean
public GetMedicationMapper getMedicationMapper(NamingSystemRegistry naming, ConceptMapRegistry concepts) {
    return new GetMedicationMapper(naming, concepts);
}

@Bean
public GetMedicationClient getMedicationClient(ProxyProperties props) {
    return new GetMedicationClient(props.getBridgeHsaId());
}
```

Lägg sedan till en ny `@RestController` för `MedicationStatement` i `ntjp-proxy/fhir/`
och en ny `ResourceProvider` + `Orchestrator` i `fhir-server` om det är en ny FHIR-resurstyp.

---

## FHIR IG och profiler

Implementation Guide finns under `ig/`. Byggs med SUSHI (FSH-kompilator).

| Profil | FHIR-resurs | Förälderprofil | Kontrakt |
|---|---|---|---|
| `SEEHDSCondition` | Condition | IPS `Condition-uv-ips` | GetDiagnosis:2 |
| `SEEHDSDocumentReference` | DocumentReference | FHIR R4 base | GetDocumentList:1 |

### Extensions på Condition

| Extension | URL | Källfält | Användning |
|---|---|---|---|
| `ext-source-system` | `...ext-source-system` | `sourceSystemHSAId` | Källsystemets HSA-id; spårbarhet |
| `ext-care-provider` | `...ext-care-provider` | `careProviderHSAId` | Ansvarig vårdgivare; **Sparr-nyckel** |
| `ext-care-unit` | `...ext-care-unit` | `careUnitHSAId` | Vårdenhet |
| `ext-asserted-date` | `...ext-asserted-date` | EPS `assertedDate` | Administrativt diagnosdatum |

Alla URL:er har prefix `https://ehds-brygga.inera.se/fhir/StructureDefinition/`.

Exempel på `ext-care-provider`:

```json
{
  "url": "https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-care-provider",
  "valueIdentifier": {
    "system": "urn:oid:1.2.752.129.2.1.4.1",
    "value": "SE2321000016-4HK5"
  }
}
```

### Provenance per Condition

Varje Condition-resurs åtföljs av en `Provenance` (inkluderad som `searchMode=include` i Bundle):

```json
{
  "resourceType": "Provenance",
  "target": [{ "reference": "urn:uuid:{condition-id}" }],
  "recorded": "2023-06-01T12:00:00Z",
  "agent": [
    {
      "type": { "coding": [{ "system": "http://terminology.hl7.org/CodeSystem/provenance-participant-type", "code": "author" }]},
      "who": { "identifier": { "system": "urn:oid:1.2.752.129.2.1.4.1", "value": "SE2321000016-4HK5" }}
    },
    {
      "type": { "coding": [{ "system": "...", "code": "custodian" }]},
      "who": { "identifier": { "system": "urn:oid:1.2.752.129.2.1.4.1", "value": "SE2321000016-E000000001" }}
    },
    {
      "type": { "coding": [{ "system": "...", "code": "assembler" }]},
      "who": { "identifier": { "system": "urn:oid:1.2.752.129.2.1.4.1", "value": "SE2321000999-EHDS" }}
    }
  ]
}
```

---

## PoC-begränsningar och produktionsgap

Dessa delar är medvetet ej implementerade i PoC:n och måste adresseras inför produktion.

| Gap | Beskrivning | Plats i kod |
|---|---|---|
| **JWT-validering** | Åtkomstintyg valideras ej. Gateway skickar inga JWT-claims till bryggan. | `gateway/nginx.conf`, ny Spring Security-config |
| **mTLS mot NTjP** | CXF-klienten har plats-hållarkommentar för SITHS-keystore. Inga certifikat konfigurerade. | `GetDiagnosisClient.java` rad 39–41 |
| **SAML WS-Security** | RIVTA BP 2.1 kräver SAML-assertion i SOAP-headern. Ej implementerat. | `GetDiagnosisClient.java` rad 39 |
| **JAXB från WSDL** | Lager 1-klasser är handskrivna. Bör genereras från officiella RIVTA WSDL/XSD. | `mapping-engine/.../mapping/rivta/` |
| **`COMPOSITION_ASSEMBLY`** | Utdataläget är reserverat men ej implementerat i ntjp-proxy. | `ntjp-proxy` saknar Composition-mapper |
| **CapabilityStatement per VG** | HAPI genererar ett globalt CS. Varje VG bör deklarera sina egna resurser. | Kräver `IServerConformanceProvider`-implementation |
| **PDL-loggformat** | `LoggService` loggar till mock. Formatet är inte validerat mot ATNA/BALP-specifikationen. | `LoggService.java` |
| **EI-kontraktsversion** | EI-mock använder förenklat HTTP-API. Ska använda RIVTA `GetEngagements:1`. | `EiService.java` |
| **Lokal tidzon** | `parseRivDate()` returnerar datum utan tidszon. Kräver explicit hantering av `Europe/Stockholm` → UTC. | `GetDiagnosisMapper.java`, `GetDocumentListMapper.java` |
| **Sparr: break-the-glass** | En konsument från en spärrad enhet som ändå har rätt till informationen (nödsituation) hanteras inte. Kräver kontextinfo om inloggad användares behörighet. | `SparrFilterService.java` |
| **Sparr: DocumentReference** | `SparrFilterService` filtrerar Condition via `ext-care-provider`. Motsvarande för DocumentReference saknas. | `DocumentQueryOrchestrator.java` |
| **Provenance: DocumentReference** | `GetDocumentListMapper` producerar inte Provenance. Samma mönster som GetDiagnosisMapper bör tillämpas. | `GetDocumentListMapper.java` |

---

## Projektstruktur

```
EHDS-brygga/
├── bridge/                        # Java-applikation (Maven multi-modul)
│   ├── fhir-server/               # Spring Boot + HAPI FHIR — providers, orkestrering, Sparr, Logg
│   │   └── src/main/resources/
│   │       └── config/vg-config.yaml  ← Per-VG-konfiguration (fhirEndpointUrl)
│   ├── ntjp-proxy/                # Spring Boot — SOAP/RIVTA-logik, exponerar FHIR-API
│   │   └── src/main/resources/
│   │       └── application.properties ← ntjp.tak-url, ntjp.bridge-hsa-id
│   ├── mapping-engine/            # Mappningsbibliotek — lager 1+2a+2b+3
│   │   └── src/main/resources/
│   │       ├── naming-systems.yaml         ← Lager 2a: OID↔URI-tabell
│   │       └── concept-maps/               ← Lager 2b: kod→kod-tabeller
│   └── soap-client/               # Apache CXF-klient mot NTjP
│
├── gateway/                       # nginx — TLS, multi-tenant URL-routing
│   ├── nginx.conf
│   └── well-known/                # SMART/OpenID-konfiguration (statisk JSON)
│
├── mocks/                         # Fem mock-containers (Node.js/Express)
│   ├── tak/                       # Tjänsteadresseringskatalog — routes + fysiska adresser
│   ├── ei/                        # Engagemangsindex — patientengagemang per TK
│   ├── sparr/                     # Säkerhetstjänsten — kontroll mot careProviderHSAId
│   ├── logg/                      # Loggtjänsten — tar emot loggposter, loggar till stdout
│   └── backend/                   # RIVTA SOAP-producent — testdata i responses.json
│
├── ig/                            # FHIR Implementation Guide (SUSHI/FSH)
│   └── input/
│       ├── fsh/profiles/          # SEEHDSCondition, SEEHDSDocumentReference
│       ├── fsh/extensions/        # ext-source-system, ext-care-provider, ext-care-unit, ext-asserted-date
│       ├── fsh/conceptmaps/       # DiagnosisTypeToCategoryMap
│       └── pagecontent/           # architecture.md, mapping-*.md
│
├── scripts/
│   ├── test.sh                    # Smoke-tester (HTTP-statuskoder) mot live stack
│   ├── e2e-test.sh                # E2E-tester, 30 scenarion (bash, Linux/Mac)
│   ├── e2e-test.ps1               # E2E-tester, 30 scenarion (PowerShell, Windows)
│   ├── validate.sh                # Maven-build + FHIR Validator
│   └── build-ig.sh                # Bygger IG med SUSHI + FHIR Publisher
│
├── docker-compose.yml             # Lokal stack: gateway + fhir-server + ntjp-proxy + 5 mocks
├── Makefile                       # make up/down/build/test/validate/ig/logs
└── .env.example                   # Miljövariabler för lokal körning
```

### Nyckelklasser att känna till

| Klass | Modul | Roll |
|---|---|---|
| `TenantInterceptor` | `fhir-server` | Extraherar `X-VG-HSA-ID`-header → `RequestDetails.setAttribute("vgHsaId")` |
| `QueryOrchestrator` | `fhir-server` | EI → parallella FHIR-anrop → Sparr → Logg för Condition |
| `DocumentQueryOrchestrator` | `fhir-server` | Samma pipeline för DocumentReference |
| `FhirProxyClient` | `fhir-server` | HAPI FHIR-klient mot VG-endpoint; parsar Condition+Provenance ur Bundle |
| `SparrFilterService` | `fhir-server` | Post-query Sparr på org-nivå — läser `ext-care-provider` (careProviderHSAId) |
| `ConditionProxyController` | `ntjp-proxy` | TAK → SOAP → Mapping → Bundle (Condition+Provenance) |
| `NamingSystemRegistry` | `mapping-engine` | Lager 2a: OID↔URI, `oidToUri()` och `uriToOid()` |
| `ConceptMapRegistry` | `mapping-engine` | Lager 2b: kod→kod, läses från `concept-maps/*.yaml` |
| `GetDiagnosisMapper` | `mapping-engine` | Lager 3: `GetDiagnosisResponse` → `List<MappedDiagnosisEntry>` |
| `GetDocumentListMapper` | `mapping-engine` | Lager 3: `GetDocumentListResponse` → `List<DocumentReference>` |
| `MappedDiagnosisEntry` | `mapping-engine` | Record: `(Condition condition, Provenance provenance)` |
