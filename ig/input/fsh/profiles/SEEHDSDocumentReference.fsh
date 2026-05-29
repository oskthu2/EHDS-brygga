Profile: SEEHDSDocumentReference
Parent: DocumentReference
Id: se-ehds-document-reference
Title: "SE EHDS DocumentReference"
Description: """
FHIR DocumentReference-profil för EHDS-bryggan.
Mappas från Ineras RIVTA-tjänstekontrakt GetDocumentList
(clinicalprocess:healthrecord:GetDocumentList:1).

Profilen alignar med EURIDICE/EHDS EU-specifikationer för kliniska dokument
och säkerställer att:
- Dokumenttyp (LOINC) är angiven
- Patient är identifierad med personnummer eller samordningsnummer
- Källsystem kan spåras via extension ext-source-system
- Vårdenhet och vårdgivare är angivna
"""

* ^url = "https://ehds-brygga.inera.se/fhir/StructureDefinition/se-ehds-document-reference"

* extension contains ExtSourceSystem named sourceSystem 0..1 MS
* extension[sourceSystem] ^short = "HSA-id för källsystemet som registrerade dokumentet"

* masterIdentifier MS
* masterIdentifier ^short = "Dokumentets unika identifierare (documentId från RIVTA)"

* status 1..1 MS
* status ^short = "current för aktiva dokument, superseded för inaktiva"

* type 1..1 MS
* type ^short = "Dokumenttyp (t.ex. LOINC-kod)"
* type from http://hl7.org/fhir/ValueSet/c80-doc-typecodes (extensible)

* subject 1..1 MS
* subject only Reference(Patient)
* subject ^short = "Patient som dokumentet gäller"
* subject.identifier 1..1 MS
* subject.identifier.system 1..1 MS
* subject.identifier.value 1..1 MS

* date MS
* date ^short = "Dokumentets skapandetid (documentTime från RIVTA)"

* author 1..* MS
* author ^short = "Vårdenhetens HSA-id (careUnitHSAId från RIVTA)"
* author.identifier MS
* author.identifier.system MS
* author.identifier.system ^short = "urn:oid:1.2.752.129.2.1.4.1 (HSA-id Inera) eller urn:oid:1.2.752.29.4.19 (HSA-id basprofil)"
* author.identifier.value MS

* custodian MS
* custodian ^short = "Vårdgivarens HSA-id (careProviderHSAId från RIVTA)"
* custodian.identifier MS

* description MS
* description ^short = "Dokumenttitel (title från RIVTA)"

* content 1..* MS
* content.attachment 1..1 MS
* content.attachment.contentType MS
* content.attachment.contentType ^short = "application/pdf eller text/plain"
* content.attachment.title MS
