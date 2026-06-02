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

## EURIDICE/IPS-alignment
Profilen `SEEHDSDocumentReference` alignar med EU EHDS dokumentspecifikationer
och IHE-dokumentkategorier (LOINC c80-doc-typecodes).

## OID-URI-mappningar

Personnummer och samordningsnummer konverteras till kanoniska URI:er enligt
[HL7 Sweden basprofiler-r4](https://github.com/HL7Sweden/basprofiler-r4):

| OID | URI |
|---|---|
| `1.2.752.129.2.1.3.1` | `http://electronichealth.se/identifier/personnummer` |
| `1.2.752.129.2.1.3.3` | `http://electronichealth.se/identifier/samordningsnummer` |

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
