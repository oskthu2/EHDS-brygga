// IPS profiles (basis för EURIDICE/EHDS)
Alias: $ipsCondition = http://hl7.org/fhir/uv/ips/StructureDefinition/Condition-uv-ips
Alias: $ipsPatient = http://hl7.org/fhir/uv/ips/StructureDefinition/Patient-uv-ips

// Svenska basprofiler (HL7 Sweden basprofiler-r4, hl7se.fhir.base)
Alias: $seBasePatient = http://hl7.se/fhir/ig/base/StructureDefinition/SEBasePatient
Alias: $seBasePractitionerRole = http://hl7.se/fhir/ig/base/StructureDefinition/SEBasePractitionerRole
Alias: $seBasePractitioner = http://hl7.se/fhir/ig/base/StructureDefinition/SEBasePractitioner
Alias: $seBaseOrganization = http://hl7.se/fhir/ig/base/StructureDefinition/SEBaseOrganization

// EHDS-brygga egna profiler
Alias: $seEhdsPatient = https://ehds-brygga.inera.se/fhir/StructureDefinition/se-ehds-patient

// EURIDICE/EHDS canonical base (EU EHDS IG)
Alias: $euCondition = https://hl7.eu/fhir/ehds/StructureDefinition/Condition-eu-ehds
Alias: $euDocumentReference = https://hl7.eu/fhir/ehds/StructureDefinition/DocumentReference-eu-ehds

// EU EPS (European Patient Summary) – HL7 Europe
Alias: $epsConditionObl = http://hl7.eu/fhir/eps/StructureDefinition/condition-obl-eu-eps

// Terminologi
Alias: $iheDocType = http://ihe.net/connectathon/classCodes

// Inera terminologitjänst
Alias: $diagnosisTypeCS = https://terminologitjansten.inera.se/inera-kodverksforvaltning/kodverk/kv_diagnostyp

// External code system aliases
Alias: $conditionClinical = http://hl7.org/fhir/ValueSet/condition-clinical
Alias: $conditionVerStatus = http://hl7.org/fhir/ValueSet/condition-ver-status
Alias: $conditionCategory = http://terminology.hl7.org/CodeSystem/condition-category
Alias: $loinc = http://loinc.org
Alias: $sct = http://snomed.info/sct
Alias: $sctSE = http://snomed.info/sct|http://snomed.info/sct/45991000052106
Alias: $icd10se = https://www.icd10.se/

Alias: ExtraSecurityRoleType = http://terminology.hl7.org/CodeSystem/extra-security-role-type
Alias: PurposeOfUse = http://terminology.hl7.org/CodeSystem/v3-ActReason

// Audit event terminology (DICOM + HL7 terminology.hl7.org)
Alias: DCM = http://dicom.nema.org/resources/ontology/DCM
Alias: AuditEntityType = http://terminology.hl7.org/CodeSystem/audit-entity-type
Alias: ObjectRole = http://terminology.hl7.org/CodeSystem/object-role
Alias: SEEHDSAuditSubtypeCS = https://ehds-brygga.inera.se/fhir/CodeSystem/audit-event-subtype

// Svenska identifierarsystem (HL7 Sweden basprofiler-r4)
Alias: $personnummerSystem = http://electronichealth.se/identifier/personnummer
Alias: $samordningsnummerSystem = http://electronichealth.se/identifier/samordningsnummer
Alias: $lmaNummer = http://electronichealth.se/identifier/LMA-nummer
Alias: $nationelltReservnummer = http://electronichealth.se/identifier/nationelltReservnummer
Alias: $hsaIdSystem = urn:oid:1.2.752.129.2.1.4.1
Alias: $hsaIdSystemBase = urn:oid:1.2.752.29.4.19
