# OID-till-URI-mappningar (NamingSystem)

RIVTA-tjänstekontrakt identifierar kodsystem och personidentifierare med OID:er (Object Identifiers).
FHIR föredrar URI:er. EHDS-bryggan utför konverteringen via `NamingSystemRegistry` (klass i `mapping-engine`)
som läser tabellen nedan från `naming-systems.yaml` vid uppstart.

Alla mappningar publiceras även som FHIR `NamingSystem`-resurser i denna IG och kan användas
som maskinläsbar källa för att implementera en egen OID↔URI-översättningstjänst.

## Konverteringsregler

1. Inkommande OID söks upp i tabellen; om träff returneras den registrerade URI:n.
2. Om ingen träff finns bevaras OID:n som `urn:oid:{oid}` (passthrough).
3. Konverteringen är **bidirektionell** — `NamingSystemRegistry.uriToOid()` söker i omvänd riktning.

## Patientidentifierare

Personnummer och samordningsnummer konverteras till kanoniska URI:er enligt
[HL7 Sweden basprofiler-r4](https://github.com/HL7Sweden/basprofiler-r4).

| OID | URI | FHIR NamingSystem |
|---|---|---|
| `1.2.752.129.2.1.3.1` | `http://electronichealth.se/identifier/personnummer` | [NsPersonnummer](NamingSystem-NsPersonnummer.html) |
| `1.2.752.129.2.1.3.3` | `http://electronichealth.se/identifier/samordningsnummer` | [NsSamordningsnummer](NamingSystem-NsSamordningsnummer.html) |

## Diagnoskodsystem

| OID | URI | Beskrivning | FHIR NamingSystem |
|---|---|---|---|
| `1.2.752.116.1.1.1.1.3` | `https://www.icd10.se/` | ICD-10-SE (Socialstyrelsen) | [NsIcd10Se](NamingSystem-NsIcd10Se.html) |
| `2.16.840.1.113883.6.3` | `http://hl7.org/fhir/sid/icd-10` | ICD-10 (internationell, WHO) | [NsIcd10International](NamingSystem-NsIcd10International.html) |
| `2.16.840.1.113883.6.96` | `http://snomed.info/sct` | SNOMED CT | [NsSnomedCt](NamingSystem-NsSnomedCt.html) |

Primärt diagnoskodsystem i RIVTA GetDiagnosis:2 är ICD-10-SE (`1.2.752.116.1.1.1.1.3`).
Om en producent skickar ett annat kodsystems OID tillämpas ovanstående tabell; okänd OID bevaras som `urn:oid:`.

## Åtgärdskodsystem

| OID | URI | Beskrivning | FHIR NamingSystem |
|---|---|---|---|
| `1.2.752.116.1.1.1.1.1` | `https://www.socialstyrelsen.se/statistik-och-data/klassifikationer-och-koder/kva/` | KVÅ (Klassifikation av vårdåtgärder) | [NsKva](NamingSystem-NsKva.html) |

## Vård- och omsorgsidentifierare (HSA)

HSA-id har ingen kanonisk http-URI registrerad i HL7 Sweden basprofiler-r4.
Bryggan bevarar dessa som `urn:oid:` och söker upp via de båda kända OID-varianterna.

| OID | URI (urn:oid) | Beskrivning | FHIR NamingSystem |
|---|---|---|---|
| `1.2.752.129.2.1.4.1` | `urn:oid:1.2.752.129.2.1.4.1` | HSA-id (Inera NTjP/RIVTA) | [NsHsaIdInera](NamingSystem-NsHsaIdInera.html) |
| `1.2.752.29.4.19` | `urn:oid:1.2.752.29.4.19` | HSA-id (HL7 Sweden basprofiler) | [NsHsaIdBasprofil](NamingSystem-NsHsaIdBasprofil.html) |

HSA-id förekommer i RIVTA-svar som `sourceSystemHSAId`, `careProviderHSAId` och `careUnitHSAId`
och bärs vidare till FHIR-resurser via `Condition.recorder`, `Condition.extension[sourceSystem]`,
`Condition.extension[careProvider]`, `Condition.extension[careUnit]`, `DocumentReference.author`,
`DocumentReference.custodian` och `DocumentReference.extension[careProvider]`.

## Professionella identifierare

Dessa OID:er förekommer i RIVTA-svar när producenten identifierar vård- och behandlingsansvariga.
De saknar kanoniska http-URI:er och bevaras som `urn:oid:`.

| OID | URI (urn:oid) | Beskrivning | FHIR NamingSystem |
|---|---|---|---|
| `1.2.752.116.3.1.1` | `urn:oid:1.2.752.116.3.1.1` | Legitimationsnummer (Socialstyrelsen) | [NsLegitimationsnummer](NamingSystem-NsLegitimationsnummer.html) |
| `1.2.752.116.3.1.2` | `urn:oid:1.2.752.116.3.1.2` | Förskrivarkod (Socialstyrelsen) | [NsForskrivarkod](NamingSystem-NsForskrivarkod.html) |
| `1.2.752.116.3.1.3` | `urn:oid:1.2.752.116.3.1.3` | HOSP-yrken (Socialstyrelsen) | [NsHospYrken](NamingSystem-NsHospYrken.html) |
| `1.2.752.116.1.3.6` | `urn:oid:1.2.752.116.1.3.6` | SOSNYK yrkeskategorier (Socialstyrelsen) | [NsSosnyk](NamingSystem-NsSosnyk.html) |

## Fallback-beteende

OID:er som inte finns i tabellen ovan bevaras oförändrade som `urn:oid:{oid}`.
Detta säkerställer att bryggan aldrig tappar identifierarinformation även om en ny OID dyker upp.

## Källfil

Den auktoritativa källan för bryggornas NamingSystem-konfiguration är
`bridge/mapping-engine/src/main/resources/naming-systems.yaml`.
FHIR NamingSystem-resurserna i denna IG genereras ur samma tabell och är alltid i synk med koden.
