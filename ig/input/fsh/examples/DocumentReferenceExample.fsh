Instance: DocumentReferenceExample
InstanceOf: SEEHDSDocumentReference
Title: "DocumentReference – exempeldokument"
Description: "Exempel på en SEEHDSDocumentReference mappat från GetCareDocumentation:3 (RIVTA)"
Usage: #example

* meta.source = "urn:oid:1.2.752.129.2.1.4.1#SE2321000016-39KJ"

* masterIdentifier.value = "rec-001-vgr"

* status = #current

* type.coding[0].system = "urn:oid:1.2.752.129.2.2.2.11"
* type.coding[0].code = #bes
* type.coding[0].display = "Besöksanteckning"

* subject.identifier.system = "http://electronichealth.se/identifier/personnummer"
* subject.identifier.value = "191212121212"

* date = "2024-01-15T10:30:00Z"

* author[0].type = "PractitionerRole"
* author[0].identifier.system = "urn:oid:1.2.752.129.2.1.4.1"
* author[0].identifier.value = "SE2321000016-39KJ"
* author[0].display = "Anna Andersson"

* authenticator.type = "PractitionerRole"
* authenticator.identifier.system = "urn:oid:1.2.752.129.2.1.4.1"
* authenticator.identifier.value = "SE2321000016-39KJ"
* authenticator.display = "Anna Andersson"

* description = "Mottagningsanteckning kardiologi"

* context.related[0].identifier.value = "process-42"

* content[0].attachment.contentType = #"text/plain; charset=utf-8"
* content[0].attachment.title = "Mottagningsanteckning kardiologi"
