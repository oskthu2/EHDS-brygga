# EHDS-brygga – Konventioner för Claude Code

## Projektöversikt

EHDS-brygga är en FHIR R4-brygga som översätter svenska RIVTA SOAP-anrop till FHIR R4-resurser.
Projektet består av fyra Maven-moduler under `bridge/`:

| Modul | Syfte |
|---|---|
| `mapping-engine` | RIVTA → FHIR-mappning, NamingSystem, ConceptMap |
| `fhir-server` | Spring Boot HAPI FHIR-server, Sparr-filter |
| `ntjp-proxy` | NTjP-proxy (routing) |
| `soap-client` | SOAP-klientgenerering (skip tester – externa anrop) |

## Hur man kör tester

```bash
# Hela bridge
cd bridge && mvn test

# Enskild modul
cd bridge && mvn test -pl mapping-engine
cd bridge && mvn test -pl fhir-server

# Enstaka testklass
cd bridge && mvn test -pl mapping-engine -Dtest=RivDateParserTest
```

## Testkonventioner

### Ramverk

- **JUnit 5** (`org.junit.jupiter:junit-jupiter`) i alla moduler
- **Mockito** (`org.mockito:mockito-core`) i fhir-server för Spring-beroenden
- **Maven Surefire 3.2.5** konfigurerat i varje modul

### Namngivning

- Testklassnamn: `<KlassnamnsomTeastas>Test`  
- Testmetodnamn: **svenska substantiv/verb som beskriver beteendet**, t.ex.:
  - `datumstrangMedAtta tecken_ger_iso_datum`
  - `nullIndata_ger_null`
  - `aktivt_statusCode_ger_CURRENT`
- Undvik prefix `test_` – JUnit 5 kräver det inte

### Organisation

Använd `@Nested`-klasser för att gruppera test på logiska grupper:

```java
class GetDiagnosisMapperTest {
    @Nested class NullOchTomInput { ... }
    @Nested class DiagnosKod { ... }
    @Nested class Patient { ... }
    @Nested class ProvenanceAgenter { ... }
}
```

### Vad som ska testas

1. **Null-/tomma indata** – mappers ska returnera `List.of()`, inte kasta undantag
2. **Icke-OK ResultCode** – ska returnera tom lista
3. **Fältmappning** – varje RIVTA-fält mappar till rätt FHIR-element
4. **OID→URI-konvertering** – via `NamingSystemRegistry` (läser `/naming-systems.yaml`)
5. **ConceptMap-översättning** – via `ConceptMapRegistry` (läser `/concept-maps/diagnosis-type.yaml`)
6. **Statusvärden** – `active`→`CURRENT`, övriga→`SUPERSEDED`
7. **Fail-closed-beteende** – `SparrFilterService` filtrerar bort poster utan giltigt HSA-id

### Testisolering

- `NamingSystemRegistry` och `ConceptMapRegistry` kan instansieras direkt i tester –
  de läser från `src/main/resources` som är på Maven-testklasspath automatiskt
- `SparrFilterService` behöver `RestTemplate`-mock – använd Mockito
- Undvik Spring-kontext i enhetstester (ingen `@SpringBootTest`)

### En test per beteende

Varje testmetod verifierar **ett** beteende. Tre rader assert i samma metod är ok om
de alla testar samma sak (t.ex. att ett fält har rätt system + kod + display).

## Projektstruktur (nyckelklasser)

```
mapping-engine/
  RivDateParser            – YYYYMMDD / YYYYMMDDHHmmss → ISO 8601
  ProvenanceBuilder        – Bygger Provenance med tre agenter (custodian/author/assembler)
  GetDiagnosisMapper       – GetDiagnosis → Condition + Provenance
  GetDocumentListMapper    – GetDocumentList → DocumentReference + Provenance
  DocBookToNarrativeTransformer – DocBook XML → FHIR Narrative XHTML
  NamingSystemRegistry     – OID ↔ URI (läser /naming-systems.yaml)
  ConceptMapRegistry       – Diagnostyp HD/BY → encounter-diagnosis/bi-diagnos

fhir-server/
  SparrFilterService       – Post-query Sparr-filter (fail-closed)
  AppProperties            – Spring Boot @ConfigurationProperties
```

## Viktiga designbeslut

- **Fail-closed Sparr**: saknat eller ogiltigt `careProviderHSAId` → filtrera bort
- **HSA-id-mönster**: `^SE[0-9]+-[A-Za-z0-9]+$`
- **DocBook**: använd `getTagName()` (inte `getLocalName()`) – namespace awareness är av
- **`headingTag(depth)`**: klampar till h2–h6
- **OID-fallback**: okänd OID returnerar `urn:oid:<oid>`
