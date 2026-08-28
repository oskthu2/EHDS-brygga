# Arkitektur

## Översikt

EHDS-bryggan exponerar en enskild vårdgivares (VG) hälsodata via ett EURIDICE/FHIR R4-API.
Varje VG har ett dedikerat virtuellt FHIR-endpoint under `/{vg-hsa-id}/fhir/`, och bryggan
fungerar som en direktöversättare mellan FHIR och regionens befintliga RIVTA SOAP-tjänster
i nationell tjänsteplattform (NTjP).

Systemet är utformat för att vara:

- **VG-scopat** – varje VG har ett eget virtuellt FHIR-endpoint (`/{vgHsaId}/fhir`) med eget CapabilityStatement; ingen aggregering över VG-gränser i ett anrop
- **Konfigurationsdrivet** – nya tjänstekontrakt aktiveras via `vg-config.yaml` + ny mappningsklass
- **Stateless** – bryggan cachelagrar ingen patientdata; alla anrop är realtidsgenomströmning
- **Spårbart** – alla anrop loggas som FHIR `AuditEvent` i lokal audit-databas (se [Audit-händelser](audit-events.html))
- **Säkert** – JWT-validering, mTLS mot NTjP, post-query spärrfiltrering

## Containers och moduler

EHDS-bryggan består av **två huvud-containers**: en gateway och en bryggtjänst.
Bryggtjänsten i sin tur är uppbyggd av **tre interna moduler**.
I testmiljö tillkommer fyra mock-containers samt en HAPI FHIR audit-databas.

| Container / modul | Teknisk karaktär | Syfte |
|---|---|---|
| **Gateway** | nginx (PoC), KONG + WSO2 APIM (prod) | TLS-terminering, URL-routing. Vidarebefordrar hela sökvägen `/{vgHsaId}/fhir/**` till fhir-server oförändrad. |
| **fhir-server** *(bryggtjänst, modul 1)* | Spring Boot + HAPI FHIR | FHIR Resource Providers. Extraherar `{vgHsaId}` ur URL-sökvägen. Virtualiserar ett FHIR-endpoint per VG: `/{vgHsaId}/fhir/metadata` returnerar VG-specifikt CapabilityStatement baserat på `vg-config.yaml`. Orkestrering: FHIR-anrop per VG-resurs → Spärr → Logg. Serverar `/.well-known/smart-configuration` (gemensamt). |
| **ntjp-proxy** *(bryggtjänst, modul 2)* | Spring Boot + Apache CXF | All SOAP/RIVTA-logik. Innehåller mapping-engine och soap-client. Exponerar FHIR-API per VG. Deployerbar centralt eller nära VG. |
| **mapping-engine** *(bryggtjänst, modul 3)* | Maven-bibliotek (JAR) | RIVTA JAXB-typer, NamingSystemRegistry (OID↔URI), ConceptMapRegistry (RIVTA-kod → FHIR-kod), TK-specifika mappningsklasser (GetDiagnosisMapper, GetDocumentListMapper). |

### Mock-containers (testmiljö)

| Container | Port | Syfte |
|---|---|---|
| `mock-tjanstekatalog` | 4001 | T1 — tjänstesökning (FHIR `Endpoint`-sökning, fysisk adress per VG+tjänstekontrakt) |
| `mock-sparr` | 4003 | Säkerhetstjänsten (spärr) |
| `audit-db` | 4004 | HAPI FHIR R4 – lagrar AuditEvent-resurser (se [Audit-händelser](audit-events.html)) |
| `mock-backend` | 4005 | Producerande journalsystem (SOAP, kräver Bearer-åtkomstintyg) |
| `mock-fedkatalog` | 4006 | F1 — medlemsverifiering (FHIR `OrganizationAffiliation`-sökning) |
| `mock-token-issuer` | 4007 | Åtkomstintygsutfärdare (OAuth2 `client_credentials`) |

Katalogtjänsterna (`mock-tjanstekatalog`, `mock-fedkatalog`) och åtkomstintygsutfärdaren
(`mock-token-issuer`) speglar API-kontraktet i Ineras "T2-katalogtjänster"-demomiljö, så att
bryggan på sikt kan pekas om mot de riktiga tjänsterna genom att enbart byta bas-URL:er.

## Systemkomponenter

### API Gateway (NGINX)

Ingångspunkt för alla externa FHIR-anrop. Hanterar:

- TLS-terminering
- URL-routing: vidarebefordrar `/{vgHsaId}/fhir/**` till fhir-server med sökvägen oförändrad
- *Planerat:* vidarebefordran till Åtkomstintygstjänsten för JWT-validering

### Åtkomstintygstjänst *(planerad)*

Validerar JWT-åtkomstintyg och kontrollerar att anropande system har rätt scope
(t.ex. `system/Condition.read`). Extraherar HoS-person, vårdgivare och vårdsyfte
ur JWT-claims och gör dessa tillgängliga för bryggtjänsten.

### fhir-server (Spring Boot + HAPI FHIR)

Kärnan i FHIR-laget. Virtualiserar ett FHIR-endpoint per VG under `/{vgHsaId}/fhir/**`.
Extraherar VG-kontexten ur URL-sökvägen och driver anropsflödet:

1. Slår upp per-resurs-konfiguration för VG:n i `vg-config.yaml` (endpoint-URL + accessmetod)
2. Anropar VG-endpointen via HTTP/FHIR (ntjp-proxy eller nativt FHIR-API, beroende på `access`)
3. Filtrerar svaret mot Säkerhetstjänsten (spärr, organisationsnivå)
4. Returnerar `Bundle` till konsumenten (AuditEvent POSTas asynkront, se [Audit-händelser](audit-events.html))

`/{vgHsaId}/fhir/metadata` returnerar ett VG-specifikt CapabilityStatement som enbart
listar de resurstyper som VG:n stöder enligt `vg-config.yaml`. Okänt `vgHsaId` → 404.

`/.well-known/smart-configuration` returnerar gemensam SMART-konfiguration för alla VG:ar
(authorizationendpoint, token, introspection, scopes).

### ntjp-proxy (Spring Boot + Apache CXF)

Separat tjänst (port 8091) som äger all RIVTA SOAP-logik. Tar emot FHIR-förfrågningar
från fhir-server och returnerar FHIR-resurser, men utför internt:

1. **F1 — medlemsverifiering:** `CatalogDiscoveryService` frågar federationsmedlemskatalogen
   om VG:n har ett aktivt medlemskap. Nekas anropet direkt om inte.
2. **T1 — tjänstesökning:** `CatalogDiscoveryService` slår upp den fysiska adressen för
   rätt tjänstekontrakt hos tjänstekatalogen (`Endpoint`-sökning på HSA-id + RIVTA-namespace).
3. **Åtkomstintyg:** `AccessTokenService` hämtar (och cachelagrar) ett OAuth2-åtkomstintyg
   via `client_credentials` från åtkomstintygsutfärdaren.
4. Konverterar FHIR-frågeparametrar till SOAP-request (URI → OID via `NamingSystemRegistry`)
5. Anropar den uppslagna adressen med `LogicalAddress` + `x-rivta-original-serviceconsumer-hsaid`
   + `Authorization: Bearer {access_token}` (mTLS: se [PoC-begränsningar](#kanda-begransningar))
6. Mappar SOAP-svar till FHIR-resurser + Provenance-poster (via `GetDiagnosisMapper`)
7. Returnerar en lokal `Bundle` med `searchMode=match` (Condition) och `searchMode=include` (Provenance)

Gränssnittet mot fhir-server är rent FHIR HTTP — fhir-server vet inte om endpointen är
ntjp-proxy eller ett nativt FHIR-API. Det styrs enbart av `endpointUrl` per resurstyp i `vg-config.yaml`.
T1/F1-uppslaget mot katalogtjänsterna sker internt i ntjp-proxy, transparent för fhir-server.

### mapping-engine (Maven-bibliotek)

Delat bibliotek som innehåller:

- RIVTA JAXB-typer (`mapping/rivta/`)
- `NamingSystemRegistry` — OID ↔ URI-mappning, inkl. HL7 Sweden basprofiler-r4
- `ConceptMapRegistry` — RIVTA-kod → FHIR-kod (t.ex. diagnostyp → category)
- `GetDiagnosisMapper` / `GetDocumentListMapper` — TK-specifik mappningslogik
- `MappedDiagnosisEntry` — record som håller `(Condition, Provenance)` länkade genom pipelinen

### Katalogtjänster (T1/F1) och åtkomstintygsutfärdare

I stället för att förlita sig på en implicit NTjP-intern routingtabell slår ntjp-proxy själv
upp den fysiska adressen och verifierar federationsmedlemskap innan anrop görs — samma
mönster som Ineras "T2-katalogtjänster"-demomiljö (Uppslagsdemo):

- **Federationsmedlemskatalogen (F1):** `GET /OrganizationAffiliation?participating-organization.identifier={system}|{vgHsaId}&active=true`
- **Tjänstekatalogen (T1):** `GET /Endpoint?organization.identifier={system}|{vgHsaId}&status=active&implements={rivtaNamespace}`
- **Åtkomstintygsutfärdaren:** `POST /token` (`grant_type=client_credentials`) — mockad WSO2 Key Manager-motsvarighet

Producenten (mock-backend) kräver ett `Authorization: Bearer`-huvud på SOAP-anropet och
svarar 401 utan det — samma nolläge som demomiljön visar för anrop utan åtkomstintyg.

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

### VG-scopat anrop: GET /{vgHsaId}/fhir/Condition

```
Konsument        Gateway       fhir-server        ntjp-proxy        Inera / VG
    |                |               |                  |                |
    |-- GET           |               |                  |                |
    |   /{vgHsaId}/  |               |                  |                |
    |   fhir/        |               |                  |                |
    |   Condition?   |               |                  |                |
    |   patient={pnr}→               |                  |                |
    |                |-- routing    →|                  |                |
    |                |   (sökväg    |                  |                |
    |                |   oförändrad)|                  |                |
    |                |               |                  |                |
    |          [JWT-validering, scope: system/Condition.read — planerat]
    |                |               |                  |                |
    |                |               |-- GET Condition? →               |
    |                |               |   (FHIR HTTP)    |               |
    |                |               |                  |-- F1: aktiv medlem? -→ (fedkatalog)
    |                |               |                  |←-- ja/nej -----|
    |                |               |                  |-- T1: fysisk URL? --→ (tjänstekatalog)
    |                |               |                  |←-- Endpoint.address -|
    |                |               |                  |-- token (client_credentials) --→ (åtkomstintygsutfärdare)
    |                |               |                  |←-- access_token -----|
    |                |               |                  |-- SOAP        |
    |                |               |                  |   GetDiagnosis:2
    |                |               |                  |   + Bearer   →| (uppslagen adress)
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

### Loggning (PDL / AuditEvent)

Alla åtkomster loggas som FHIR `AuditEvent`-resurser med patientidentifierare, aktör,
tidpunkt och ändamål. Loggposter POSTas asynkront till en dedikerad HAPI FHIR-instans
(`audit-db`). Se [Audit-händelser](audit-events.html) för detaljer.

## Driftsättning

Bryggan driftsätts som **två huvud-containers** i Kubernetes:

- `gateway` — NGINX (PoC), TLS-terminering, multi-tenant URL-routing
- `bryggtjänst` — innehåller fhir-server och ntjp-proxy med delad mapping-engine

De tre interna modulerna (`fhir-server`, `ntjp-proxy`, `mapping-engine`) kan vid behov
deployeras som separata pods, t.ex. för att köra ntjp-proxy nära en specifik VG.

I lokal utveckling tillkommer sex mock-containers (Tjänstekatalog, Federationsmedlemskatalog,
Åtkomstintygsutfärdare, Spärr, Backend-SOAP, samt EI/Logg som ännu inte är inkopplade i
anropsflödet) plus `audit-db` (HAPI FHIR för AuditEvent-lagring) via `docker-compose.yml`
i projektets rot.

## Kända begränsningar {#kanda-begransningar}

### Break-the-glass

En vårdgivare som tillhör en spärrad enhet men ändå har rätt att ta del av informationen
(t.ex. nödsituationer) hanteras inte. Denna logik kräver kontextinformation om inloggad
användares behörighet och är out of scope för PoC:en. EHDS-bryggan applicerar alla spärrar
utan undantag.

### DocumentReference: Provenance

Provenance skapas för DocumentReference-poster på samma sätt som för Condition —
med `custodian` (`careProviderHSAId`), `author` (`careUnitHSAId`) och `assembler`
(bryggan). Sparr-filtret körs därmed på fullständig organisationsnivå för båda
resurstyper.
