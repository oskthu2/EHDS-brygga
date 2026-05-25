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
| `documentEntry.sourceSystemHSAId` | `extension:ext-source-system` | Används för Spärr-filtrering |
| `statusCode == "active"` | `status = current` | Annars `superseded` |

## EURIDICE/IPS-alignment
Profilen `SEEHDSDocumentReference` alignar med EU EHDS dokumentspecifikationer
och IHE-dokumentkategorier (LOINC c80-doc-typecodes).
