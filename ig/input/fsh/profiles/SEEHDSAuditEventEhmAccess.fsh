Profile: SEEHDSAuditEventEhmAccess
Parent: AuditEvent
Id: se-ehds-audit-event-ehm-access
Title: "SE EHDS AuditEvent – eHM-åtkomst"
Description: """
Audit-händelse som loggas av **fhir-server** när en konsument (t.ex. eHM:s åtkomsttjänst)
anropar EHDS-bryggan med en patientbunden fråga.

Händelsen fångar:
- Vem som frågade (konsumentsystemet, identifierat via JWT/OAuth-klient eller HSA-id)
- Vilket patientidentitetssystem och -värde som söktes
- Vilken VG och resurstyp som efterfrågades
- Hur stort svar som returnerades (resultCount i `entity[query].detail`)

**Loggningspunkt:** direkt efter att `Bundle` levererats till konsumenten, fire-and-forget.
"""

* type = DCM#110112 "Query"
* type MS
* action = #R
* action MS
* recorded 1..1 MS
* outcome 1..1 MS

// Subtype: identifierar händelsetypen
* subtype ^slicing.discriminator[0].type = #value
* subtype ^slicing.discriminator[0].path = "$this"
* subtype ^slicing.rules = #open
* subtype contains accessSubtype 1..1 MS
* subtype[accessSubtype] = SEEHDSAuditSubtypeCS#ehm-access "eHM Access"

// Agents
* agent ^slicing.discriminator[0].type = #value
* agent ^slicing.discriminator[0].path = "requestor"
* agent ^slicing.rules = #open
* agent contains
    consumer 1..1 MS and
    bridge 1..1 MS

* agent[consumer] ^short = "Konsumentsystemet (t.ex. eHM-portalen)"
* agent[consumer].requestor = true
* agent[consumer].who 1..1 MS
* agent[consumer].who only Reference(Device or Organization or Practitioner)
* agent[consumer].type 1..1 MS
* agent[consumer].type = DCM#110152 "Destination Role ID"

* agent[bridge] ^short = "EHDS-bryggan (fhir-server, identifierad med bridgeHsaId)"
* agent[bridge].requestor = false
* agent[bridge].who 1..1 MS
* agent[bridge].type 1..1 MS
* agent[bridge].type = DCM#110153 "Source Role ID"

// Source: fhir-server-instansen
* source.observer 1..1 MS
* source.observer ^short = "fhir-server-instansen (bridgeHsaId)"

// Entities
* entity ^slicing.discriminator[0].type = #value
* entity ^slicing.discriminator[0].path = "role"
* entity ^slicing.rules = #open
* entity contains
    patient 1..1 MS and
    query 1..1 MS

* entity[patient] ^short = "Patienten som data hämtades för"
* entity[patient].role = ObjectRole#1 "Patient"
* entity[patient].type = AuditEntityType#1 "Person"
* entity[patient].what 1..1 MS
* entity[patient].what ^short = "Patient.identifier med personnummer/samordningsnummer"

* entity[query] ^short = "Frågeparametrar: resurstyp, VG-HSA-id, resultCount"
* entity[query].role = ObjectRole#24 "Query"
* entity[query].type = AuditEntityType#2 "System Object"
* entity[query].query 1..1 MS
* entity[query].query ^short = "Base64-kodat: resourceType?patient.identifier=system|value&vg=hsaId (resultCount=N)"
