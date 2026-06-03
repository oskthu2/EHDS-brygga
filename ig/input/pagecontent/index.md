# SE EHDS Brygga – Implementation Guide

## Vad är EHDS-bryggan?

EHDS-bryggan är en tjänst som möjliggör åtkomst till svenska hälsodata via FHIR R4 API, i enlighet med
kraven i EU:s förordning om det europeiska hälsodatautrymmet (EHDS – European Health Data Space).

Bryggan fungerar som ett översättningslager mellan:

- **FHIR R4** – det moderna, öppna API-protokoll som EHDS kräver för primär och sekundär användning av hälsodata
- **Ineras RIVTA-tjänstekontrakt** – de etablerade SOAP/XML-baserade nationella tjänstekontrakten som svenska
  journalsystem och vårdinformationssystem redan implementerar

Genom EHDS-bryggan kan FHIR-klienter hämta patientdata från nationella källor utan att källsystemen
behöver bygga om sina befintliga integrationer.

## Syfte med denna Implementation Guide

Denna Implementation Guide (IG) dokumenterar och definierar de FHIR-profiler och mappningar som
EHDS-bryggan använder. Den riktar sig till:

- Systemutvecklare som integrerar mot EHDS-bryggan via FHIR
- Arkitekter som utvärderar EHDS-bryggan för sin organisation
- Informatiker och kliniker som vill förstå hur svenska hälsodata representeras i FHIR

## Tjänstekontrakt och mappningar

EHDS-bryggan mappar Ineras RIVTA-tjänstekontrakt till FHIR R4-resurser. Mappningarna är
konfigurationsdrivna och nya tjänstekontrakt kan läggas till utan kodändringar i bryggan.

### Implementerade mappningar

| RIVTA-tjänstekontrakt | FHIR-resurs | Status |
|---|---|---|
| `GetDiagnosis:2` | [Condition](StructureDefinition-se-ehds-condition.html) | Implementerad |
| `GetDocumentList:1` | [DocumentReference](StructureDefinition-se-ehds-document-reference.html) | Implementerad |

### Planerade mappningar

| RIVTA-tjänstekontrakt | FHIR-resurs | Status |
|---|---|---|
| `GetMedication:2` | MedicationStatement | Planerad |
| `GetObservation:1` | Observation | Planerad |
| `GetAllergyIntolerance:1` | AllergyIntolerance | Planerad |

## Dokumentation

- [Mappning: GetDiagnosis → Condition](mapping-getdiagnosis.html) – Detaljerad mappningstabell och exempel
- [Mappning: GetDocumentList → DocumentReference](mapping-getdocumentlist.html) – Fältmappning och OID-konverteringar
- [Arkitektur](architecture.html) – Systembeskrivning och flödesdiagram
- [OID-till-URI-mappningar](naming-systems.html) – Alla 12 OID↔URI-konverteringar som NamingSystem-resurser
- [Artefakter](artifacts.html) – Alla FHIR-profiler, kodsystem och valuemängder
- [Nedladdningar](downloads.html) – FHIR NPM-paket (`package.tgz`)

## Teknisk information

- **FHIR-version:** R4 (4.0.1)
- **Canonical URL:** `https://ehds-brygga.inera.se/fhir`
- **Utgivare:** Inera AB
- **Kontakt:** ehds@inera.se
- **Status:** Draft (ci-build)
