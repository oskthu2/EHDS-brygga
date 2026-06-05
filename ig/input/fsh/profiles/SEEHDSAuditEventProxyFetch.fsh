Profile: SEEHDSAuditEventProxyFetch
Parent: AuditEvent
Id: se-ehds-audit-event-proxy-fetch
Title: "SE EHDS AuditEvent – proxy-hämtning och -konvertering"
Description: """
Audit-händelse som loggas av **ntjp-proxy** efter att en SOAP/RIVTA-fråga skickats till NTjP
och svaret konverterats till FHIR-resurser.

Händelsen fångar:
- Vilket system som begärde hämtningen (fhir-server, identifierat med HSA-id)
- Vilken NTjP-endpoint och logisk adress (VG HSA-id) som anropades
- Vilken FHIR-resurstyp och patientidentitet som efterfrågades
- Hur många resurser som konverterades

Producenter i NTjP loggar sina egna SOAP-transaktioner separat i nationell logg.

**Loggningspunkt:** direkt efter lyckad SOAP-svar och FHIR-konvertering i ntjp-proxy.
"""

* type = DCM#110112 "Query"
* type MS
* action = #R
* action MS
* recorded 1..1 MS
* outcome 1..1 MS
* outcome ^short = "0 = framgång, 8 = SOAP-anrop misslyckades"

// Subtype
* subtype ^slicing.discriminator[0].type = #value
* subtype ^slicing.discriminator[0].path = "$this"
* subtype ^slicing.rules = #open
* subtype contains fetchSubtype 1..1 MS
* subtype[fetchSubtype] = SEEHDSAuditSubtypeCS#proxy-fetch "Proxy Fetch & Convert"

// Agents
* agent ^slicing.discriminator[0].type = #value
* agent ^slicing.discriminator[0].path = "requestor"
* agent ^slicing.rules = #open
* agent contains
    caller 1..1 MS and
    proxy 1..1 MS

* agent[caller] ^short = "fhir-server – systemet som anropade ntjp-proxy"
* agent[caller].requestor = true
* agent[caller].who 1..1 MS
* agent[caller].who ^short = "fhir-server identifierad med bridgeHsaId"
* agent[caller].type 1..1 MS
* agent[caller].type = DCM#110152 "Destination Role ID"

* agent[proxy] ^short = "ntjp-proxy – systemet som genomförde SOAP-anropet"
* agent[proxy].requestor = false
* agent[proxy].who 1..1 MS
* agent[proxy].who ^short = "ntjp-proxy identifierad med bridgeHsaId + ':ntjp-proxy'"
* agent[proxy].type 1..1 MS
* agent[proxy].type = DCM#110153 "Source Role ID"
* agent[proxy].network 1..1 MS
* agent[proxy].network.address 1..1 MS
* agent[proxy].network.address ^short = "NTjP-endpoint URL (t.ex. http://ntjp-proxy:8091)"

// Source: ntjp-proxy-instansen
* source.observer 1..1 MS

// Entities
* entity ^slicing.discriminator[0].type = #value
* entity ^slicing.discriminator[0].path = "role"
* entity ^slicing.rules = #open
* entity contains
    patient 1..1 MS and
    query 1..1 MS

* entity[patient] ^short = "Patienten vars data hämtades"
* entity[patient].role = ObjectRole#1 "Patient"
* entity[patient].type = AuditEntityType#1 "Person"
* entity[patient].what 1..1 MS

* entity[query] ^short = "FHIR-resurstyp, logisk adress (VG HSA-id) och resultCount"
* entity[query].role = ObjectRole#24 "Query"
* entity[query].type = AuditEntityType#2 "System Object"
* entity[query].query 1..1 MS
* entity[query].query ^short = "Base64: resourceType?patient.identifier=system|value&logicalAddress=hsaId (resultCount=N)"
