Profile: SEEHDSAuditEventEhmAccess
Parent: AuditEvent
Id: se-ehds-audit-event-ehm-access
Title: "SE EHDS AuditEvent – eHM-åtkomst"
Description: """
Audit-händelse som loggas av **fhir-server** när en konsument (eHM:s åtkomsttjänst eller
annat SMART-klientssystem) anropar EHDS-bryggan med en patientbunden fråga.

## SMART-kontext och vad som är tillgängligt

Anrop görs med ett **SMART Bearer-token** (JWT). Utan att validera signaturen kan bryggan
base64-avkoda JWT-payload och extrahera följande claims:

| JWT-claim | Källa | Mappas till |
|---|---|---|
| `client_id` / `azp` | SMART backend services | `agent[system].who` — eHM-applikationens identitet |
| `fhirUser` | SMART EHR Launch | `agent[user].who` — inloggad vårdpersonal (Practitioner-referens) |
| `sub` (när ≠ `client_id`) | SMART App Launch | `agent[user].who` — alternativ användarkälla |
| `purpose_of_use` | SE-specifik SITHS-extension | `purposeOfEvent` — ändamål med åtkomsten |
| `scope` | SMART standard | `purposeOfEvent` (extrakt: `patient/*` → TREAT) |

### Två scenarier

**Rent system-till-system (SMART Backend Services):** Tokenet saknar `fhirUser`/`sub` som
skiljer sig från `client_id`. `agent[user]` utelämnas. `agent[system].requestor = true`.

**Med användarkontext (SMART App Launch / EHR Launch):** Tokenet innehåller `fhirUser`
eller en `sub` som identifierar en specifik vårdpersonal. Då är `agent[user].requestor = true`
och `agent[system].requestor = false` (applikationen agerar på uppdrag av användaren).

### purposeOfEvent

Populeras från `purpose_of_use`-claim (SE-extension, t.ex. `TREAT`, `ETREAT`) eller
utläsas ur `scope`-fragmentet om `purpose_of_use` saknas. Kan vara frånvarande i
tokens utan explicit ändamålsangivelse — bryggan loggar vad som finns tillgängligt.

### Nuläge

JWT-signaturen valideras ännu inte i bryggan (gateway-planerat). Claimuttolkning sker
enbart i loggningssyfte — tilliten skapas av att gatewayen validerade tokenet.
"""

* type = DCM#110112 "Query"
* type MS
* action = #R
* action MS
* recorded 1..1 MS
* outcome 1..1 MS

* purposeOfEvent 0..* MS
* purposeOfEvent from http://terminology.hl7.org/ValueSet/v3-PurposeOfUse (preferred)
* purposeOfEvent ^short = "Ändamål: TREAT (vård och behandling), ETREAT (nödsituation) – från purpose_of_use-claim eller SMART scope"

// Subtype
* subtype ^slicing.discriminator[0].type = #value
* subtype ^slicing.discriminator[0].path = "$this"
* subtype ^slicing.rules = #open
* subtype contains accessSubtype 1..1 MS
* subtype[accessSubtype] = SEEHDSAuditSubtypeCS#ehm-access "eHM Access"

// Agent slicing on type pattern to support both system-only and system+user scenarios
* agent ^slicing.discriminator[0].type = #pattern
* agent ^slicing.discriminator[0].path = "type"
* agent ^slicing.rules = #open
* agent contains
    system 1..1 MS and
    user 0..1 MS and
    bridge 1..1 MS

* agent[system] ^short = "eHM-applikationen – identifierad via client_id/azp-claim"
* agent[system] ^definition = """
  Representerar det anropande systemet (eHM-applikationen).
  requestor = true när enbart systemtoken utan användarkontext.
  requestor = false när agent[user] är present (systemet agerar å användarens vägnar).
  """
* agent[system].type 1..1 MS
* agent[system].type = DCM#110150 "Application"
* agent[system].who 1..1 MS
* agent[system].who ^short = "client_id eller azp från JWT"
* agent[system].requestor 1..1 MS

* agent[user] ^short = "Inloggad vårdpersonal – fhirUser eller sub (när skild från client_id)"
* agent[user] ^definition = """
  Representerar den mänskliga initiativtagaren. Sätts när JWT innehåller fhirUser-claim
  (SMART EHR Launch) eller en sub som identifierar en specifik person och inte är
  identisk med client_id.
  requestor = true: användaren är den faktiska initiativtagaren.
  """
* agent[user].type 1..1 MS
* agent[user].type = ExtraSecurityRoleType#humanuser "Human User"
* agent[user].who 1..1 MS
* agent[user].who ^short = "Practitioner-referens (fhirUser) eller subject-identifier (sub)"
* agent[user].requestor = true

* agent[bridge] ^short = "EHDS-bryggan (fhir-server) – svarar på förfrågan"
* agent[bridge].type 1..1 MS
* agent[bridge].type = DCM#110153 "Source Role ID"
* agent[bridge].who 1..1 MS
* agent[bridge].who ^short = "bridgeHsaId (SE2321000999-EHDS eller konfigurerat värde)"
* agent[bridge].requestor = false

// Source
* source.observer 1..1 MS

// Entities
* entity ^slicing.discriminator[0].type = #value
* entity ^slicing.discriminator[0].path = "role"
* entity ^slicing.rules = #open
* entity contains
    patient 1..1 MS and
    query 1..1 MS

* entity[patient] ^short = "Patienten vars data efterfrågades"
* entity[patient].role = ObjectRole#1 "Patient"
* entity[patient].type = AuditEntityType#1 "Person"
* entity[patient].what 1..1 MS

* entity[query] ^short = "Frågeparametrar: resurstyp, VG HSA-id, resultCount"
* entity[query].role = ObjectRole#24 "Query"
* entity[query].type = AuditEntityType#2 "System Object"
* entity[query].query 1..1 MS
