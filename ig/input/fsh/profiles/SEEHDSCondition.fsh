Profile: SEEHDSCondition
Parent: $ipsCondition
Id: se-ehds-condition
Title: "SE EHDS Condition"
Description: """
FHIR Condition-profil för EHDS-bryggan, alignad med IPS (International Patient Summary)
och EURIDICE/EHDS EU-specifikationer.
Mappas från Ineras RIVTA-tjänstekontrakt GetDiagnosis
(clinicalprocess:activity:conditions:GetDiagnosis:2).

Profilen säkerställer att:
- Diagnoskod (ICD-10-SE) är angiven
- Patient är identifierad med personnummer eller samordningsnummer
- Diagnostyp (huvud-/bidiagnos) är angiven
- Källsystem kan spåras via extension och recorder
"""

* ^url = "https://ehds-brygga.inera.se/fhir/StructureDefinition/se-ehds-condition"

// EURIDICE/EHDS alignment: markera att denna profil implementerar EU EHDS Condition
* ^baseDefinition = "http://hl7.org/fhir/uv/ips/StructureDefinition/Condition-uv-ips"

// IPS kräver att patientens medicinska historia är representerad
* bodySite MS
* note MS

* extension contains ExtSourceSystem named sourceSystem 0..1 MS
* extension[sourceSystem] ^short = "HSA-id för källsystemet som registrerade diagnosen"

* clinicalStatus 1..1 MS
* clinicalStatus ^short = "Klinisk status: active om ingen slutdatum, resolved om slutdatum finns"
* clinicalStatus from $conditionClinical (required)

* verificationStatus 1..1 MS
* verificationStatus ^short = "Verifieringsstatus – sätts normalt till confirmed vid mappning från RIVTA"
* verificationStatus from $conditionVerStatus (required)

* category 1..* MS
* category ^short = "Diagnostyp (HD=Huvuddiagnos → encounter-diagnosis, BY=Bidiagnos → bi-diagnos)"
* category from https://ehds-brygga.inera.se/fhir/ValueSet/SEDiagnosisType (extensible)

* code 1..1 MS
* code ^short = "Diagnoskod (t.ex. ICD-10-SE)"
* code.coding 1..* MS
* code.coding ^slicing.discriminator.type = #value
* code.coding ^slicing.discriminator.path = "system"
* code.coding ^slicing.rules = #open
* code.coding ^slicing.description = "Diagnoskodning med stöd för ICD-10-SE som namngivet snitt"
* code.coding contains ICD10SE 0..1

* code.coding[ICD10SE] ^short = "ICD-10-SE diagnoskod"
* code.coding[ICD10SE] ^definition = "Diagnoskodning enligt ICD-10-SE, det svenska tillägget till ICD-10"
* code.coding[ICD10SE].system 1..1
* code.coding[ICD10SE].system = $icd10se (exactly)
* code.coding[ICD10SE].system ^short = "ICD-10-SE kodsystem (https://www.icd10.se/)"
* code.coding[ICD10SE].code 1..1
* code.coding[ICD10SE].code ^short = "ICD-10-SE kod, t.ex. J18.9"
* code.coding[ICD10SE].display MS
* code.coding[ICD10SE].display ^short = "Klartext för diagnosen, t.ex. Pneumoni, ospecificerad"

* subject 1..1 MS
* subject only Reference(Patient)
* subject ^short = "Patient som diagnosen gäller – identifieras med personnummer eller samordningsnummer"
* subject.identifier 1..1 MS
* subject.identifier ^short = "Patientidentifierare (personnummer eller samordningsnummer)"
* subject.identifier.system 1..1 MS
* subject.identifier.system ^short = "http://electronichealth.se/identifier/personnummer eller http://electronichealth.se/identifier/samordningsnummer"
* subject.identifier.value 1..1 MS
* subject.identifier.value ^short = "Personnummer eller samordningsnummer, t.ex. 191212121212"

* onset[x] MS
* onset[x] only dateTime
* onsetDateTime ^short = "Diagnosens startdatum, mappat från diagnosisBody.diagnosisTimePeriod.start"

* abatement[x] MS
* abatement[x] only dateTime
* abatementDateTime ^short = "Diagnosens slutdatum, mappat från diagnosisBody.diagnosisTimePeriod.end"

* recordedDate MS
* recordedDate ^short = "Registreringsdatum, mappat från diagnosisHeader.documentTime (YYYYMMDDHHMMSS → ISO 8601)"

* recorder MS
* recorder only Reference(Practitioner or PractitionerRole or Patient or RelatedPerson or Organization)
* recorder ^short = "Registrerande enhet eller system"
* recorder.identifier MS
* recorder.identifier ^short = "HSA-id för registrerande system eller enhet"
* recorder.identifier.system MS
* recorder.identifier.system ^short = "urn:oid:1.2.752.129.2.1.4.1 (HSA-id, Inera NTjP) eller urn:oid:1.2.752.29.4.19 (HSA-id, basprofil)"
* recorder.identifier.value MS
* recorder.identifier.value ^short = "HSA-id, t.ex. SE2321000016-4HK5"
