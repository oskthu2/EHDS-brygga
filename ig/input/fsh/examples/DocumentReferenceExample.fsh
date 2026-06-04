Instance: DocumentReferenceExample
InstanceOf: SEEHDSDocumentReference
Title: "DocumentReference – exempeldokument"
Description: "Exempel på en SEEHDSDocumentReference mappat från GetDocumentList (RIVTA)"
Usage: #example

* meta.source = "urn:oid:1.2.752.129.2.1.4.1#SE2321000016-39KJ"

* status = #current

* type.coding[0].system = "http://loinc.org"
* type.coding[0].code = #34133-9
* type.coding[0].display = "Summary of episode note"

* subject.identifier.system = "http://electronichealth.se/identifier/personnummer"
* subject.identifier.value = "191212121212"

* date = "2024-01-15T10:30:00Z"

* author[0].identifier.system = "urn:oid:1.2.752.129.2.1.4.1"
* author[0].identifier.value = "SE2321000016-39KJ"

* description = "Inskrivningsanteckning"

* content[0].attachment.contentType = #application/pdf
* content[0].attachment.title = "Inskrivningsanteckning"
