Profile: SEEHDSAuditEventSparrFilter
Parent: AuditEvent
Id: se-ehds-audit-event-sparr-filter
Title: "SE EHDS AuditEvent – spärrtillämpning"
Description: """
Audit-händelse som loggas av **fhir-server** när post-query-spärrar tillämpas på svarsdata.

Händelsen fångar:
- Hur många poster som skickades vidare respektive filtrerades bort
- Vilken patient spärrkontrollen gällde
- URL:en till spärrtjänsten som konsulterades

Sparrtjänsten kontrollerar yttre spärr (`careProviderHSAId`, organisationsnivå).
Fail-closed: om spärrtjänsten ej nås filtreras posten bort och `outcome = 4` (minor failure).

**Loggningspunkt:** direkt efter `SparrFilterService.filterConditions/filterDocumentReferences`.
"""

* type = DCM#110112 "Query"
* type MS
* action = #R
* action MS
* recorded 1..1 MS
* outcome 1..1 MS
* outcome ^short = "0 = framgång (sparrtjänsten svarade), 4 = spärrtjänsten nåddes ej (fail-closed)"

// Subtype
* subtype ^slicing.discriminator[0].type = #value
* subtype ^slicing.discriminator[0].path = "$this"
* subtype ^slicing.rules = #open
* subtype contains filterSubtype 1..1 MS
* subtype[filterSubtype] = SEEHDSAuditSubtypeCS#sparr-filter "Spärr Filter Applied"

// Agents
* agent ^slicing.discriminator[0].type = #value
* agent ^slicing.discriminator[0].path = "requestor"
* agent ^slicing.rules = #open
* agent contains
    consumer 1..1 MS and
    bridge 1..1 MS

* agent[consumer] ^short = "Konsumentsystemet vars anrop utlöste spärrkontrollen"
* agent[consumer].requestor = true
* agent[consumer].who 1..1 MS
* agent[consumer].type 1..1 MS
* agent[consumer].type = DCM#110152 "Destination Role ID"

* agent[bridge] ^short = "fhir-server – den aktör som tillämpade spärrarna"
* agent[bridge].requestor = false
* agent[bridge].who 1..1 MS
* agent[bridge].type 1..1 MS
* agent[bridge].type = DCM#110153 "Source Role ID"

// Source
* source.observer 1..1 MS

// Entities
* entity ^slicing.discriminator[0].type = #value
* entity ^slicing.discriminator[0].path = "role"
* entity ^slicing.rules = #open
* entity contains
    patient 1..1 MS and
    filterResult 1..1 MS

* entity[patient] ^short = "Patienten vars data spärrfiltrerades"
* entity[patient].role = ObjectRole#1 "Patient"
* entity[patient].type = AuditEntityType#1 "Person"
* entity[patient].what 1..1 MS

* entity[filterResult] ^short = "Antal poster efter filtrering och URL till spärrtjänst"
* entity[filterResult].role = ObjectRole#13 "Security Granule"
* entity[filterResult].type = AuditEntityType#2 "System Object"
* entity[filterResult].detail ^slicing.discriminator[0].type = #value
* entity[filterResult].detail ^slicing.discriminator[0].path = "type"
* entity[filterResult].detail ^slicing.rules = #open
* entity[filterResult].detail contains resultCount 1..1 MS
* entity[filterResult].detail[resultCount].type = "resultCount"
* entity[filterResult].detail[resultCount].value[x] only string
* entity[filterResult].detail[resultCount] ^short = "Antal poster som passerade spärren (som sträng, t.ex. '12')"
