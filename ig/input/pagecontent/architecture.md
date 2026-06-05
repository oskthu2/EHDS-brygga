# Arkitektur

## Översikt

EHDS-bryggan exponerar en enskild vårdgivares (VG) hälsodata via ett EURIDICE/FHIR R4-API.
Varje VG har en dedikerad API-yta under `/fhir/{vg-hsa-id}/`, och bryggan fungerar som en
direktöversättare mellan FHIR och regionens befintliga RIVTA SOAP-tjänster i nationell
tjänsteplattform (NTjP).

Systemet är utformat för att vara:

- **VG-scopat** – ett URL-segment per VG; ingen aggregering över VG-gränser i ett anrop
- **Konfigurationsdrivet** – nya tjänstekontrakt aktiveras via `vg-config.yaml` + ny mappningsklass
- **Stateless** – bryggan cachelagrar ingen patientdata; alla anrop är realtidsgenomströmning
- **Spårbart** – alla anrop loggas (ATNA/BALP) i Ineras loggtjänst
- **Säkert** – JWT-validering, mTLS mot NTjP, post-query spärrfiltrering

## Containers och moduler

EHDS-bryggan består av **två huvud-containers**: en gateway och en bryggtjänst.
Bryggtjänsten i sin tur är uppbyggd av **tre interna moduler**.
I testmiljö tillkommer fyra mock-containers samt en HAPI FHIR audit-databas.

| Container / modul | Teknisk karaktär | Syfte |
|---|---|---|
| **Gateway** | nginx (PoC), KONG + WSO2 APIM (prod) | TLS-terminering, URL-routing, extraherar `{vg-hsa-id}` → `X-VG-HSA-ID`-header. Serverar SMART `/.well-known/`-endpoints (statisk JSON). |
| **fhir-server** *(bryggtjänst, modul 1)* | Spring Boot + HAPI FHIR | FHIR Resource Providers. Orkestrering: parallella FHIR-anrop per VG-resurs → Spärr → Logg. Routing styrs av per-resurs-konfiguration i `vg-config.yaml`. |
| **ntjp-proxy** *(bryggtjänst, modul 2)* | Spring Boot + Apache CXF | All SOAP/RIVTA-logik. Innehåller mapping-engine och soap-client. Exponerar FHIR-API per VG. Deployerbar centralt eller nära VG. |
| **mapping-engine** *(bryggtjänst, modul 3)* | Maven-bibliotek (JAR) | RIVTA JAXB-typer, NamingSystemRegistry (OID↔URI), ConceptMapRegistry (RIVTA-kod → FHIR-kod), TK-specifika mappningsklasser (GetDiagnosisMapper, GetDocumentListMapper). |

### Mock-containers (testmiljö)

| Container | Port | Syfte |
|---|---|---|
| `mock-ntjp` | 4001 | NTjP / Nationell Tjänsteplattform (SOAP-router) |
| `mock-sparr` | 4003 | Säkerhetstjänsten (spärr) |
| `audit-db` | 4004 | HAPI FHIR R4 – lagrar AuditEvent-resurser (se [Audit-händelser](audit-events.html)) |
| `mock-backend` | 4005 | Producerande journalsystem (SOAP) |

## Systemkomponenter

### API Gateway (NGINX)

Ingångspunkt för alla externa FHIR-anrop. Hanterar:

- TLS-terminering
- Multi-tenant URL-routing: extraherar `{vg-hsa-id}` ur `/fhir/{vg-hsa-id}/{resurs}`
  och vidarebefordrar det som `X-VG-HSA-ID`-header till fhir-server
- *Planerat:* vidarebefordran till Åtkomstintygstjänsten för JWT-validering

### Åtkomstintygstjänst *(planerad)*

Validerar JWT-åtkomstintyg och kontrollerar att anropande system har rätt scope
(t.ex. `system/Condition.read`). Extraherar HoS-person, vårdgivare och vårdsyfte
ur JWT-claims och gör dessa tillgängliga för bryggtjänsten.

### fhir-server (Spring Boot + HAPI FHIR)

Kärnan i FHIR-laget. Tar emot och validerar FHIR-förfrågningar, extraherar VG-kontexten
från `X-VG-HSA-ID`-headern och driver anropsflödet:

1. Slår upp per-resurs-konfiguration för VG:n i `vg-config.yaml` (endpoint-URL + accessmetod)
2. Anropar VG-endpointen via HTTP/FHIR (ntjp-proxy eller nativt FHIR-API, beroende på `access`)
3. Filtrerar svaret mot Säkerhetstjänsten (spärr, organisationsnivå)
4. Returnerar `Bundle` till konsumenten (AuditEvent POSTas asynkront, se [Audit-händelser](audit-events.html))

### ntjp-proxy (Spring Boot + Apache CXF)

Separat tjänst (port 8091) som äger all RIVTA SOAP-logik. Tar emot FHIR-förfrågningar
från fhir-server och returnerar FHIR-resurser, men utför internt:

1. Konverterar FHIR-frågeparametrar till SOAP-request (URI → OID via `NamingSystemRegistry`)
2. Anropar NTjP/VP med mTLS och korrekt `LogicalAddress` + `x-rivta-original-serviceconsumer-hsaid`
3. Mappar SOAP-svar till FHIR-resurser + Provenance-poster (via `GetDiagnosisMapper`)
4. Returnerar en lokal `Bundle` med `searchMode=match` (Condition) och `searchMode=include` (Provenance)

Gränssnittet mot fhir-server är rent FHIR HTTP — fhir-server vet inte om endpointen är
ntjp-proxy eller ett nativt FHIR-API. Det styrs enbart av `endpointUrl` per resurstyp i `vg-config.yaml`.

### mapping-engine (Maven-bibliotek)

Delat bibliotek som innehåller:

- RIVTA JAXB-typer (`mapping/rivta/`)
- `NamingSystemRegistry` — OID ↔ URI-mappning, inkl. HL7 Sweden basprofiler-r4
- `ConceptMapRegistry` — RIVTA-kod → FHIR-kod (t.ex. diagnostyp → category)
- `GetDiagnosisMapper` / `GetDocumentListMapper` — TK-specifik mappningslogik
- `MappedDiagnosisEntry` — record som håller `(Condition, Provenance)` länkade genom pipelinen

### NTjP / VP (Nationell Tjänsteplattform / Virtuell Producent)

Routar SOAP-anropet till rätt producent baserat på tjänstekontrakt och logisk adress.
SOAP-klienten skickar VG:ns HSA-id som logisk adress; NTjP resolver den fysiska adressen.

### Säkerhetstjänsten (Spärr)

Post-query-filtrering: körs efter att SOAP-svaret mappats till FHIR-resurser, på fhir-server-sidan.
Sparrtjänsten kontrollerar yttre spärr (`careProviderHSAId`, organisationsnivå) och inre spärr
(`careUnitHSAId`, avdelningsnivå). Identifierarna läses från `Provenance.agent`:
`careProviderHSAId` som `agent[role=custodian]` (juridiskt ansvarig) och
`careUnitHSAId` som `agent[role=author]` (informationsägare). Varken
`careProviderHSAId` eller `careUnitHSAId` placeras som extension inne i FHIR-resursen.

Spärrkontrollen är synkron och sker per unikt `(careProviderHSAId, careUnitHSAId)`-par i svaret
(cachelagrat per request). Fail-closed gäller: saknas giltig Provenance eller misslyckas
anropet till spärrtjänsten filtreras posten bort.

**PoC-begränsning:** En vårdgivare från en spärrad enhet som ändå har rätt att ta del av
informationen (t.ex. nödsituationer, break-the-glass) hanteras inte. Se [Kända begränsningar](#kanda-begransningar).

### Loggtjänst (ATNA/BALP)

Åtkomstloggning i ATNA/BALP-format. Logganropet sker fire-and-forget efter att
Bundle levererats till klienten.

## Anropsflöde

### VG-scopat anrop: GET /fhir/{vg-hsa-id}/Condition

```
Konsument        Gateway       fhir-server        ntjp-proxy        Inera / VG
    |                |               |                  |                |
    |-- GET /fhir/   |               |                  |                |
    |   {vg-hsa-id}/ |               |                  |                |
    |   Condition?   |               |                  |                |
    |   patient={pnr}→               |                  |                |
    |                |-- X-VG-HSA-ID |                  |                |
    |                |   + routing  →|                  |                |
    |                |               |                  |                |
    |          [JWT-validering, scope: system/Condition.read — planerat]
    |                |               |                  |                |
    |                |               |-- GET Condition? →               |
    |                |               |   (FHIR HTTP)    |               |
    |                |               |                  |-- SOAP        |
    |                |               |                  |   GetDiagnosis:2
    |                |               |                  |              →| (NTjP/VP)
    |                |               |                  |               |→ Producent
    |                |               |                  |               |←- SOAP-svar
    |                |               |                  |←-- SOAP-svar --|
    |                |               |                  |                |
    |                |               |              [Mappning SOAP→FHIR  |
    |                |               |               Condition+Provenance]
    |                |               |←-- Bundle (match+include) --------|
    |                |               |                  |                |
    |                |           [pair Condition↔Provenance via target]
    |                |               |                  |                |
    |                |           [Spärr: careProviderHSAId               |
    |                |            (organisationsnivå)]  |                |
    |                |               |                  |                |
    |←-- 200 OK -----|←-- Bundle ----|                  |                |
    |                |           (AuditEvent POSTas asynkront → audit-db)|
```

**Viktigt:** ett VG-scopat anrop resulterar i exakt ett SOAP-anrop till exakt en
logisk adress. Bryggan aggregerar inte data över VG-gränser. En producent kan ha
data från flera vårdenheter inom samma VG — dessa inkluderas alla, men data som
tillhör andra VG:er i samma producentsystem filtreras bort av spärrkomponenten.

## Provenance

För varje Condition skapas en länkad Provenance-resurs som inkluderas i svaret med
`Bundle.entry.search.mode = include`. Provenance följer IHE QEDm-mönstret och innehåller
tre agentroller:

| Agent-roll | Källa | Syfte |
|---|---|---|
| `custodian` | `careProviderHSAId` | Juridiskt ansvarig vårdgivare (organisationsnivå, används av Sparrtjänsten) |
| `author` | `careUnitHSAId` | Informationsägare vårdenhet som förvaltar journalposten |
| `assembler` | `EHDS_BRIDGE_HSA_ID` (env-variabel) | EHDS-bryggan som sammansatte FHIR-bundlen |

`Provenance.target` pekar på `urn:uuid:{Condition.id}`. `MappedDiagnosisEntry` håller
Condition och Provenance länkade som ett par genom hela pipelinen, inklusive Sparr-filtret —
om en Condition filtreras bort tas även dess Provenance bort.

## Mappningsmotor

Tvåfas, tre-lagers-arkitektur, implementerad i `mapping-engine`-modulen:

### Fas 1: FHIR → SOAP (begäran)

Lager 3 (TK-specifik mappningsklass) + Lager 1 (RIVTA JAXB-typer) + Lager 2a (NamingSystem)
konstruerar SOAP-begäranstrukturen från FHIR-frågeparametrarna.
`NamingSystemRegistry.uriToOid()` konverterar t.ex. `http://electronichealth.se/identifier/personnummer`
→ `1.2.752.129.2.1.3.1`.

### Fas 2: SOAP → FHIR (svar)

Lager 3 + Lager 1 + Lager 2a + Lager 2b (ConceptMap) producerar FHIR-resurser + Provenance
från SOAP-svarets element. `NamingSystemRegistry.oidToUri()` konverterar OID:er till
kanoniska URI:er (HL7 Sweden basprofiler-r4).

| Lager | Innehåll | Klass/fil |
|-------|----------|-----------|
| 1 | RIVTA JAXB-typer: `CVType`, `PersonIdType`, `DatePeriodType` m.fl. | `mapping/rivta/`, `mapping/rivta/doclist/` |
| 2a | OID ↔ FHIR URI (bidirektionell): `1.2.752.129.2.1.3.1` ↔ personnummer-URI | `NamingSystemRegistry` / `naming-systems.yaml` |
| 2b | Kod ↔ kod: RIVTA diagnosTyp → FHIR `category` | `ConceptMapRegistry` / `concept-maps.yaml` |
| 3 | TK-specifik logik per tjänstekontrakt | `GetDiagnosisMapper`, `GetDocumentListMapper` |

## Lägga till ett nytt tjänstekontrakt

1. **`vg-config.yaml`** — lägg till en ny post under `resources` för varje VG som ska
   tillhandahålla den nya resurstypen: ange `access: tk` (via ntjp-proxy) eller `access: fhir`
   (nativt FHIR-API) samt `endpointUrl`
2. **Java-mappningsklass** — implementera mappningslogiken (lager 3) i `mapping-engine`;
   returnera `List<MappedDiagnosisEntry>` (eller motsvarande record med resurs + Provenance)
3. **JAXB-typer** — lägg till kontraktets XML-typer i `mapping/rivta/` (lager 1)
4. **ntjp-proxy controller** — registrera en ny `@RestController` i ntjp-proxy som
   tar emot FHIR-frågan, anropar SOAP via `soap-client` och returnerar en lokal Bundle
5. **ProxyBeanConfig** — registrera eventuellt ny SOAP-tjänsteklient (CXF)
6. **FHIR-profil** — ny FSH-profil i denna IG + mappningssida

Ingen ändring i Gateway, fhir-server-orkestrerare eller HAPI-konfiguration behövs.

## Säkerhet och dataskydd

### Autentisering och auktorisation

- Inkommande klienter autentiseras via OAuth 2.0 / SMART on FHIR *(JWT-validering planerat)*
- Anrop till NTjP autentiseras med SITHS-certifikat (mTLS, RIVTA BP 2.1)
- Bryggan agerar som teknisk aktör med eget HSA-id (`SE2321000999-EHDS`) mot NTjP

### Åtkomstkontroll

- Post-query: spärrtjänsten filtrerar svar *efter* SOAP-anrop och mappning
- Fail-closed: saknas giltig Provenance, ogiltigt HSA-id eller infrastrukturfel filtreras posten bort
- Yttre spärr: `careProviderHSAId` (organisationsnivå) från `Provenance.agent[role=custodian]`
- Inre spärr: `careUnitHSAId` (avdelningsnivå) från `Provenance.agent[role=author]`

### Loggning (PDL / ATNA/BALP)

Alla åtkomster loggas med patientidentifierare, aktör, tidpunkt och ändamål.
Loggposter skickas till Ineras loggtjänst i ATNA/BALP-format.

## Driftsättning

Bryggan driftsätts som **två huvud-containers** i Kubernetes:

- `gateway` — NGINX (PoC), TLS-terminering, multi-tenant URL-routing
- `bryggtjänst` — innehåller fhir-server och ntjp-proxy med delad mapping-engine

De tre interna modulerna (`fhir-server`, `ntjp-proxy`, `mapping-engine`) kan vid behov
deployeras som separata pods, t.ex. för att köra ntjp-proxy nära en specifik VG.

I lokal utveckling tillkommer fyra mock-containers (NTjP, Spärr, Backend-SOAP) plus
`audit-db` (HAPI FHIR för AuditEvent-lagring) via `docker-compose.yml` i projektets rot.

## Kända begränsningar {#kanda-begransningar}

### Break-the-glass

En vårdgivare som tillhör en spärrad enhet men ändå har rätt att ta del av informationen
(t.ex. nödsituationer) hanteras inte. Denna logik kräver kontextinformation om inloggad
användares behörighet och är out of scope för PoC:en. EHDS-bryggan applicerar alla spärrar
utan undantag.

### DocumentReference: Provenance

Sparr-filtret körs på organisationsnivå (`careProviderHSAId`) för både Condition och
DocumentReference. Däremot skapas inga Provenance-resurser för DocumentReference-poster –
hela kedjan (author/custodian/assembler) som finns för Condition saknas. Detta är en känd
PoC-begränsning.
