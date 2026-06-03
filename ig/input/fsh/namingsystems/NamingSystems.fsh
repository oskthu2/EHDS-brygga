// ============================================================
// Patientidentifierare (HL7 Sweden basprofiler-r4)
// ============================================================

Instance: NsPersonnummer
InstanceOf: NamingSystem
Usage: #definition
Title: "Personnummer"
Description: "Nationellt personnummer (OID 1.2.752.129.2.1.3.1) utfärdat av Skatteverket. Kanonisk URI enligt HL7 Sweden basprofiler-r4."
* name = "NsPersonnummer"
* status = #active
* kind = #identifier
* date = "2024-01-01"
* publisher = "Inera AB"
* contact.name = "Inera AB"
* contact.telecom.system = #url
* contact.telecom.value = "https://www.inera.se"
* jurisdiction = urn:iso:std:iso:3166#SE "Sweden"
* uniqueId[+].type = #oid
* uniqueId[=].value = "1.2.752.129.2.1.3.1"
* uniqueId[+].type = #uri
* uniqueId[=].value = "http://electronichealth.se/identifier/personnummer"
* uniqueId[=].preferred = true

Instance: NsSamordningsnummer
InstanceOf: NamingSystem
Usage: #definition
Title: "Samordningsnummer"
Description: "Samordningsnummer (OID 1.2.752.129.2.1.3.3) utfärdat av Skatteverket. Kanonisk URI enligt HL7 Sweden basprofiler-r4."
* name = "NsSamordningsnummer"
* status = #active
* kind = #identifier
* date = "2024-01-01"
* publisher = "Inera AB"
* contact.name = "Inera AB"
* contact.telecom.system = #url
* contact.telecom.value = "https://www.inera.se"
* jurisdiction = urn:iso:std:iso:3166#SE "Sweden"
* uniqueId[+].type = #oid
* uniqueId[=].value = "1.2.752.129.2.1.3.3"
* uniqueId[+].type = #uri
* uniqueId[=].value = "http://electronichealth.se/identifier/samordningsnummer"
* uniqueId[=].preferred = true

// ============================================================
// Diagnoskodsystem
// ============================================================

Instance: NsIcd10Se
InstanceOf: NamingSystem
Usage: #definition
Title: "ICD-10-SE"
Description: "ICD-10-SE (OID 1.2.752.116.1.1.1.1.3) – det svenska tillägget till ICD-10, utgivet av Socialstyrelsen. Primärt diagnoskodsystem i RIVTA GetDiagnosis:2."
* name = "NsIcd10Se"
* status = #active
* kind = #codesystem
* date = "2024-01-01"
* publisher = "Inera AB"
* contact.name = "Inera AB"
* contact.telecom.system = #url
* contact.telecom.value = "https://www.inera.se"
* jurisdiction = urn:iso:std:iso:3166#SE "Sweden"
* uniqueId[+].type = #oid
* uniqueId[=].value = "1.2.752.116.1.1.1.1.3"
* uniqueId[+].type = #uri
* uniqueId[=].value = "https://www.icd10.se/"
* uniqueId[=].preferred = true

Instance: NsIcd10International
InstanceOf: NamingSystem
Usage: #definition
Title: "ICD-10 (International)"
Description: "ICD-10 internationell version (OID 2.16.840.1.113883.6.3) utgiven av WHO. Används när diagnoskodsystemets OID inte är det svenska (1.2.752.116.1.1.1.1.3)."
* name = "NsIcd10International"
* status = #active
* kind = #codesystem
* date = "2024-01-01"
* publisher = "Inera AB"
* contact.name = "Inera AB"
* contact.telecom.system = #url
* contact.telecom.value = "https://www.inera.se"
* uniqueId[+].type = #oid
* uniqueId[=].value = "2.16.840.1.113883.6.3"
* uniqueId[+].type = #uri
* uniqueId[=].value = "http://hl7.org/fhir/sid/icd-10"
* uniqueId[=].preferred = true

Instance: NsSnomedCt
InstanceOf: NamingSystem
Usage: #definition
Title: "SNOMED CT"
Description: "SNOMED Clinical Terms (OID 2.16.840.1.113883.6.96). Kan förekomma som kodsystem i RIVTA-svar om producenten kodar med SNOMED CT."
* name = "NsSnomedCt"
* status = #active
* kind = #codesystem
* date = "2024-01-01"
* publisher = "Inera AB"
* contact.name = "Inera AB"
* contact.telecom.system = #url
* contact.telecom.value = "https://www.inera.se"
* uniqueId[+].type = #oid
* uniqueId[=].value = "2.16.840.1.113883.6.96"
* uniqueId[+].type = #uri
* uniqueId[=].value = "http://snomed.info/sct"
* uniqueId[=].preferred = true

// ============================================================
// Åtgärdskodsystem
// ============================================================

Instance: NsKva
InstanceOf: NamingSystem
Usage: #definition
Title: "KVÅ – Klassifikation av vårdåtgärder"
Description: "KVÅ (OID 1.2.752.116.1.1.1.1.1) – Socialstyrelsens klassifikation av åtgärder. Kan förekomma i RIVTA-svar som kompletterande kodsystem."
* name = "NsKva"
* status = #active
* kind = #codesystem
* date = "2024-01-01"
* publisher = "Inera AB"
* contact.name = "Inera AB"
* contact.telecom.system = #url
* contact.telecom.value = "https://www.inera.se"
* jurisdiction = urn:iso:std:iso:3166#SE "Sweden"
* uniqueId[+].type = #oid
* uniqueId[=].value = "1.2.752.116.1.1.1.1.1"
* uniqueId[+].type = #uri
* uniqueId[=].value = "https://www.socialstyrelsen.se/statistik-och-data/klassifikationer-och-koder/kva/"
* uniqueId[=].preferred = true

// ============================================================
// Vård- och omsorgsidentifierare (HSA)
// ============================================================

Instance: NsHsaIdInera
InstanceOf: NamingSystem
Usage: #definition
Title: "HSA-id (Inera NTjP/RIVTA)"
Description: "HSA-identifierare (OID 1.2.752.129.2.1.4.1) i Ineras RIVTA-profil och NTjP. Används för sourceSystemHSAId, careProviderHSAId och careUnitHSAId i RIVTA-svar. Bevaras som urn:oid: eftersom ingen kanonisk URI finns."
* name = "NsHsaIdInera"
* status = #active
* kind = #identifier
* date = "2024-01-01"
* publisher = "Inera AB"
* contact.name = "Inera AB"
* contact.telecom.system = #url
* contact.telecom.value = "https://www.inera.se"
* jurisdiction = urn:iso:std:iso:3166#SE "Sweden"
* uniqueId[+].type = #oid
* uniqueId[=].value = "1.2.752.129.2.1.4.1"
* uniqueId[+].type = #uri
* uniqueId[=].value = "urn:oid:1.2.752.129.2.1.4.1"
* uniqueId[=].preferred = true

Instance: NsHsaIdBasprofil
InstanceOf: NamingSystem
Usage: #definition
Title: "HSA-id (HL7 Sweden basprofiler)"
Description: "HSA-identifierare (OID 1.2.752.29.4.19) i HL7 Sweden basprofiler-r4. Alternativ OID för samma identifierarespace som 1.2.752.129.2.1.4.1. Bevaras som urn:oid: eftersom ingen kanonisk URI finns."
* name = "NsHsaIdBasprofil"
* status = #active
* kind = #identifier
* date = "2024-01-01"
* publisher = "Inera AB"
* contact.name = "Inera AB"
* contact.telecom.system = #url
* contact.telecom.value = "https://www.inera.se"
* jurisdiction = urn:iso:std:iso:3166#SE "Sweden"
* uniqueId[+].type = #oid
* uniqueId[=].value = "1.2.752.29.4.19"
* uniqueId[+].type = #uri
* uniqueId[=].value = "urn:oid:1.2.752.29.4.19"
* uniqueId[=].preferred = true

// ============================================================
// Professionella identifierare (HL7 Sweden basprofiler-r4)
// ============================================================

Instance: NsLegitimationsnummer
InstanceOf: NamingSystem
Usage: #definition
Title: "Legitimationsnummer (Socialstyrelsen)"
Description: "Legitimationsnummer (OID 1.2.752.116.3.1.1) utfärdat av Socialstyrelsen för legitimerade yrkesgrupper inom hälso- och sjukvård. Förekommer i Provenance-resurser för vård- och behandlingsansvariga."
* name = "NsLegitimationsnummer"
* status = #active
* kind = #identifier
* date = "2024-01-01"
* publisher = "Inera AB"
* contact.name = "Inera AB"
* contact.telecom.system = #url
* contact.telecom.value = "https://www.inera.se"
* jurisdiction = urn:iso:std:iso:3166#SE "Sweden"
* uniqueId[+].type = #oid
* uniqueId[=].value = "1.2.752.116.3.1.1"
* uniqueId[+].type = #uri
* uniqueId[=].value = "urn:oid:1.2.752.116.3.1.1"
* uniqueId[=].preferred = true

Instance: NsForskrivarkod
InstanceOf: NamingSystem
Usage: #definition
Title: "Förskrivarkod (Socialstyrelsen)"
Description: "Förskrivarkod (OID 1.2.752.116.3.1.2) utfärdat av Socialstyrelsen. Identifierar förskrivare av läkemedel. Förekommer i Provenance-resurser."
* name = "NsForskrivarkod"
* status = #active
* kind = #identifier
* date = "2024-01-01"
* publisher = "Inera AB"
* contact.name = "Inera AB"
* contact.telecom.system = #url
* contact.telecom.value = "https://www.inera.se"
* jurisdiction = urn:iso:std:iso:3166#SE "Sweden"
* uniqueId[+].type = #oid
* uniqueId[=].value = "1.2.752.116.3.1.2"
* uniqueId[+].type = #uri
* uniqueId[=].value = "urn:oid:1.2.752.116.3.1.2"
* uniqueId[=].preferred = true

Instance: NsHospYrken
InstanceOf: NamingSystem
Usage: #definition
Title: "HOSP-yrken (Socialstyrelsen)"
Description: "Klassifikation av yrkeskategorier i hälso- och sjukvård (OID 1.2.752.116.3.1.3) från Socialstyrelsens HOSP-register. Förekommer i RIVTA-svar som professionskod."
* name = "NsHospYrken"
* status = #active
* kind = #codesystem
* date = "2024-01-01"
* publisher = "Inera AB"
* contact.name = "Inera AB"
* contact.telecom.system = #url
* contact.telecom.value = "https://www.inera.se"
* jurisdiction = urn:iso:std:iso:3166#SE "Sweden"
* uniqueId[+].type = #oid
* uniqueId[=].value = "1.2.752.116.3.1.3"
* uniqueId[+].type = #uri
* uniqueId[=].value = "urn:oid:1.2.752.116.3.1.3"
* uniqueId[=].preferred = true

Instance: NsSosnyk
InstanceOf: NamingSystem
Usage: #definition
Title: "SOSNYK – yrkeskategorier (Socialstyrelsen)"
Description: "SOSNYK – Socialstyrelsens klassifikation av yrkeskategorier (OID 1.2.752.116.1.3.6). Förekommer i RIVTA-svar som professionskod och i HSA-katalogens yrkesklassificering."
* name = "NsSosnyk"
* status = #active
* kind = #codesystem
* date = "2024-01-01"
* publisher = "Inera AB"
* contact.name = "Inera AB"
* contact.telecom.system = #url
* contact.telecom.value = "https://www.inera.se"
* jurisdiction = urn:iso:std:iso:3166#SE "Sweden"
* uniqueId[+].type = #oid
* uniqueId[=].value = "1.2.752.116.1.3.6"
* uniqueId[+].type = #uri
* uniqueId[=].value = "urn:oid:1.2.752.116.1.3.6"
* uniqueId[=].preferred = true
