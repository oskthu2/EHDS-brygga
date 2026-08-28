# EHDS-bryggan mot Ineras "T2-katalogtjänster"-demomiljö

Det här dokumentet jämför EHDS-bryggans samverkansmockar (katalog-/adressuppslag,
medlemsverifiering, åtkomstintyg) med Ineras publika demoportal för T2-katalogtjänster
("Uppslagsdemo"), och beskriver vad som ändrats i den här branchen för att på sikt göra det
möjligt att peka bryggan mot de riktiga tjänsterna istället för mockarna.

## Demomiljöns flöde (Uppslagsdemo)

Demoportalen visar "hela samverkansmönstret" i fyra steg, för en logisk adress (HSA-id) och
valfri specifikation:

1. **T1 — tjänstesökning:** `GET {tjänstekatalogen}/Endpoint?organization.identifier={system}|{hsaId}&status=active[&implements={specifikation}]`
2. **F1 — medlemsverifiering:** `GET {federationsmedlemskatalogen}/OrganizationAffiliation?participating-organization.identifier={system}|{hsaId}&active=true`
3. **Åtkomstintyg:** utfärdas av den utfärdare som anslutningspunkten (Endpoint) anvisar,
   via OAuth2 `client_credentials` mot WSO2 Key Manager. Intyget är en JWT som avkodas utan
   signaturkontroll i labbet.
4. **Anrop på uppslagen adress:** via WSO2 API Gateway, som svarar 401 utan åtkomstintyg och
   200 med.

Katalogerna är FHIR R5 (`/tjanstekatalog/r5`, `/fed-katalog/r5`), formulärtjänsten som anropas
i exemplet är FHIR R4 (`/form-fhir/r4`). Identifierarsystemet för HSA-id är
`urn:oid:1.2.752.29.4.19`.

## Läget innan denna branch

EHDS-bryggan hade fem mockar (`mocks/tak`, `mocks/ei`, `mocks/sparr`, `mocks/logg`,
`mocks/backend`) som modellerade en helt annan, äldre samverkansmodell — en implicit,
NTjP-intern routingtabell istället för klientdriven katalogsökning:

| Egenskap | EHDS-bryggan (före) | T2-katalogtjänster-demon |
|---|---|---|
| Adressuppslag | `mocks/tak` exponerade ett eget REST-API (`GET /routing`, `GET /routing/address`) för fysisk adress — men **anropades aldrig** av `ntjp-proxy`. `ProxyProperties.ntjpUrl` pekade istället direkt på `mock-ntjp:4001` som om det vore den fysiska adressen. | Konsumenten slår själv upp fysisk adress via en FHIR `Endpoint`-sökning (T1) mot tjänstekatalogen, innan anropet görs. |
| Medlemsverifiering | Fanns inte. Ingen kontroll av att producenten var en aktiv part. | F1 mot federationsmedlemskatalogen (`OrganizationAffiliation`), obligatoriskt steg innan anrop. |
| Åtkomstintyg | Fanns inte. SOAP-anropen skickade ingen `Authorization`-header. | OAuth2 `client_credentials` mot en av anslutningspunkten anvisad utfärdare; API-gatewayen kräver token (401 utan, 200 med). |
| Katalogformat | Eget, proprietärt JSON-API (`{ routes: [...] }`). | FHIR-resurser (`Endpoint`, `OrganizationAffiliation`) sökta via standard FHIR-sökparametrar. |
| Identifierarsystem | HSA-id utan explicit `system` i mock-API:et. | `organization.identifier=urn:oid:1.2.752.29.4.19|{hsaId}` — redan konsekvent med bryggans egen `naming-systems.yaml`. |

**Konsekvens:** eftersom `mock-ntjp` (byggd från `mocks/tak`) aldrig implementerade en
SOAP-mottagande endpoint, gick varje VG-scopat `Condition`/`DocumentReference`-anrop via
`ntjp-proxy` mot en `404` från Express, fångades av `catch`-blocket i
`ConditionProxyController`/`DocumentReferenceProxyController`, och gav tomma bundlar. CI:s
`docker compose`-baserade E2E-körning maskerade detta (`continue-on-error: true` på
integrationstest-steget i `.github/workflows/ci.yml`), så felet syntes aldrig som en röd build.
Detta var alltså inte bara en skillnad mot demomiljön, utan ett existerande, dolt fel i
huvudflödet.

## Ändringar i denna branch

Mockarna och `ntjp-proxy` har byggts om för att spegla demomiljöns kontrakt och flöde:

- **`mocks/tak` → `mocks/tjanstekatalog`** (T1): exponerar `GET /Endpoint` som en riktig FHIR
  `searchset`-Bundle, filtrerbar på `organization.identifier` och `implements` (RIVTA-namespace).
- **`mocks/fedkatalog`** (nytt, F1): exponerar `GET /OrganizationAffiliation`, samma
  sökparameterform som demon.
- **`mocks/token-issuer`** (nytt): exponerar `.well-known/openid-configuration` och
  `POST /token` (`client_credentials`) och utfärdar en riktig, avkodbar JWT (HMAC-signerad med
  en dev-nyckel — ingen part validerar signaturen, i linje med demons eget påpekande om att
  intyget "avkodas ... utan signaturkontroll").
- **`mocks/backend`**: kräver nu `Authorization: Bearer <token>` på `POST /soap` — 401 utan,
  200 med, samma nolläge som demons sista steg visar.
- **`ntjp-proxy`**: nya klasser `CatalogDiscoveryService` (T1 + F1) och `AccessTokenService`
  (token, cachelagrad i minnet). `ConditionProxyController` och `DocumentReferenceProxyController`
  kör nu F1 → T1 → åtkomstintyg → SOAP-anrop i tur och ordning, istället för att anropa en
  hårdkodad "NTjP-URL" direkt. `GetDiagnosisClient`/`GetDocumentListClient` tar emot den
  uppslagna fysiska adressen och åtkomstintyget som parametrar och sätter
  `Authorization: Bearer`-headern på SOAP-anropet.
- Detta fixar även det dolda felet ovan: `ConditionProxyController`/`DocumentReferenceProxyController`
  anropar nu en verklig, adresserbar endpoint (den uppslagna via T1) istället för en
  URL utan mottagare.
- **`mocks/ei` kopplades in** i det oscopade ("alla VG") anropsflödet via ny `EiService`
  i `fhir-server`. Detta var, precis som TAK-routingen, tidigare bara dokumenterat men
  aldrig kopplat in: `mock-ei` saknades helt i `docker-compose.yml`, och
  `VgConfigLoader.findByHsaId(configs, null)` gav en `NullPointerException` för varje
  oscopat `Condition`/`DocumentReference`-anrop (`hsaId.equals(...)` på ett `null`-värde) —
  återigen maskerat av CI:s `continue-on-error`. `QueryOrchestrator`/`DocumentQueryOrchestrator`
  grenar nu explicit: känt `vgHsaId` → oförändrad enkel-VG-sökning; `null` (oscopat anrop) →
  EI avgör vilka VG:er som frågas, resultaten sammanfogas.

Se README:s [Katalogtjänster och åtkomstintyg (T1/F1)](../README.md#katalogtjänster-och-åtkomstintyg-t1f1)
och [Anropsflöde (oscopat — "alla VG")](../README.md#anropsflöde-oscopat--alla-vg) för
konfigurationsdetaljer.

## Kvarstående skillnader mot den riktiga demomiljön

Dessa gap är medvetet kvar i den här PoC:n:

| Gap | Beskrivning |
|---|---|
| **FHIR-version** | Demons kataloger är R5 (`/r5`-suffix); mockarna och `CatalogDiscoveryService` använder R4-strukturer. `Endpoint`/`OrganizationAffiliation` är i praktiken oförändrade mellan versionerna, men en riktig integration bör verifiera detta mot den faktiska profilen. |
| **Anvisad utfärdare** | I demon anvisar anslutningspunkten (Endpoint-resursen) vilken utfärdare som gäller för just den producenten. PoC:n använder en enda, statiskt konfigurerad utfärdare (`ntjp.token-issuer-url`) för alla VG:er — `Endpoint`-mocken bär ingen utfärdarreferens ännu. |
| **WSO2 API Gateway** | Demons åtkomstkontroll (401/200) sker i en API Gateway framför producenten. I PoC:n sitter samma kontroll i `mock-backend` självt, som får spela båda rollerna. |
| **mTLS / SAML** | RIVTA BP 2.1 kräver SITHS-certifikat (mTLS) och SAML-assertion utöver OAuth2-intyget. Fortfarande placeholder-kommentarer i `GetDiagnosisClient`/`GetDocumentListClient`. |
| **Engagemangsindex (EI): RIVTA-kontrakt** | `mocks/ei` och `EiService` (nu inkopplade i det oscopade anropsflödet) exponerar/konsumerar ett förenklat HTTP-API. Produktion ska använda RIVTA `GetEngagements:1`. |
| **Produktionens verkliga bas-URL:er** | Att faktiskt koppla mot de riktiga T2-katalogtjänsterna kräver TLS, klientcertifikat/nycklar utfärdade av Inera, och att byta `ntjp.tjanstekatalog-url` / `ntjp.fedkatalog-url` / `ntjp.token-issuer-url` (+ klient-id/secret) från mock-adresserna till de riktiga. Själva anropskontraktet (sökparametrar, resurstyper, tokenflöde) är redan detsamma. |

## Så byter du mockarna mot riktiga tjänster senare

Eftersom mockarnas HTTP-kontrakt nu speglar demomiljöns FHIR-sökningar och OAuth2-flöde,
är omkopplingen i princip bara konfiguration:

1. Sätt `ntjp.tjanstekatalog-url`, `ntjp.fedkatalog-url`, `ntjp.token-issuer-url` till de
   riktiga tjänsternas bas-URL:er (via WSO2 API Gateway).
2. Sätt `ntjp.token-client-id` / `ntjp.token-client-secret` till bryggans riktiga
   OAuth2-klientuppgifter hos Inera.
3. Lägg till mTLS-konfiguration (SITHS-klientcertifikat) på `RestTemplate`/CXF-klienterna —
   se `docs/t2-katalogtjanster.md`-gapen ovan och README:s PoC-begränsningstabell.
4. Verifiera att de riktiga katalogernas `Endpoint`/`OrganizationAffiliation`-resurser är
   R5-kompatibla med det `CatalogDiscoveryService` förväntar sig (fält som används:
   `Endpoint.address`, `Endpoint.status`, `OrganizationAffiliation.active`).
