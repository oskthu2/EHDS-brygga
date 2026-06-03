# Mappning: GetDocumentList → DocumentReference

## Tjänstekontrakt
**RIVTA-kontrakt:** `urn:riv:clinicalprocess:healthrecord:GetDocumentListResponder:1`  
**FHIR-resurstyp:** `DocumentReference` (profil: `se-ehds-document-reference`)

## Fältmappning

| RIVTA-element | FHIR-element | Kommentar |
|---|---|---|
| `documentEntry.documentId` | `DocumentReference.masterIdentifier.value` | Unik dokumentidentifierare |
| `documentEntry.title` | `DocumentReference.description` | Dokumenttitel i klartext |
| `documentEntry.documentTime` | `DocumentReference.date` | ISO 8601 via parseRivDate |
| `documentEntry.typeCode` (CVType) | `DocumentReference.type` | OID→URI via NamingSystemRegistry |
| `documentEntry.patientId` | `DocumentReference.subject.identifier` | Personnummer/samordningsnummer |
| `documentEntry.careUnitHSAId` | `DocumentReference.author[0].identifier` | HSA-id vårdenhet |
| `documentEntry.careProviderHSAId` | `DocumentReference.custodian.identifier` | HSA-id vårdgivare |
| `documentEntry.sourceSystemHSAId` | `extension:ext-source-system` | Källsystemets HSA-id |
| `statusCode == "active"` | `status = current` | Annars `superseded` |

## content.attachment – platshållare

`DocumentReference.content.attachment` sätts alltid med `contentType = application/pdf`
och `title` från dokumentets titel. **Ingen binär dokumentdata bäddas in.**

RIVTA GetDocumentList:1 är ett *registeranrop* — det returnerar metadata om dokument,
inte dokumentinnehållet. `attachment.data` och `attachment.url` lämnas tomma.
För att hämta det faktiska dokumentet krävs ett separat anrop till producenten
(t.ex. via GetDocument-kontraktet).

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
| `1.2.752.129.2.1.4.1` | `urn:oid:1.2.752.129.2.1.4.1` | HSA-id (Inera NTjP) — author, custodian, ext-care-provider |
| `1.2.752.29.4.19` | `urn:oid:1.2.752.29.4.19` | HSA-id (HL7 Sweden basprofiler) |

## Spärr (Sparr)

`GetDocumentListMapper` sätter `ext-care-provider` med `careProviderHSAId`.
`DocumentQueryOrchestrator` läser denna extension och utför spärrkontroll på
organisationsnivå – samma mönster som Condition-flödet via `SparrFilterService`.

| Fält | Källa | Syfte |
|---|---|---|
| `extension[ext-care-provider]` | `careProviderHSAId` | Används av spärrfiltret |
| `custodian.identifier` | `careProviderHSAId` | Synlig i FHIR-resursen |
| `author[0].identifier` | `careUnitHSAId` | Vårdenhet |

## PoC-begränsningar

### Provenance

Inga Provenance-resurser skapas för DocumentReference-poster. Hela Provenance-kedjan
(vårdgivare → vårdenhet → brygga) som beskrivs för Condition-flödet saknas här.
Detta är en känd PoC-begränsning.
