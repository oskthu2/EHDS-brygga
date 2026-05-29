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
/fhir/{vg-hsa-id}/Condition?patient.identifier=urn:oid:1.2.752.129.2.1.3.1|191212121212
```

Ett anrop till en VG-scopad URL resulterar i **exakt ett SOAP-anrop** till exakt en logisk adress
(VG:ns HSA-id). Bryggan aggregerar inte data över VG-gränser. Det är den konsumerande applikationen
(t.ex. EHDS-portal) som anropar flera VG-URLer parallellt vid behov.

**Varför:** EURIDICE-specifikationen kräver att varje VG:s datakälla är tydligt identifierad i svaret.
Aggregering döljer ursprung och försvårar spårbarhet och spärrtillämpning.

### Post-query spärr — inte pre-query

Spärrkontrollen körs **efter** att SOAP-svaret mappats till FHIR-resurser. Anropet till
Säkerhetstjänsten sker synkront per unika källsystem (`sourceSystemHSAId`) i svaret.
En producent kan ha data från flera underenheter/VG:er — spärrfiltret tar bort resurser
vars `ext-source-system` patienten spärrat.

**Varför:** RIVTA-producenter svarar på VG-nivå men svaret kan innehålla data från
underenheter som individen spärrat. Filtreringen måste ske på resursnivå, inte på
anropsnivå.

### Källstrategi per VG — SOAP eller FHIR-passtrthrough

Varje VG konfigureras med antingen `SOAP` (anrop via NTjP) eller `FHIR_PASSTHROUGH`
(proxy till VG:ns egna FHIR-server). Vägvalet styrs i `vg-config.yaml`.

### Utdataläge per informationsmängd — resursorienterat eller dokumentutbyte

Varje tjänstekontrakt konfigureras med `outputMode` i `services.yaml`:

| `outputMode` | Semantik | MVP-exempel |
|---|---|---|
| `RESOURCE_PER_ELEMENT` | Varje SOAP-element → en FHIR-resurs | GetDiagnosis → `Condition` |
| `COMPOSITION_ASSEMBLY` | Hela svaret → en `Composition` med sektioner (EURIDICE dokumentutbyte) | GetCareDocumentation → `Composition` *(ej implementerat)* |

---

## Snabbstart

**Krav:** Docker, Docker Compose, Java 21, Maven 3.9+

```bash
# Klona och starta hela stacken med mock-tjänster
git clone https://github.com/oskthu2/EHDS-brygga
cd EHDS-brygga
cp .env.example .env
make up          # startar gateway + bridge + 5 mock-containers

# Kör integrationstest mot lokal stack
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
curl "http://localhost:8080/fhir/SE2321000016-4HK5/Condition?patient.identifier=urn:oid:1.2.752.129.2.1.3.1|191212121212"

# Dokumentlista för samma patient
curl "http://localhost:8080/fhir/SE2321000016-4HK5/DocumentReference?patient.identifier=urn:oid:1.2.752.129.2.1.3.1|191212121212"

# Oscopat anrop (frågar alla konfigurerade VG:er via TAK+EI)
curl "http://localhost:8080/fhir/Condition?patient.identifier=urn:oid:1.2.752.129.2.1.3.1|191212121212"
```

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
│  bridge (Spring Boot)    │  Tre Maven-moduler i samma JVM-process:
│  ┌──────────────────────┐│  • fhir-server   – HAPI FHIR, providers, orchestrering
│  │ fhir-server          ││  • mapping-engine – semantisk översättning (3 lager)
│  │ mapping-engine       ││  • soap-client    – Apache CXF mot NTjP/VP
│  │ soap-client          ││
│  └──────────────────────┘│
└────────────┬─────────────┘
             │ SOAP/HTTPS (mTLS, RIVTA BP 2.1)
             ▼
      NTjP / VP → Regionens producent
```

I lokal utveckling: fem extra mock-containers (TAK, EI, Spärr, Logg, Backend-SOAP).

### Anropsflöde (VG-scopat)

```
Konsument       Gateway         Bridge                      Inera / VG
    │               │               │                           │
    │─GET /fhir/────►               │                           │
    │  {vg-hsa-id}/ │               │                           │
    │  Condition?   │               │                           │
    │  patient=...  │               │                           │
    │               │─X-VG-HSA-ID──►│                           │
    │               │  + strip      │                           │
    │               │  /fhir/prefix │                           │
    │               │               │                           │
    │        [JWT-validering — planerat, se §PoC]               │
    │               │               │                           │
    │               │               │─Slå upp logisk adress     │
    │               │               │  i vg-config.yaml         │
    │               │               │                           │
    │               │               │─EI: finns data?──────────►│
    │               │               │◄─ja/nej───────────────────│
    │               │               │                           │
    │               │          [Fas 1: FHIR→SOAP                │
    │               │           mappning lager 3+1+2a]          │
    │               │               │                           │
    │               │               │─GetDiagnosis:2───────────►│ (NTjP/VP)
    │               │               │                           │→ Producent
    │               │               │◄─SOAP-svar────────────────│
    │               │               │                           │
    │               │          [Fas 2: SOAP→FHIR                │
    │               │           mappning lager 3+1+2a+2b]       │
    │               │               │                           │
    │               │          [Post-query spärr:               │
    │               │           filter per ext-source-system]   │
    │               │               │                           │
    │               │          [Åtkomstlogg ATNA/BALP]          │
    │               │               │                           │
    │◄─200 OK Bundle─────────────────│                           │
```

---

## Konfiguration

### `bridge/fhir-server/src/main/resources/config/vg-config.yaml`

Definierar en rad per VG som bryggan känner till.

```yaml
vgConfigs:
  - vgHsaId: SE2321000016-4HK5        # VG:ns HSA-id — används som logisk adress i NTjP
    description: "VGR – Västra Götalandsregionen"
    source: SOAP                       # källstrategi: SOAP | FHIR_PASSTHROUGH

  - vgHsaId: SE2321000098-7XYZ
    description: "SLL – Stockholms läns landsting"
    source: SOAP

  # FHIR_PASSTHROUGH-exempel (när VG exponerar eget FHIR-API):
  # - vgHsaId: SE2321000999-DEMO
  #   description: "Demo-region med eget FHIR"
  #   source: FHIR_PASSTHROUGH
  #   fhirBaseUrl: https://fhir.demo-region.se/r4
```

| Fält | Typ | Beskrivning |
|---|---|---|
| `vgHsaId` | String | VG:ns HSA-id. Används som logisk adress i RIVTA-anropet (SOAP) eller som URL-diskriminator. |
| `source` | Enum | `SOAP` — anropa via NTjP. `FHIR_PASSTHROUGH` — proxy till `fhirBaseUrl`. |
| `fhirBaseUrl` | String | Krävs om `source: FHIR_PASSTHROUGH`. Bas-URL för VG:ns FHIR-server. |

### `bridge/fhir-server/src/main/resources/config/services.yaml`

Definierar en rad per RIVTA-tjänstekontrakt som bryggan hanterar.

```yaml
serviceContracts:
  - id: GetDiagnosis
    namespace: "urn:riv:clinicalprocess:activity:conditions:GetDiagnosisResponder:2"
    soapAction: "GetDiagnosis"
    fhirResource: Condition            # vilken FHIR-resurstyp som produceras
    transformer: GetDiagnosis          # namn på Java-mappningsklassen (lager 3)
    outputMode: RESOURCE_PER_ELEMENT   # RESOURCE_PER_ELEMENT | COMPOSITION_ASSEMBLY
    useTAK: true                       # slå upp fysisk adress via TAK
    useEI: true                        # kontrollera Engagemangsindex
    useSparr: true                     # kör post-query spärrkontroll
    useLogg: true                      # logga i ATNA/BALP-format
    searchParams:
      - name: "patient.identifier"
        required: true
```

| Fält | Typ | Beskrivning |
|---|---|---|
| `namespace` | String | RIVTA XML-namespace för tjänstekontraktet. Används som SOAP-action och för TAK-uppslag. |
| `transformer` | String | Namn som identifierar Java-mappningsklassen (lager 3). Konventionen är TK-id utan version. |
| `outputMode` | Enum | `RESOURCE_PER_ELEMENT` — ett svar-element → en FHIR-resurs. `COMPOSITION_ASSEMBLY` — hela svaret → en Composition (EURIDICE dokumentutbyte). |
| `useTAK` | Bool | Fråga TAK för att hämta fysisk URL för VG:ns producent. |
| `useEI` | Bool | Kontrollera EI om patienten har data hos VG:ns system (undviker onödiga SOAP-anrop). |
| `useSparr` | Bool | Aktivera post-query spärrkontroll via Säkerhetstjänsten. |
| `useLogg` | Bool | Logga åtkomsten (fire-and-forget). |

---

## Mappningsmotor

Mappningsmotorn är uppdelad i tre oberoende lager. Alla tre lager deltar i **fas 2**
(SOAP→FHIR). Lager 3+1+2a deltar också i **fas 1** (FHIR→SOAP, konstruktion av begäran).

```
Fas 1: FHIR-fråga → SOAP-begäran
  Lager 3 (TK-specifik Java-klass)
      └── Lager 1 (RIVTA JAXB-typer)
      └── Lager 2a (NamingSystem: OID → URI)

Fas 2: SOAP-svar → FHIR-resurser
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

**Datumsformat:** RIVTA använder heltalssträngar. Mapparen konverterar:
- `YYYYMMDD` → `YYYY-MM-DD`
- `YYYYMMDDHHmmss` → `YYYY-MM-DDTHH:mm:ss`

Fallback: strängen returneras oförändrad om formatet inte känns igen.

**I produktion:** JAXB-klasserna bör genereras automatiskt från RIVTA:s WSDL/XSD via
`maven-jaxb2-plugin`. Handskrivna klasser är tillräckliga för PoC men riskerar
divergens från specifikationen vid framtida TK-versionsuppdateringar.

### Lager 2a — NamingSystem (OID ↔ FHIR URI)

**Konfigurationsfil:** `bridge/mapping-engine/src/main/resources/naming-systems.yaml`
**Java-klass:** `NamingSystemRegistry`

```yaml
entries:
  - oid: "1.2.752.116.1.1.1.1.3"
    uri: "https://www.icd10.se/"
    description: "ICD-10-SE"
  - oid: "1.2.752.129.2.1.3.1"
    uri: "urn:oid:1.2.752.129.2.1.3.1"
    description: "Personnummer"
  - oid: "1.2.752.129.2.1.4.1"
    uri: "urn:oid:1.2.752.129.2.1.4.1"
    description: "HSA-id"
```

**Fallback:** OID:er utan matchning returneras som `urn:oid:{oid}` — konsumenten ser
alltid ett giltigt FHIR-URI, aldrig en rå OID.

Lägg till rader i `naming-systems.yaml` för varje kodsystem ett nytt TK introducerar.
Ingen kodändring behövs.

**Full tabell för implementerade OID-mappningar:**

| OID | FHIR URI | Beskrivning |
|---|---|---|
| `1.2.752.116.1.1.1.1.3` | `https://www.icd10.se/` | ICD-10-SE |
| `2.16.840.1.113883.6.3` | `http://hl7.org/fhir/sid/icd-10` | ICD-10 (internationell) |
| `1.2.752.129.2.1.3.1` | `urn:oid:1.2.752.129.2.1.3.1` | Personnummer |
| `1.2.752.129.2.1.3.3` | `urn:oid:1.2.752.129.2.1.3.3` | Samordningsnummer |
| `1.2.752.129.2.1.4.1` | `urn:oid:1.2.752.129.2.1.4.1` | HSA-id |
| `1.2.752.116.1.1.1.1.1` | `https://www.socialstyrelsen.se/KVA` | KVÅ |

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

**Hantering av okartlagda koder:** Om källkoden inte finns i tabellen använder mapparen
källkoden och källsystemets eget kodsystem — konsumenten ser alltid originalkoden, aldrig
ett tyst bortfall.

Lägg till nya `.yaml`-filer under `concept-maps/` och registrera dem i `ConceptMapRegistry`
(kräver en rad Java-kod per ny karta, se klassen för mönstret).

### Lager 3 — TK-specifik mappningsklass

**Interface:** `TkMapper<R, F>` i `mapping-engine/src/main/java/se/inera/ehds/mapping/tk/`

```java
public interface TkMapper<R, F> {
    List<F> map(R response, MapperContext context);
}
// MapperContext innehåller: patientSystem (OID-URI), patientValue (personnummer)
```

En konkret klass per tjänstekontrakt:

| Klass | Intyp (R) | Uttyp (F) | TK |
|---|---|---|---|
| `GetDiagnosisMapper` | `GetDiagnosisResponse` | `Condition` | GetDiagnosis:2 |
| `GetDocumentListMapper` | `GetDocumentListResponse` | `DocumentReference` | GetDocumentList:1 |

**GetDiagnosisMapper — vad den gör:**

| RIVTA-element | FHIR-element | Logik |
|---|---|---|
| `diagnosisHeader.patientId.root/extension` | `subject.identifier.system/value` | OID→URI via lager 2a |
| `diagnosisHeader.sourceSystemHSAId` | `recorder.identifier` + `ext-source-system` | HSA-id på båda ställena — recorder för läsbarhet, extension för spärrkontroll |
| `diagnosisHeader.documentTime` | `recordedDate` | Datumkonvertering |
| `diagnosisBody.diagnosisCode` | `code.coding` | OID→URI via lager 2a |
| `diagnosisBody.diagnosisType` | `category` | Kod→kod via lager 2b (DiagnosisType-karta) |
| `diagnosisBody.diagnosisTimePeriod.start` | `onsetDateTime` | Datumkonvertering |
| `diagnosisBody.diagnosisTimePeriod.end` | `abatementDateTime` | Om satt → `clinicalStatus=resolved`, annars `active` |
| *(härledd)* | `clinicalStatus` | `active` om inget slutdatum, `resolved` om slutdatum finns |
| *(konstant)* | `verificationStatus` | Alltid `confirmed` — RIVTA representerar bekräftade journaluppgifter |

**`ext-source-system`-extensionen** är kritisk: den sätts av mapparen och läses av
`SparrFilterService` för att identifiera vilken källkälla en resurs tillhör. Utan extensionen
kan resursen inte spärrfiltreras och passerar alltid.

---

## Lägga till ett nytt tjänstekontrakt

Fem steg — ingen ändring i gateway, HAPI-konfiguration eller befintliga orchestrare behövs.

### Steg 1: Lägg till rad i `services.yaml`

```yaml
- id: GetMedication
  namespace: "urn:riv:clinicalprocess:actoutcome:medication:GetMedicationHistoryResponder:2"
  soapAction: "GetMedicationHistory"
  fhirResource: MedicationStatement
  transformer: GetMedication            # matchar klassprefixet i steg 3
  outputMode: RESOURCE_PER_ELEMENT
  useTAK: true
  useEI: true
  useSparr: true
  useLogg: true
  searchParams:
    - name: "patient.identifier"
      required: true
```

### Steg 2: Lägg till JAXB-typer (lager 1)

Skapa ett nytt subpackage om TK:et har ett annat XML-namespace:

```
bridge/mapping-engine/src/main/java/se/inera/ehds/mapping/rivta/medication/
  package-info.java          ← @XmlSchema(namespace="urn:riv:...", elementFormDefault=QUALIFIED)
  GetMedicationHistory.java
  GetMedicationHistoryResponse.java
  MedicationHistoryType.java
  ...
```

### Steg 3: Implementera TkMapper (lager 3)

```java
// bridge/mapping-engine/src/main/java/se/inera/ehds/mapping/tk/getmedication/GetMedicationMapper.java
public class GetMedicationMapper
        implements TkMapper<GetMedicationHistoryResponse, MedicationStatement> {

    @Override
    public List<MedicationStatement> map(GetMedicationHistoryResponse response, MapperContext ctx) {
        // 1. Nollkontroll och resultatkodkontroll
        // 2. Iterera svar-element
        // 3. Sätt ext-source-system på varje resurs (krävs för spärrkontroll)
        // 4. Använd namingSystem.oidToUri() för alla OID-värden
        // 5. Använd conceptMaps.translate...() för kodmappningar
    }
}
```

**Kom ihåg `ext-source-system`:** varje FHIR-resurs måste ha extensionen satt till
`sourceSystemHSAId` från RIVTA-svaret, annars fungerar inte spärrfiltreringen.

### Steg 4: Lägg till SOAP SEI och klient

```java
// soap-client/src/main/java/se/inera/ehds/soap/sei/GetMedicationResponderInterface.java
@WebService(targetNamespace = "urn:riv:clinicalprocess:actoutcome:medication:GetMedicationHistoryResponder:2")
public interface GetMedicationResponderInterface {
    GetMedicationHistoryResponse getMedicationHistory(
            @WebParam(name = "LogicalAddress", ...) String logicalAddress,
            @WebParam(name = "GetMedicationHistoryRequest", ...) GetMedicationHistory request);
}
```

Kopiera mönstret från `GetDiagnosisClient` för klientklassen. De enda delarna som varierar
är: SEI-interface, request-klass och `CONSUMER_HSA_HEADER`-sättning (samma mönster).

### Steg 5: Registrera i Spring-kontexten

I `MappingEngineConfig.java`:

```java
@Bean
public GetMedicationMapper getMedicationMapper(NamingSystemRegistry naming, ConceptMapRegistry concepts) {
    return new GetMedicationMapper(naming, concepts);
}

@Bean
public GetMedicationClient getMedicationClient(AppProperties props) {
    return new GetMedicationClient(props.getBridgeHsaId());
}
```

Lägg sedan till en ny `ResourceProvider` och `Orchestrator` för `MedicationStatement` —
kopiera mönstret från `ConditionResourceProvider` + `QueryOrchestrator`.

---

## FHIR IG och profiler

Implementation Guide finns under `ig/`. Byggs med SUSHI (FSH-kompilator).

| Profil | FHIR-resurs | Förälderprofil | Kontrakt |
|---|---|---|---|
| `SEEHDSCondition` | Condition | IPS `Condition-uv-ips` | GetDiagnosis:2 |
| `SEEHDSDocumentReference` | DocumentReference | FHIR R4 base | GetDocumentList:1 |

**Extension `ext-source-system`** — kontextualiserad på både `Condition` och `DocumentReference`.
Bär källsystemets HSA-id som `Identifier` (system: HSA-OID, value: HSA-id-sträng).
Används av `SparrFilterService` för att avgöra vilket källsystem en resurs härstammar från.

```json
{
  "url": "https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-source-system",
  "valueIdentifier": {
    "system": "urn:oid:1.2.752.129.2.1.4.1",
    "value": "SE2321000016-4HK5"
  }
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
| **JAXB från WSDL** | Lager 1-klasser är handskrivna. Bör genereras från officiella RIVTA WSDL/XSD. | `mapping-engine/src/main/java/se/inera/ehds/mapping/rivta/` |
| **`COMPOSITION_ASSEMBLY`** | Utdataläget är konfigurbart men ej implementerat. Ger `UnsupportedOperationException` om aktiverat. | `QueryOrchestrator.java`, `DocumentQueryOrchestrator.java` |
| **CapabilityStatement per VG** | HAPI genererar ett globalt CS. Varje VG bör deklarera sina egna resurser. | Kräver `IServerConformanceProvider`-implementation |
| **PDL-loggformat** | `LoggService` loggar till mock. Formatet är inte validerat mot ATNA/BALP-specifikationen. | `LoggService.java` |
| **EI-kontraktsversion** | EI-mock använder förenklat HTTP-API. Ska använda RIVTA `GetEngagements:1`. | `EiService.java` |
| **Lokal tidzon** | `parseRivDate()` returnerar datum utan tidszon. Kräver explicit hantering av `Europe/Stockholm` → UTC. | `GetDiagnosisMapper.java`, `GetDocumentListMapper.java` |

---

## Projektstruktur

```
EHDS-brygga/
├── bridge/                        # Java-applikation (Maven multi-modul)
│   ├── fhir-server/               # Spring Boot + HAPI FHIR — providers, orchestrering
│   │   └── src/main/resources/
│   │       ├── config/services.yaml    ← TK-konfiguration (outputMode, transformer, flaggor)
│   │       └── config/vg-config.yaml  ← Per-VG-konfiguration (source strategy)
│   ├── mapping-engine/            # Mappningsbibliotek — lager 1+2a+2b+3
│   │   └── src/main/resources/
│   │       ├── naming-systems.yaml         ← Lager 2a: OID→URI-tabell
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
│   ├── sparr/                     # Säkerhetstjänsten — spärrar (alltid false i mock)
│   ├── logg/                      # Loggtjänsten — tar emot loggposter, loggar till stdout
│   └── backend/                   # RIVTA SOAP-producent — testdata i responses.json
│
├── ig/                            # FHIR Implementation Guide (SUSHI/FSH)
│   └── input/
│       ├── fsh/profiles/          # SEEHDSCondition, SEEHDSDocumentReference
│       ├── fsh/extensions/        # ext-source-system
│       ├── fsh/conceptmaps/       # DiagnosisTypeToCategoryMap
│       └── pagecontent/           # architecture.md, mapping-*.md
│
├── scripts/
│   ├── test.sh                    # Integrationstester mot live stack
│   ├── validate.sh                # Maven-build + FHIR Validator
│   └── build-ig.sh                # Bygger IG med SUSHI + FHIR Publisher
│
├── docker-compose.yml             # Lokal stack: gateway + bridge + 5 mocks
├── Makefile                       # make up/down/build/test/validate/ig/logs
└── .env.example                   # Miljövariabler för lokal körning
```

### Nyckelklasser att känna till

| Klass | Paket | Roll |
|---|---|---|
| `TenantInterceptor` | `fhir.` | Extraherar `X-VG-HSA-ID`-header → `RequestDetails.setAttribute("vgHsaId")` |
| `QueryOrchestrator` | `orchestration.` | Driver TAK→EI→SOAP(async)→Mappning→Spärr→Logg för Condition |
| `DocumentQueryOrchestrator` | `orchestration.` | Samma pipeline för DocumentReference |
| `SparrFilterService` | `service.` | Post-query spärrkontroll — läser `ext-source-system`-extension |
| `NamingSystemRegistry` | `mapping.naming.` | Lager 2a: OID→URI, läses från `naming-systems.yaml` |
| `ConceptMapRegistry` | `mapping.concept.` | Lager 2b: kod→kod, läses från `concept-maps/*.yaml` |
| `GetDiagnosisMapper` | `mapping.tk.getdiagnosis.` | Lager 3: GetDiagnosisResponse → List\<Condition\> |
| `GetDocumentListMapper` | `mapping.tk.getdocumentlist.` | Lager 3: GetDocumentListResponse → List\<DocumentReference\> |
| `OutputMode` | `config.` | Enum: `RESOURCE_PER_ELEMENT` \| `COMPOSITION_ASSEMBLY` |
| `SourceStrategy` | `config.` | Enum: `SOAP` \| `FHIR_PASSTHROUGH` |
