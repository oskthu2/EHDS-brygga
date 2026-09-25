Profile: SEEHDSDocumentReference
Parent: DocumentReference
Id: se-ehds-document-reference
Title: "SE EHDS DocumentReference"
Description: """
FHIR DocumentReference-profil för EHDS-bryggan.
Mappas från Ineras RIVTA-tjänstekontrakt GetCareDocumentation
(clinicalprocess:healthcond:description:GetCareDocumentationResponder:3, JoL-header v2.2).

Profilen alignar med EURIDICE/EHDS EU-specifikationer för kliniska dokument
och säkerställer att:
- Dokumenttyp (clinicalDocumentNoteCode, se värdegrupp i mapping-getcaredocumentation.html) är angiven
- Patient är identifierad med personnummer eller samordningsnummer
- Källsystem identifieras via meta.source (urn:oid:{HSA_OID}#{hsaId})
- Dokumentets primärnyckel bärs av masterIdentifier (record.recordId)
- Dokumentationsansvarig är angiven som author, signerare som authenticator
  (logiska referenser mot PractitionerRole via HSA-id)
- Ansvarig vårdgivare och vårdenhet bärs av Provenance.agent[role=custodian/author]
  (inte inne i resursen) — läses direkt från accessControlHeader, se DES-005

Innehållet (content.attachment) är ett XOR mellan fritext (clinicalDocumentNoteText,
kodat som text/plain) och binärdata/URL (multimediaEntry) på RIVTA-sidan — se
"Öppna frågor" i mapping-getcaredocumentation.html för invarianterna
getcaredocumentation-body-xor och getcaredocumentation-multimedia-xor, som gäller det
inkommande RIVTA-svaret snarare än den mappade FHIR-resursen (DocumentReference har
inga separata element för de två grenarna — båda mynnar ut i content.attachment).
"""

* ^url = "https://ehds-brygga.inera.se/fhir/StructureDefinition/se-ehds-document-reference"

* meta.source MS
* meta.source ^short = "HSA-id för källsystemet, format: urn:oid:1.2.752.129.2.1.4.1#{hsaId}"

* masterIdentifier MS
* masterIdentifier ^short = "Dokumentets unika identifierare (record.recordId från RIVTA)"

* status 1..1 MS
* status ^short = "Alltid current — RIVTA-svaret saknar statusfält, källsystemet antas endast returnera aktiva anteckningar"

* type 1..1 MS
* type ^short = "Anteckningstyp (clinicalDocumentNoteCode, kodsystem ClinicalDocumentNoteCodeCS OID 1.2.752.129.2.2.2.11)"

* subject 1..1 MS
* subject only Reference(Patient)
* subject ^short = "Patient som dokumentet gäller"
* subject.identifier 1..1 MS
* subject.identifier.system 1..1 MS
* subject.identifier.value 1..1 MS

* date MS
* date ^short = "Journalpostens skapandetid (record.timestamp från RIVTA)"

* author 0..1 MS
* author ^short = "Dokumentationsansvarig (header.author.authorId) — logisk referens mot PractitionerRole via HSA-id"
* author.type MS
* author.identifier MS
* author.identifier.system MS
* author.identifier.value MS
* author.display MS
* author.display ^short = "Författarens visningsnamn (header.author.name)"

* authenticator 0..1 MS
* authenticator ^short = "Signerare (header.signature.signatureId) — logisk referens mot PractitionerRole via HSA-id"
* authenticator.type MS
* authenticator.identifier MS
* authenticator.display MS
* authenticator.display ^short = "Signerarens visningsnamn (header.signature.name)"

* description MS
* description ^short = "Anteckningens titel (clinicalDocumentNoteTitle från RIVTA)"

* context.related MS
* context.related.identifier MS
* context.related.identifier.value ^short = "careProcessId — referens till individanpassad vårdprocess"

* content 0..1 MS
* content.attachment 1..1 MS
* content.attachment.contentType MS
* content.attachment.contentType ^short = "text/plain; charset=utf-8 (clinicalDocumentNoteText) eller multimediaEntry.mediaType"
* content.attachment.data MS
* content.attachment.data ^short = "Base64: fritext (clinicalDocumentNoteText) eller binärdata (multimediaEntry.value)"
* content.attachment.url MS
* content.attachment.url ^short = "multimediaEntry.reference — ömsesidigt uteslutande med attachment.data inom multimediaEntry"
* content.attachment.title MS
