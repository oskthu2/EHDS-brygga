# Arkitektur

## Översikt

EHDS-bryggan exponerar en enskild vårdgivares (VG) hälsodata via ett EURIDICE/FHIR R4-API.
Varje VG har en dedikerad API-yta under `/fhir/{vg-hsa-id}/`, och bryggan fungerar som en
direktöversättare mellan FHIR och regionens befintliga RIVTA SOAP-tjänster i nationell
tjänsteplattform (NTjP).

Systemet är utformat för att vara:

- **VG-scopat** – ett URL-segment per VG; ingen aggregering över VG-gränser i ett anrop
- **Konfigurationsdrivet** – nya tjänstekontrakt aktiveras via `services.yaml` + ny mappningsklass
- **Stateless** – bryggan cachelagrar ingen patientdata; alla anrop är realtidsgenomströmning
- **Spårbart** – alla anrop loggas (ATNA/BALP) i Ineras loggtjänst
- **Säkert** – JWT-validering, mTLS mot NTjP, post-query spärrfiltrering

## Systemkomponenter

### API Gateway (NGINX)

Ingångspunkt för alla externa FHIR-anrop. Hanterar:

- TLS-terminering
- Multi-tenant URL-routing: extraherar `{vg-hsa-id}` ur `/fhir/{vg-hsa-id}/{resurs}`
  och vidarebefordrar det som `X-VG-HSA-ID`-header till bryggtjänsten
- *Planerat:* vidarebefordran till Åtkomstintygstjänsten för JWT-validering

### Åtkomstintygstjänst *(planerad)*

Validerar JWT-åtkomstintyg och kontrollerar att anropande system har rätt scope
(t.ex. `system/Condition.read`). Extraherar HoS-person, vårdgivare och vårdsyfte
ur JWT-claims och gör dessa tillgängliga för bryggtjänsten.

### Bryggtjänst (Spring Boot + HAPI FHIR, Java)

Kärnkomponenten. Innehåller tre interna Maven-moduler:

**fhir-server** — tar emot och validerar FHIR-förfrågningar, extraherar VG-kontexten
från `X-VG-HSA-ID`-headern och driver anropsflödet:

1. Slår upp logisk adress för VG:n i `vg-config.yaml`
2. Frågar EI om patienten har data registrerat hos VG:ns system
3. Delegerar till mappningsmotorn och SOAP-klienten
4. Filtrerar svaret mot Säkerhetstjänsten (spärr)
5. Loggar åtkomsten (ATNA/BALP)

**mapping-engine** — tvåfas semantisk översättning (se [Mappningsmotor](#mappningsmotor))

**soap-client** — Apache CXF-klient mot NTjP/VP, mTLS (SITHS), RIVTA BP 2.1

### EI (Engagemangsindex)

Ineras index över vilka patienter som har data i vilka system. Används för att bekräfta
att VG:ns system har information om patienten innan SOAP-anropet görs — sparar onödiga
anrop till producenten.

### NTjP / VP (Nationell Tjänsteplattform / Virtuell Producent)

Routar SOAP-anropet till rätt producent baserat på tjänstekontrakt och logisk adress.
SOAP-klienten skickar VG:ns HSA-id som logisk adress; NTjP resolver den fysiska adressen.

### Säkerhetstjänsten (Spärr)

Post-query-filtrering: körs efter att SOAP-svaret mappats till FHIR-resurser.
Filtrerar bort resurser vars källsystem (`ext-source-system`-extension) patienten
har spärrat eller som anroparen saknar behörighet till.

Spärrkontrollen är synkron och sker per unika källsystem i svaret. Vid fel mot
Säkerhetstjänsten gäller *fail-open* — bryggan döljer aldrig data på grund av
infrastrukturfel.

### Loggtjänst (ATNA/BALP)

Åtkomstloggning i ATNA/BALP-format. Logganropet sker fire-and-forget efter att
Bundle levererats till klienten.

## Anropsflöde

### VG-scopat anrop: GET /fhir/{vg-hsa-id}/Condition

```
Konsument          Gateway          Bryggtjänst           Inera / VG
    |                  |                  |                    |
    |-- GET /fhir/     |                  |                    |
    |   {vg-hsa-id}/   |                  |                    |
    |   Condition?     |                  |                    |
    |   patient={pnr} →|                  |                    |
    |                  |-- X-VG-HSA-ID   |                    |
    |                  |   + strip prefix→|                    |
    |                  |                  |                    |
    |              [JWT-validering, scope: system/Condition.read — planerat]
    |                  |                  |                    |
    |                  |                  |-- Slå upp logisk  |
    |                  |                  |   adress i        |
    |                  |                  |   vg-config.yaml  |
    |                  |                  |                    |
    |                  |                  |-- EI: finns data? →|
    |                  |                  |←-- ja/nej ---------|
    |                  |                  |                    |
    |                  |           [Mappning FHIR→SOAP         |
    |                  |            lager 3+1+2a]              |
    |                  |                  |                    |
    |                  |                  |-- GetDiagnosis:2  →| (NTjP/VP)
    |                  |                  |                    |→ Regionens producent
    |                  |                  |                    |←- SOAP-svar
    |                  |                  |←-- SOAP-svar ------|
    |                  |                  |                    |
    |                  |           [Mappning SOAP→FHIR         |
    |                  |            lager 3+1+2a+2b]           |
    |                  |                  |                    |
    |                  |           [Spärr/filtrering:          |
    |                  |            ta bort resurser från VG:er|
    |                  |            som patienten spärrat]     |
    |                  |                  |                    |
    |                  |           [Åtkomstlogg ATNA/BALP]     |
    |                  |                  |                    |
    |←-- 200 OK -------|←-- Bundle -------|                    |
```

**Viktigt:** ett VG-scopat anrop resulterar i exakt ett SOAP-anrop till exakt en
logisk adress. Bryggan aggregerar inte data över VG-gränser. En producent kan ha
data från flera vårdenheter inom samma VG — dessa inkluderas alla, men data som
tillhör andra VG:er i samma producentsystem filtreras bort av spärrkomponenten.

## Mappningsmotor

Tvåfas, tre-lagers-arkitektur:

### Fas 1: FHIR → SOAP (begäran)

Lager 3 (TK-specifik mappningsklass) + Lager 1 (RIVTA JAXB-typer) + Lager 2a (NamingSystem)
konstruerar SOAP-begäranstrukturen från FHIR-frågeparametrarna.

### Fas 2: SOAP → FHIR (svar)

Lager 3 + Lager 1 + Lager 2a + Lager 2b (ConceptMap) producerar FHIR-resurser från
SOAP-svarets element.

| Lager | Innehåll | Klass/fil |
|-------|----------|-----------|
| 1 | RIVTA JAXB-typer: `CVType`, `PersonIdType`, `DatePeriodType` m.fl. | `mapping/rivta/`, `mapping/rivta/doclist/` |
| 2a | OID ↔ FHIR URI: `1.2.752.129.2.1.3.1` → personnummer-system | `NamingSystemRegistry` / `naming-systems.yaml` |
| 2b | Kod ↔ kod: RIVTA diagnosTyp → FHIR `category` | `ConceptMapRegistry` / `concept-maps.yaml` |
| 3 | TK-specifik logik per tjänstekontrakt | `GetDiagnosisMapper`, `GetDocumentListMapper` |

### Utdataläge per informationsmängd

Styrs av `outputMode` i `services.yaml`:

| Värde | Beskrivning | Exempel |
|-------|-------------|---------|
| `RESOURCE_PER_ELEMENT` | Varje SOAP-element → en FHIR-resurs | GetDiagnosis → `Condition` |
| `COMPOSITION_ASSEMBLY` | Flera element → en `Composition` med sektioner (EURIDICE dokumentutbyte) | GetCareDocumentation → `Composition` *(planerat)* |

## Lägga till ett nytt tjänstekontrakt

1. **`services.yaml`** — lägg till en ny rad med `id`, `namespace`, `fhirResource`,
   `transformer` och `outputMode`
2. **Java-mappningsklass** — implementera `TkMapper<ResponseType, FhirType>` som
   `GetDiagnosisMapper` / `GetDocumentListMapper` (lager 3)
3. **JAXB-typer** — lägg till kontraktets XML-typer i `mapping/rivta/` (lager 1)
4. **SOAP SEI** — `@WebService`-interface för det nya kontraktet i `soap-client`
5. **FHIR-profil** — ny FSH-profil i denna IG + mappningssida

Ingen ändring i Gateway, orkestrerare eller HAPI-konfiguration behövs.

## Säkerhet och dataskydd

### Autentisering och auktorisation

- Inkommande klienter autentiseras via OAuth 2.0 / SMART on FHIR *(JWT-validering planerat)*
- Anrop till NTjP autentiseras med SITHS-certifikat (mTLS, RIVTA BP 2.1)
- Bryggan agerar som teknisk aktör med eget HSA-id (`SE2321000999-EHDS`) mot NTjP

### Åtkomstkontroll

- Post-query: spärrtjänsten filtrerar svar *efter* SOAP-anrop och mappning
- 0-tolerans för läckage: filtrering sker alltid oavsett om spärrkontroll lyckas
  (fail-open innebär att data visas vid infrastrukturfel — inte att spärrar ignoreras)

### Loggning (PDL / ATNA/BALP)

Alla åtkomster loggas med patientidentifierare, aktör, tidpunkt och ändamål.
Loggposter skickas till Ineras loggtjänst i ATNA/BALP-format.

## Driftsättning

Bryggan driftsätts som två containers i Kubernetes:

- `gateway` — NGINX, TLS-terminering, multi-tenant routing
- `bridge` — Spring Boot fat JAR med mapping-engine och soap-client inbyggda

I lokal utveckling tillkommer fem mock-containers (TAK, EI, Spärr, Logg, Backend-SOAP)
via `docker-compose.yml` i projektets rot.
