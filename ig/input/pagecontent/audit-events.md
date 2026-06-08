# Audit-händelser

EHDS-bryggan loggar patientdataåtkomst som FHIR `AuditEvent`-resurser och lagrar dem i en
dedikerad HAPI FHIR-instans (audit-databasen). Mönstret följer IHE BALP (Basic Audit Log Patterns).

## Tre loggningspunkter

| Händelsetyp | Profil | Loggningspunkt | Vem loggar |
|---|---|---|---|
| [eHM-åtkomst](#ehm-access) | [SEEHDSAuditEventEhmAccess] | Konsument frågar bryggan | fhir-server |
| [Spärrtillämpning](#sparr-filter) | [SEEHDSAuditEventSparrFilter] | Post-query-filter körs | fhir-server |
| [Proxy-hämtning](#proxy-fetch) | [SEEHDSAuditEventProxyFetch] | SOAP-svar mottaget och konverterat | ntjp-proxy |

[SEEHDSAuditEventEhmAccess]: StructureDefinition-se-ehds-audit-event-ehm-access.html
[SEEHDSAuditEventSparrFilter]: StructureDefinition-se-ehds-audit-event-sparr-filter.html
[SEEHDSAuditEventProxyFetch]: StructureDefinition-se-ehds-audit-event-proxy-fetch.html

Loggning i NTjP och hos TK-producenten sker oberoende i nationell infrastruktur.

## Gemensam struktur

Alla tre profiler delar:

| Element | Värde |
|---|---|
| `type` | DCM#110112 "Query" |
| `action` | `R` (Read) |
| `recorded` | Tidsstämpel (UTC, obligatorisk) |
| `outcome` | `0` = framgång, `4`/`8` = fel (se respektive profil) |
| `source.observer` | HSA-id för den loggande bryggtjänsten |
| `entity[patient]` | Patient-identifierare (personnummer/samordningsnummer) |

Händelsetypen särskiljs via `subtype` med kod från
[SEEHDSAuditEventSubtype](CodeSystem-se-ehds-audit-event-subtype.html).

## eHM-åtkomst {#ehm-access}

**Profil:** [SEEHDSAuditEventEhmAccess]  
**Subtype:** `ehm-access`

Loggas av fhir-server för varje inkommande patientbunden fråga. Innehåller:

- `agent[system]` (requestor=true om ingen användare): eHM-applikationen, identifierad via JWT-claim `client_id` eller `azp`
- `agent[user]` (requestor=true, valfri): inloggad vårdpersonal, identifierad via JWT-claim `fhirUser` eller `sub` (när skild från `client_id`); utelämnas vid rent systemanrop
- `agent[bridge]` (requestor=false): fhir-server med DCM#110153 "Source Role ID"
- `purposeOfEvent`: från JWT-claim `purpose_of_use` eller extraherat ur `scope` (t.ex. `TREAT`, `ETREAT`); utelämnas om saknas
- `entity[patient]`: patientidentifierare
- `entity[query]`: Base64-koded sträng med resurstyp, VG HSA-id och resultCount

**Nuläge:** implementerad i `LoggService` — AuditEvent POSTas asynkront till audit-databasens `/AuditEvent`-endpoint.

## Spärrtillämpning {#sparr-filter}

**Profil:** [SEEHDSAuditEventSparrFilter]  
**Subtype:** `sparr-filter`

Loggas av fhir-server efter `SparrFilterService` körts. Fångar utfallet av spärrkontrollerna:

- `outcome = 0`: sparrtjänsten svarade utan fel
- `outcome = 4`: sparrtjänsten nåddes ej — fail-closed har tillämpats
- `entity[filterResult].detail[resultCount]`: antal poster som passerade filtret

Ger revisionsspår för hur många poster som faktiskt lämnades ut efter sekretessfiltrering.

**Nuläge:** implementerad i `LoggService.logSparrFilter()` — anropas av orchestratorerna direkt efter att `SparrFilterService` körts.

## Proxy-hämtning {#proxy-fetch}

**Profil:** [SEEHDSAuditEventProxyFetch]  
**Subtype:** `proxy-fetch`

Loggas av ntjp-proxy efter att ett SOAP-anrop till NTjP slutförts och konverterats till FHIR:

- `agent[caller]` (requestor=true): fhir-server (bridgeHsaId) som initierade anropet
- `agent[proxy]` (requestor=false): ntjp-proxy med `network.address` = NTjP-endpoint URL
- `entity[query]`: resurstyp, logisk adress (VG HSA-id), resultCount
- `outcome = 8` om SOAP-anropet misslyckades

Möjliggör spårning på transaktionsnivå: vilka SOAP-anrop gjordes, mot vilka VG:er, med vilket resultat.

**Nuläge:** implementerad i `ProxyAuditService.logProxyFetch()` i ntjp-proxy — anropas av `ConditionProxyController` och `DocumentReferenceProxyController` efter varje SOAP-anrop.

## Infrastruktur

Audit-databasen är en HAPI FHIR R4-instans (`audit-db` i docker-compose) utan annan affärslogik.
`LoggService` i fhir-server POSTar AuditEvent-resurser asynkront (fire-and-forget) till
`{EHDS_AUDIT_FHIR_URL}/AuditEvent`. Misslyckade POST-anrop loggas som varningar men blockerar
inte svarsleveransen till konsumenten.

Audit-databasen kan frågas med standard FHIR-sök:

```
GET /AuditEvent?patient.identifier=system|value&date=gt2024-01-01
GET /AuditEvent?entity-type=system|code&_count=100
```
