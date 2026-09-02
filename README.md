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

### Post-query spärr på organisations- och avdelningsnivå

Spärrkontrollen körs **efter** att svar mappats till FHIR-resurser. Identifierarna läses
från `Provenance.agent` — inte från extensions inne i resursen:
- Yttre spärr: `careProviderHSAId` (organisationsnivå) från `Provenance.agent[role=custodian]`
- Inre spärr: `careUnitHSAId` (avdelningsnivå) från `Provenance.agent[role=author]`

Varje svar innehåller en `Provenance`-resurs per Condition med tre agenter:
- `custodian` = juridiskt ansvarig vårdgivare (`careProviderHSAId`) — yttre Sparr-nyckel
- `author` = informationsägare vårdenhet (`careUnitHSAId`) — inre Sparr-nyckel
- `assembler` = bryggan (`EHDS_BRIDGE_HSA_ID`)

Fail-closed gäller: saknas giltig Provenance, ogiltigt HSA-id eller infrastrukturfel
filtreras posten bort. `careProviderHSAId` placeras **inte** som extension inne i FHIR-resursen.

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

I lokal utveckling: sju extra mock-containers (Tjänstekatalog, Federationsmedlemskatalog,
Åtkomstintygsutfärdare, EI, Spärr, Logg, Backend-SOAP) — se [Katalogtjänster och
åtkomstintyg (T1/F1)](#katalogtjänster-och-åtkomstintyg-t1f1).

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
    │               │               │─GET /fhir/{vg}/───────►                   │
    │               │               │  Condition?patient=..  │                   │
    │               │               │                       │─F1: aktiv medlem?─►(fedkatalog)
    │               │               │                       │◄──ja/nej──────────│
    │               │               │                       │─T1: fysisk URL?───►(tjänstekatalog)
    │               │               │                       │◄──Endpoint.address─│
    │               │               │                       │─token (client_cred)►(åtkomstintygsutfärdare)
    │               │               │                       │◄──access_token────│
    │               │               │                       │                   │
    │               │               │                       │─GetDiagnosis:2────►(uppslagen adress)
    │               │               │                       │  + Bearer-token   │→ Producent
    │               │               │                       │◄──SOAP-svar───────│
    │               │               │                       │                   │
    │               │               │                       │  [SOAP→FHIR        │
    │               │               │                       │   mappning +       │
    │               │               │                       │   Provenance]      │
    │               │               │◄──Bundle (Condition   │                   │
    │               │               │   + Provenance)────────                   │
    │               │               │                       │                   │
    │               │         [Post-query Sparr (fail-closed):│                   │
    │               │          yttre: careProviderHSAId      │                   │
    │               │          inre:  careUnitHSAId]         │                   │
    │               │               │                       │                   │
    │               │         [Åtkomstlogg ATNA/BALP]        │                   │
    │               │               │                       │                   │
    │◄─200 OK Bundle─────────────────│                       │                   │
```

### Anropsflöde (oscopat — "alla VG")

Ett anrop utan `{vgHsaId}` i sökvägen (`GET /fhir/Condition`) känner inte till vilka VG:er
som ska frågas. `QueryOrchestrator`/`DocumentQueryOrchestrator` frågar då Engagemangsindexet
(EI) om vilka logiska adresser som har ett engagemang för patienten inom det aktuella
tjänstekontraktet, och kör sedan samma VG-scopade pipeline (F1 → T1 → åtkomstintyg → SOAP →
Sparr → Logg) per träff. Resultaten sammanfogas i en gemensam Bundle — **utan
ägarskaps-deduplicering**: eftersom mock-backend inte skiljer producent på `LogicalAddress`,
ger detta i PoC:n dubblerade träffar när flera VG:er "äger" samma underliggande data (se
E2E-testerna: 3 diagnoser × 2 VG = 6 för `/fhir/Condition`).

```
fhir-server                    EI                    (per engagerad VG)
    │                           │                            │
    │─GET /engagement?──────────►                            │
    │  patientId&namespace      │                            │
    │◄──[{logicalAddress}, ...]─│                            │
    │                           │                            │
    │─── för varje logicalAddress: F1 → T1 → åtkomstintyg → SOAP → Sparr → Logg ───►
    │◄──────────────────────────────────────────────────────────────────────────────│
    │                                                                                │
    │  [Bundle-resultat sammanfogas, total = summan av alla VG:ers träffar]          │
```

EI-uppslaget är **fail-safe, inte fail-closed**: kan EI inte nås frågas ingen VG (tom
Bundle) — det oscopade anropet gör aldrig en blind sökning mot samtliga konfigurerade VG:er.

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

Konfigurerar proxyns katalogtjänsteuppslag (T1/F1), åtkomstintyg och autentisering.

```properties
ntjp.tjanstekatalog-url=http://mock-tjanstekatalog:4001
ntjp.fedkatalog-url=http://mock-fedkatalog:4006
ntjp.token-issuer-url=http://mock-token-issuer:4007
ntjp.token-client-id=ehds-brygga
ntjp.token-client-secret=mock-secret
ntjp.org-identifier-system=urn:oid:1.2.752.29.4.19
ntjp.bridge-hsa-id=SE2321000999-EHDS
server.port=8091
```

| Egenskap | Beskrivning |
|---|---|
| `ntjp.tjanstekatalog-url` | Tjänstekatalogens URL för T1 (fysisk adressuppslagning) |
| `ntjp.fedkatalog-url` | Federationsmedlemskatalogens URL för F1 (medlemsverifiering) |
| `ntjp.token-issuer-url` | Åtkomstintygsutfärdarens URL (OAuth2 `client_credentials`) |
| `ntjp.token-client-id` / `ntjp.token-client-secret` | Bryggans klientuppgifter mot åtkomstintygsutfärdaren |
| `ntjp.org-identifier-system` | Identifierarsystem för HSA-id i katalogsökningarna (`urn:oid:1.2.752.29.4.19`) |
| `ntjp.bridge-hsa-id` | Bryggans HSA-id som skickas i `x-rivta-original-serviceconsumer-hsaid` och används som `assembler` i Provenance |

### Katalogtjänster och åtkomstintyg (T1/F1)

Uppslagsflödet i `ntjp-proxy` (`CatalogDiscoveryService` + `AccessTokenService`) följer samma
mönster som Ineras "T2-katalogtjänster"-demomiljö (Uppslagsdemo): bryggan slår själv upp
producentens fysiska adress och verifierar federationsmedlemskap, istället för att förlita sig
på en implicit NTjP-routingtabell:

1. **F1 — medlemsverifiering:** `GET {fedkatalogUrl}/OrganizationAffiliation?participating-organization.identifier={system}|{vgHsaId}&active=true`
2. **T1 — tjänstesökning:** `GET {tjanstekatalogUrl}/Endpoint?organization.identifier={system}|{vgHsaId}&status=active&implements={rivtaNamespace}`
3. **Åtkomstintyg:** `POST {tokenIssuerUrl}/token` (`grant_type=client_credentials`) — cachelagras i minnet tills det närmar sig utgång
4. **Anrop:** SOAP-anrop mot adressen från steg 2, med `Authorization: Bearer {access_token}`

Se [PoC-begränsningar](#poc-begränsningar-och-produktionsgap) för vad som skiljer denna PoC
från den riktiga demomiljön (bl.a. fast konfigurerad utfärdare istället för en per
anslutningspunkt anvisad sådan, och R4- istället för R5-profilerade kataloger), och
[`docs/t2-katalogtjanster.md`](docs/t2-katalogtjanster.md) för en fullständig
gap-analys mot Ineras "T2-katalogtjänster"-demomiljö.

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
| `GetDocumentListMapper` | `GetDocumentListResponse` | `List<MappedDocumentEntry>` | GetDocumentList:1 |

`MapperContext` skickas med vid varje mappning och innehåller:
- `patientSystem` — FHIR-URI för patientidentifierarsystemet
- `patientValue` — patientens identifierarvärde
- `bridgeHsaId` — bryggans HSA-id (sätts som `assembler`-agent i Provenance)

**`GetDiagnosisMapper` — producerar `MappedDiagnosisEntry(Condition, Provenance)` per diagnos:**

| RIVTA-element | FHIR-element | Logik |
|---|---|---|
| `diagnosisHeader.patientId.root/extension` | `subject.identifier.system/value` | OID→URI via lager 2a |
| `diagnosisHeader.sourceSystemHSAId` | `Condition.meta.source` | `urn:oid:{HSA_OID}#{hsaId}` — spårbarhet |
| `diagnosisHeader.careProviderHSAId` | `Provenance.agent[custodian]` | Juridiskt ansvarig — yttre Sparr-nyckel |
| `diagnosisHeader.careUnitHSAId` | `Provenance.agent[author]` | Informationsägare vårdenhet — inre Sparr-nyckel |
| `diagnosisHeader.documentTime` | `recordedDate` + `Provenance.recorded` | Datumkonvertering |
| `diagnosisBody.diagnosisCode` | `code.coding` | OID→URI via lager 2a |
| `diagnosisBody.diagnosisType` | `category` | Kod→kod via lager 2b (DiagnosisType-karta) |
| `diagnosisBody.diagnosisTimePeriod.start` | `onsetDateTime` | Datumkonvertering |
| `diagnosisBody.diagnosisTimePeriod.end` | `abatementDateTime` | Om satt → `clinicalStatus=resolved`, annars `active` |
| `diagnosisBody.assertedDate` *(EPS)* | `ext-asserted-date` | Administrativt datum, skiljer sig från `recordedDate` |
| *(härledd)* | `clinicalStatus` | `active` om inget slutdatum, `resolved` om slutdatum finns |
| *(konstant)* | `verificationStatus` | Alltid `confirmed` |
| `bridgeHsaId` från `MapperContext` | `Provenance.agent[assembler]` | Bryggan som sammansättande aktör |

**Sparr-integrationen (fail-closed):** `SparrFilterService` i `fhir-server` läser
`careProviderHSAId` från `Provenance.agent[role=custodian]` och `careUnitHSAId` från
`Provenance.agent[role=author]`. Saknas giltig Provenance eller giltigt HSA-id, eller
misslyckas anropet till spärrtjänsten, filtreras posten bort.

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
        // 3. Sätt meta.source = urn:oid:{HSA_OID}#{sourceSystemHSAId} — spårbarhet
        // 4. Bygg Provenance: custodian=careProviderHSAId, author=careUnitHSAId — Sparr-nyckel
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

### Extensions och meta på Condition

| Fält | Källa | Användning |
|---|---|---|
| `Condition.meta.source` | `sourceSystemHSAId` | Källsystemets HSA-id som URI (`urn:oid:{HSA_OID}#{hsaId}`); spårbarhet |
| `ext-asserted-date` | EPS `assertedDate` | Administrativt diagnosdatum; enda kvarvarande extension |

Extension-URL-prefix: `https://ehds-brygga.inera.se/fhir/StructureDefinition/`.

Exempel på `meta.source`:

```json
{
  "meta": {
    "source": "urn:oid:1.2.752.129.2.1.4.1#SE2321000016-4HK5"
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
      "type": { "coding": [{ "system": "http://terminology.hl7.org/CodeSystem/provenance-participant-type", "code": "custodian" }]},
      "who": { "identifier": { "system": "urn:oid:1.2.752.129.2.1.4.1", "value": "SE2321000016-4HK5" }}
    },
    {
      "type": { "coding": [{ "system": "http://terminology.hl7.org/CodeSystem/provenance-participant-type", "code": "author" }]},
      "who": { "identifier": { "system": "urn:oid:1.2.752.129.2.1.4.1", "value": "SE2321000016-E000000001" }}
    },
    {
      "type": { "coding": [{ "system": "http://terminology.hl7.org/CodeSystem/provenance-participant-type", "code": "assembler" }]},
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
| **mTLS mot producenten** | CXF-klienten har plats-hållarkommentar för SITHS-keystore. Inga certifikat konfigurerade. | `GetDiagnosisClient.java` rad 45 |
| **SAML WS-Security** | RIVTA BP 2.1 kräver SAML-assertion i SOAP-headern, utöver det nya OAuth2-åtkomstintyget. Ej implementerat. | `GetDiagnosisClient.java` rad 43 |
| **Fast konfigurerad åtkomstintygsutfärdare** | I demomiljön anvisar anslutningspunkten (Endpoint) vilken utfärdare som gäller per producent. PoC:n använder en enda, statiskt konfigurerad utfärdare (`ntjp.token-issuer-url`) för alla VG:er. | `AccessTokenService.java`, `ProxyProperties.java` |
| **Katalogtjänster: R4 istället för R5** | Demomiljöns tjänstekatalog/federationskatalog är FHIR R5 (`/r5`-suffix). Mockarna och `CatalogDiscoveryService` använder R4-strukturer (`Endpoint`/`OrganizationAffiliation` är i praktiken oförändrade mellan R4 och R5) för att undvika ett extra FHIR-versionsberoende i PoC:n. | `mocks/tjanstekatalog`, `mocks/fedkatalog`, `CatalogDiscoveryService.java` |
| **Åtkomstintygssignatur** | `mock-token-issuer` signerar med en delad dev-nyckel men ingen part validerar signaturen (`mock-backend` kontrollerar bara att ett Bearer-värde finns) — motsvarande demomiljöns eget påpekande att intyget där avkodas "utan signaturkontroll". | `mocks/token-issuer/server.js`, `mocks/backend/server.js` |
| **JAXB från WSDL** | Lager 1-klasser är handskrivna. Bör genereras från officiella RIVTA WSDL/XSD. | `mapping-engine/.../mapping/rivta/` |
| **`COMPOSITION_ASSEMBLY`** | Utdataläget är reserverat men ej implementerat i ntjp-proxy. | `ntjp-proxy` saknar Composition-mapper |
| **CapabilityStatement per VG** | HAPI genererar ett globalt CS. Varje VG bör deklarera sina egna resurser. | Kräver `IServerConformanceProvider`-implementation |
| **PDL-loggformat** | `LoggService` loggar till mock. Formatet är inte validerat mot ATNA/BALP-specifikationen. | `LoggService.java` |
| **EI-kontraktsversion** | EI är inkopplat i det oscopade anropsflödet, men mock-ei och `EiService` använder ett förenklat HTTP-API. Ska använda RIVTA `GetEngagements:1`. | `EiService.java`, `mocks/ei/server.js` |
| **Lokal tidzon** | `parseRivDate()` returnerar datum utan tidszon. Kräver explicit hantering av `Europe/Stockholm` → UTC. | `GetDiagnosisMapper.java`, `GetDocumentListMapper.java` |
| **Sparr: break-the-glass** | En konsument från en spärrad enhet som ändå har rätt till informationen (nödsituation) hanteras inte. Kräver kontextinfo om inloggad användares behörighet. | `SparrFilterService.java` |
| **Patient kontra personal-anrop** | `SparrFilterService` körs ovillkorligt på varje svar. NDI-scenario 2 (patient ser egen data) ska inte spärrfiltreras, men bryggan gör idag ingen skillnad på anropskontext. Se [`docs/ndi-anvandningsscenarier.md`](docs/ndi-anvandningsscenarier.md). | `SparrFilterService.java`, `QueryOrchestrator.java` |
| **Ombud (NDI-scenario 3)** | Medvetet parkerat — ingen NFF-integration eller fullmaktskontroll finns. Se parkeringsavsnittet i [`docs/ndi-anvandningsscenarier.md`](docs/ndi-anvandningsscenarier.md). | Saknas helt |

---

## Projektstruktur

```
EHDS-brygga/
├── bridge/                        # Java-applikation (Maven multi-modul)
│   ├── fhir-server/               # Spring Boot + HAPI FHIR — providers, orkestrering, Sparr, Logg
│   │   └── src/main/resources/
│   │       └── config/vg-config.yaml  ← Per-VG-konfiguration (fhirEndpointUrl)
│   ├── ntjp-proxy/                # Spring Boot — SOAP/RIVTA-logik, T1/F1-uppslag, exponerar FHIR-API
│   │   └── src/main/resources/
│   │       └── application.properties ← ntjp.tjanstekatalog-url, ntjp.fedkatalog-url, ntjp.token-issuer-url
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
├── mocks/                         # Sju mock-containers (Node.js/Express)
│   ├── tjanstekatalog/            # T1 — FHIR Endpoint-sökning, fysisk adress per VG+TK
│   ├── fedkatalog/                # F1 — FHIR OrganizationAffiliation-sökning, aktivt medlemskap
│   ├── token-issuer/              # Åtkomstintygsutfärdare — OAuth2 client_credentials (JWT)
│   ├── ei/                        # Engagemangsindex — patientengagemang per TK
│   ├── sparr/                     # Säkerhetstjänsten — kontroll mot careProviderHSAId
│   ├── logg/                      # Loggtjänsten — tar emot loggposter, loggar till stdout
│   └── backend/                   # RIVTA SOAP-producent — kräver Bearer-token, testdata i responses.json
│
├── ig/                            # FHIR Implementation Guide (SUSHI/FSH)
│   └── input/
│       ├── fsh/profiles/          # SEEHDSCondition, SEEHDSDocumentReference
│       ├── fsh/extensions/        # ext-asserted-date
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
| `QueryOrchestrator` | `fhir-server` | VG-scopat: FHIR-anrop → Sparr → Logg. Oscopat: EI → samma pipeline per engagerad VG → sammanfogning, för Condition |
| `DocumentQueryOrchestrator` | `fhir-server` | Samma pipeline för DocumentReference |
| `EiService` | `fhir-server` | Frågar Engagemangsindexet vilka VG:er som har engagemang för patienten (endast oscopade anrop) |
| `FhirProxyClient` | `fhir-server` | HAPI FHIR-klient mot VG-endpoint; parsar Condition+Provenance ur Bundle |
| `SparrFilterService` | `fhir-server` | Post-query Sparr (fail-closed) — läser HSA-id från Provenance.agent (custodian/author) |
| `ConditionProxyController` | `ntjp-proxy` | F1 → T1 → åtkomstintyg → SOAP → Mapping → Bundle (Condition+Provenance) |
| `CatalogDiscoveryService` | `ntjp-proxy` | T1 (tjänstekatalog) och F1 (federationsmedlemskatalog) mot katalogtjänsterna |
| `AccessTokenService` | `ntjp-proxy` | Hämtar och cachelagrar åtkomstintyg (OAuth2 `client_credentials`) |
| `NamingSystemRegistry` | `mapping-engine` | Lager 2a: OID↔URI, `oidToUri()` och `uriToOid()` |
| `ConceptMapRegistry` | `mapping-engine` | Lager 2b: kod→kod, läses från `concept-maps/*.yaml` |
| `GetDiagnosisMapper` | `mapping-engine` | Lager 3: `GetDiagnosisResponse` → `List<MappedDiagnosisEntry>` |
| `GetDocumentListMapper` | `mapping-engine` | Lager 3: `GetDocumentListResponse` → `List<MappedDocumentEntry>` |
| `MappedDiagnosisEntry` | `mapping-engine` | Record: `(Condition condition, Provenance provenance)` |
| `MappedDocumentEntry` | `mapping-engine` | Record: `(DocumentReference documentReference, Provenance provenance)` |
