CodeSystem: SEEHDSAuditEventSubtype
Id: se-ehds-audit-event-subtype
Title: "SE EHDS AuditEvent – händelsesubtyper"
Description: "Subtypkoder för EHDS-bryggans BALP-liknande audit-händelser. En kod per loggningspunkt i pipelinen."

* ^url = "https://ehds-brygga.inera.se/fhir/CodeSystem/audit-event-subtype"
* ^status = #active
* ^caseSensitive = true

* #proxy-fetch "Proxy Fetch & Convert"
    "ntjp-proxy hämtade patientdata via SOAP/RIVTA från NTjP och konverterade svaret till FHIR-resurser."
* #sparr-filter "Spärr Filter Applied"
    "fhir-server tillämpade post-query-spärrar och tog bort förbjudna poster ur svaret."
* #ehm-access "eHM Access"
    "eHM:s åtkomsttjänst (eller annan konsument) anropade EHDS-bryggan och fick ett Bundle-svar."
