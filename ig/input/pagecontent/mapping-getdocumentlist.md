# Mappning: GetDocumentList → DocumentReference

## Tjänstekontrakt
**RIVTA-kontrakt:** `urn:riv:clinicalprocess:healthrecord:GetDocumentListResponder:1`  
**FHIR-resurstyp:** `DocumentReference` (profil: `se-ehds-document-reference`) + `Provenance`

## Fältmappning

| RIVTA-element | FHIR-element | Kommentar |
|---|---|---|
| `documentEntry.documentId` | `DocumentReference.masterIdentifier.value` | Unik dokumentidentifierare |
| `documentEntry.title` | `DocumentReference.description` | Dokumenttitel i klartext |
| `documentEntry.documentTime` | `DocumentReference.date` | ISO 8601 via parseRivDate |
| `documentEntry.typeCode.code` | `DocumentReference.type.coding.code` | Dokumenttypskod |
| `documentEntry.typeCode.codeSystem` | `DocumentReference.type.coding.system` | OID→URI via NamingSystemRegistry |
| `documentEntry.typeCode.displayName` | `DocumentReference.type.coding.display` | Kodverkets officiella benämning |
| `documentEntry.typeCode.originalText` | `DocumentReference.type.text` | Fritext från källsystemet; om saknad används `displayName` som fallback |
| `documentEntry.patientId` | `DocumentReference.subject.identifier` | Personnummer/samordningsnummer |
| `documentEntry.careUnitHSAId` | `DocumentReference.author[0].identifier` | Vårdenhet (informationsägare) |
| `documentEntry.careProviderHSAId` | `Provenance.agent[role=custodian].who.identifier` | Juridiskt ansvarig vårdgivare – bärs **enbart** i Provenance |
| `documentEntry.careUnitHSAId` | `Provenance.agent[role=author].who.identifier` | Vårdenhet i Provenance |
| `documentEntry.sourceSystemHSAId` | `DocumentReference.meta.source` | Källsystemets HSA-id som URI (urn:oid:{OID}#{hsaId}) |
| `statusCode == "active"` | `status = current` | Annars `superseded` |

## content.attachment – platshållare

`DocumentReference.content.attachment` sätts alltid med `contentType = application/pdf`
och `title` från dokumentets titel. **Ingen binär dokumentdata bäddas in.**

RIVTA GetDocumentList:1 är ett *registeranrop* — det returnerar metadata om dokument,
inte dokumentinnehållet. `attachment.data` och `attachment.url` lämnas tomma.
För att hämta det faktiska dokumentet krävs ett separat anrop till producenten
(t.ex. via GetDocument-kontraktet).

## Dokumenttypsmappning mot LOINC

`DocumentReference.type` ska enligt `SEEHDSDocumentReference`-profilen populeras med
en LOINC-kod från värdemängden c80-doc-typecodes (bindning: extensible).
RIVTA:s `typeCode` är en CVType vars kodsystem varierar per region och källsystem —
det krävs kartläggning av vilka koder som faktiskt förekommer i produktionsmiljö och
sedan en ConceptMap analogt med `DiagnosisTypeToCategoryMap`.

Tabellen nedan är ett preliminärt försök baserat på IHE XDS-praxis och svenska
journalsystems dokumentkategorier. **Den behöver valideras mot verkliga GetDocumentList-svar
innan den används i produktion.**

| Svensk anteckningstyp | LOINC-kod | LOINC-benämning |
|---|---|---|
| Epikris / Utskrivningsanteckning | 18842-5 | Discharge summary |
| Inskrivningsanteckning | 47039-3 | Inpatient admission history and physical note |
| Läkarbesöksanteckning (öppenvård) | 34108-1 | Outpatient note |
| Akutanteckning | 34878-9 | Emergency medicine note |
| Konsultationsvar / Remissvar | 11488-4 | Consult note |
| Remiss | 57133-1 | Referral note |
| Operationsberättelse | 11504-8 | Surgical operation note |
| Anestesijournal | 59775-7 | Procedure anesthesia note |
| Omvårdnadsanteckning | 28617-9 | Nursing note |
| Röntgenutlåtande / Radiologirapport | 18748-4 | Diagnostic imaging study |
| Patologisvar | 11526-1 | Pathology study |
| Psykiatrijournal | 11521-2 | Psychiatry note |
| Rehabiliteringsanteckning / Övrig anteckning | 11506-3 | Progress note |
| Läkemedelsberättelse | 56445-0 | Medication summary document |

Fram till dess att en verifierad ConceptMap finns passerar bryggan `typeCode` som
den anländer från RIVTA (OID konverteras till URI via NamingSystemRegistry) utan
LOINC-översättning. FHIR-klienter som kräver LOINC-koder behöver hantera detta
som en PoC-begränsning.

## EURIDICE/IPS-alignment
Profilen `SEEHDSDocumentReference` alignar med EU EHDS dokumentspecifikationer
och IHE-dokumentkategorier (LOINC c80-doc-typecodes).

## OID-URI-mappningar

Personnummer, samordningsnummer och HSA-id konverteras till kanoniska URI:er via `NamingSystemRegistry`.
Se [OID-till-URI-mappningar](naming-systems.html) för den kompletta tabellen.

De OID:er som förekommer i GetDocumentList:1-svar:

| OID | URI | Beskrivning |
|---|---|---|
| `1.2.752.129.2.1.3.1` | `http://electronichealth.se/identifier/personnummer` | Personnummer |
| `1.2.752.129.2.1.3.3` | `http://electronichealth.se/identifier/samordningsnummer` | Samordningsnummer |
| `1.2.752.129.2.1.4.1` | `urn:oid:1.2.752.129.2.1.4.1` | HSA-id (Inera NTjP) — author, meta.source, Provenance |
| `1.2.752.29.4.19` | `urn:oid:1.2.752.29.4.19` | HSA-id (HL7 Sweden basprofiler) |

## Provenance

Varje `DocumentReference` åtföljs av en `Provenance`-resurs som bär den organisatoriska
härkomsten. `careProviderHSAId` placeras **enbart** i Provenance och inte inne i
`DocumentReference`-resursen.

| Provenance-fält | Källa | Roll | Semantik |
|---|---|---|---|
| `agent[0].type` | – | `custodian` | Juridiskt ansvarig vårdgivare |
| `agent[0].who.identifier` | `careProviderHSAId` | – | HSA-id för vårdgivaren |
| `agent[1].type` | – | `author` | Informationsägare vårdenhet |
| `agent[1].who.identifier` | `careUnitHSAId` | – | HSA-id för vårdenheten |
| `agent[2].type` | – | `assembler` | EHDS-bryggan |
| `agent[2].who.identifier` | `bridgeHsaId` | – | Bryggans HSA-id |

## Spärr (Sparr)

Spärrkontrollen läser `careProviderHSAId` och `careUnitHSAId` från Provenance —
samma mönster som Condition-flödet via `SparrFilterService`.

| Fält | Källa | Syfte |
|---|---|---|
| `Provenance.agent[custodian].who` | `careProviderHSAId` | Yttre spärr (organisationsnivå) |
| `Provenance.agent[author].who` | `careUnitHSAId` | Inre spärr (avdelningsnivå) |
| `author[0].identifier` | `careUnitHSAId` | Synlig vårdenhet i resursen |

## PoC-begränsningar

### Binary document data
Binär dokumentdata hämtas inte; `attachment.data` och `attachment.url` är tomma.
