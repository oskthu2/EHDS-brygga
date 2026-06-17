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
- Patient är identifierad med personnummer eller samordningsnummer (SEEHDSPatient)
- Diagnostyp (HD/BY) är angiven som ett namngivet snitt i category
- Källsystem identifieras via meta.source (urn:oid:{HSA_OID}#{hsaId})
- Ansvarig hälso- och sjukvårdspersonal anges som recorder (SEBasePractitionerRole)
- Rättslig äkthetsintygsgivare anges som asserter (SEBasePractitionerRole)
- Ansvarig vårdgivare bärs av Provenance.agent[role=custodian] (inte inne i resursen)
"""

* ^url = "https://ehds-brygga.inera.se/fhir/StructureDefinition/se-ehds-condition"
* ^experimental = true

* bodySite MS
* note MS

* meta.source MS
* meta.source ^short = "HSA-id för källsystemet, format: urn:oid:1.2.752.129.2.1.4.1#{hsaId}"

* extension contains ExtAssertedDate named assertedDate 0..1 MS
* extension[assertedDate] ^short = "Administrativt intygsgivningsdatum för legalAuthenticator (YYYYMMDD → YYYY-MM-DD)"

* clinicalStatus 1..1 MS
* clinicalStatus ^short = "Klinisk status: active om inget slutdatum, resolved om slutdatum finns"
* clinicalStatus from $conditionClinical (required)

* verificationStatus 1..1 MS
* verificationStatus ^short = "Verifieringsstatus – sätts normalt till confirmed vid mappning från RIVTA"
* verificationStatus from $conditionVerStatus (required)

* category 1..* MS
* category ^slicing.discriminator.type = #value
* category ^slicing.discriminator.path = "coding.system"
* category ^slicing.rules = #open
* category ^short = "Diagnostyp: måste innehålla minst ett snitt med kod från Ineras kv_diagnostyp"
* category contains diagnostyp 1..1 MS
* category[diagnostyp] ^short = "Diagnostyp (HD=Huvuddiagnos, BY=Bidiagnos) från Ineras terminologitjänst"
* category[diagnostyp] from SEDiagnosisTypeVS (required)

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
* subject only Reference($seEhdsPatient)
* subject ^short = "Patient som diagnosen gäller – identifieras med personnummer eller samordningsnummer (SEEHDSPatient)"
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
* recorder only Reference($seBasePractitionerRole)
* recorder ^short = "Ansvarig hälso- och sjukvårdspersonal (accountableHealthcareProfessional från RIVTA) – SEBasePractitionerRole med HSA-id i identifier[hsaid]"

* asserter MS
* asserter only Reference($seBasePractitionerRole)
* asserter ^short = "Rättslig äkthetsintygsgivare (legalAuthenticator från RIVTA) – SEBasePractitionerRole med HSA-id i identifier[hsaid]; datum i extension[assertedDate]"
