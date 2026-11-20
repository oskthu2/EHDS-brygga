
import sys
import argparse
import time as sys_time
import uuid
import re
import io
import json
import decimal
from datetime import datetime, time as datetime_time
from malac.utils import date as dateutil, exceptions, html as htmlutil
from html import escape as html_escape
import lm_diagnosis_model
import malac.models.fhir.r5
from malac.models.fhir import utils
from malac.utils import fhirpath

description_text = "This has been compiled by the MApping LAnguage compiler for Health Data, short MaLaC-HD. See arguments for more details."
one_timestamp = datetime.now(datetime.now().astimezone().tzinfo)
fhirpath_utils = fhirpath.FHIRPathUtils(malac.models.fhir.r5)
shared_vars = {}

def init_argparse() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=description_text)
    parser.add_argument(
       '-s', '--source', help="the source file path, or '-' to read from stdin", required=True
    )
    parser.add_argument(
       '-t', '--target', help="the target file path the result will be written to, or '-' to write to stdout", required=True
    )
    parser.add_argument(
       '-st', '--source-type', choices=['xml', 'json'], help="source format; required when the source is '-' (stdin), otherwise detected from the file extension", required=False
    )
    parser.add_argument(
       '-tt', '--target-type', choices=['xml', 'json'], help="target format; required when the target is '-' (stdout), otherwise detected from the file extension", required=False
    )
    return parser

_SOURCE_JSON_SUPPORTED = False
_TARGET_JSON_SUPPORTED = True

def _resolve_format(path, explicit, kind, json_supported):
    fmt = explicit
    if not fmt:
        if path == '-':
            raise ValueError("when the %s is '-' its format must be set with --%s-type" % (kind, kind))
        if path.endswith('.xml'):
            fmt = 'xml'
        elif path.endswith('.json'):
            fmt = 'json'
        else:
            raise ValueError("unknown %s file ending (expected .xml or .json): %s" % (kind, path))
    if fmt == 'json' and not json_supported:
        raise ValueError("JSON is not supported for the %s of this mapping (the model is XML-only)" % kind)
    return fmt

def _read_source(source_path, source_type):
    if source_type == 'xml':
        src = sys.stdin.buffer if source_path == '-' else source_path
        return lm_diagnosis_model.parse(src, silence=True)
    raise ValueError('unknown source type: ' + str(source_type))

def _write_target(target_path, target_type, result):
    out = sys.stdout if target_path == '-' else open(target_path, 'w', newline='', encoding='utf-8')
    try:
        if target_type == 'xml':
            out.write('<?xml version="1.0" encoding="UTF-8"?>\n')
            result.export(out, 0, namespacedef_='xmlns="http://hl7.org/fhir" xmlns:v3="urn:hl7-org:v3"')
        elif target_type == 'json':
            json.dump(result.exportJson(), out, indent=4, cls=malac.models.fhir.r5.GdsJsonEncoder_)
        else:
            raise ValueError('unknown target type: ' + str(target_type))
    finally:
        if target_path != '-':
            out.close()

def transform(source_path, target_path, source_type=None, target_type=None):
    start = sys_time.time()
    print('+++++++ Transformation from '+source_path+' to '+target_path+' started +++++++', file=sys.stderr)

    source_type = _resolve_format(source_path, source_type, 'source', _SOURCE_JSON_SUPPORTED)
    target_type = _resolve_format(target_path, target_type, 'target', _TARGET_JSON_SUPPORTED)
    src = _read_source(source_path, source_type)
    tgt = malac.models.fhir.r5.Provenance()
    GetDiagnosisToProvenance(src, tgt)
    _write_target(target_path, target_type, tgt)

    print('altogether in '+str(round(sys_time.time()-start,3))+' seconds.', file=sys.stderr)
    print('+++++++ Transformation from '+source_path+' to '+target_path+' ended  +++++++', file=sys.stderr)

def GetDiagnosisToProvenance(src, tgt):
    for d in (getattr(src, 'diagnosis', None) if isinstance(getattr(src, 'diagnosis', None), list) else ([] if not getattr(src, 'diagnosis', None) else [getattr(src, 'diagnosis', None)])):
        for h in (getattr(d, 'diagnosisHeader', None) if isinstance(getattr(d, 'diagnosisHeader', None), list) else ([] if not getattr(d, 'diagnosisHeader', None) else [getattr(d, 'diagnosisHeader', None)])):
            for ahp in (getattr(h, 'accountableHealthcareProfessional', None) if isinstance(getattr(h, 'accountableHealthcareProfessional', None), list) else ([] if not getattr(h, 'accountableHealthcareProfessional', None) else [getattr(h, 'accountableHealthcareProfessional', None)])):
                for giver in (getattr(ahp, 'healthcareProfessionalCareGiverHSAId', None) if isinstance(getattr(ahp, 'healthcareProfessionalCareGiverHSAId', None), list) else ([] if not getattr(ahp, 'healthcareProfessionalCareGiverHSAId', None) else [getattr(ahp, 'healthcareProfessionalCareGiverHSAId', None)])):
                    for giverVal in (getattr(giver, 'value', None) if isinstance(getattr(giver, 'value', None), list) else ([] if not getattr(giver, 'value', None) else [getattr(giver, 'value', None)])):
                        custAgent = malac.models.fhir.r5.Provenance_Agent()
                        tgt.agent.append(custAgent)
                        custAgent.type_ = malac.models.fhir.r5.CodeableConcept(coding=[malac.models.fhir.r5.Coding(system=malac.models.fhir.r5.uri(value='http://terminology.hl7.org/CodeSystem/provenance-participant-type'), code=malac.models.fhir.r5.string(value='custodian'))])
                        custWho = malac.models.fhir.r5.Reference()
                        custAgent.who = custWho
                        custId = malac.models.fhir.r5.Identifier()
                        custWho.identifier = custId
                        for giverValSys in (giverVal if isinstance(giverVal, list) else ([] if not giverVal else [giverVal])):
                            custId.system = malac.models.fhir.r5.uri(value='urn:oid:1.2.752.129.2.1.4.1')
                        for giverValV in (giverVal if isinstance(giverVal, list) else ([] if not giverVal else [giverVal])):
                            custId.value = (malac.models.fhir.r5.string(value=giverValV) if isinstance(giverValV, str) else malac.models.fhir.r5.string(value=giverValV.value))
                for unit in (getattr(ahp, 'healthcareProfessionalCareUnitHSAId', None) if isinstance(getattr(ahp, 'healthcareProfessionalCareUnitHSAId', None), list) else ([] if not getattr(ahp, 'healthcareProfessionalCareUnitHSAId', None) else [getattr(ahp, 'healthcareProfessionalCareUnitHSAId', None)])):
                    for unitVal in (getattr(unit, 'value', None) if isinstance(getattr(unit, 'value', None), list) else ([] if not getattr(unit, 'value', None) else [getattr(unit, 'value', None)])):
                        authAgent = malac.models.fhir.r5.Provenance_Agent()
                        tgt.agent.append(authAgent)
                        authAgent.type_ = malac.models.fhir.r5.CodeableConcept(coding=[malac.models.fhir.r5.Coding(system=malac.models.fhir.r5.uri(value='http://terminology.hl7.org/CodeSystem/provenance-participant-type'), code=malac.models.fhir.r5.string(value='author'))])
                        authWho = malac.models.fhir.r5.Reference()
                        authAgent.who = authWho
                        authId = malac.models.fhir.r5.Identifier()
                        authWho.identifier = authId
                        for unitValSys in (unitVal if isinstance(unitVal, list) else ([] if not unitVal else [unitVal])):
                            authId.system = malac.models.fhir.r5.uri(value='urn:oid:1.2.752.129.2.1.4.1')
                        for unitValV in (unitVal if isinstance(unitVal, list) else ([] if not unitVal else [unitVal])):
                            authId.value = (malac.models.fhir.r5.string(value=unitValV) if isinstance(unitValV, str) else malac.models.fhir.r5.string(value=unitValV.value))
    asmAgent = malac.models.fhir.r5.Provenance_Agent()
    tgt.agent.append(asmAgent)
    asmAgent.type_ = malac.models.fhir.r5.CodeableConcept(coding=[malac.models.fhir.r5.Coding(system=malac.models.fhir.r5.uri(value='http://terminology.hl7.org/CodeSystem/provenance-participant-type'), code=malac.models.fhir.r5.string(value='assembler'))])
    asmWho = malac.models.fhir.r5.Reference()
    asmAgent.who = asmWho
    asmId = malac.models.fhir.r5.Identifier()
    asmWho.identifier = asmId
    asmId.system = malac.models.fhir.r5.uri(value='urn:oid:1.2.752.129.2.1.4.1')
    asmId.value = malac.models.fhir.r5.string(value='SE2321000999-EHDS')

def unpack_container(resource_container):
    if resource_container is None:
        return None
    if resource_container.Account is not None:
        return resource_container.Account
    if resource_container.ActivityDefinition is not None:
        return resource_container.ActivityDefinition
    if resource_container.ActorDefinition is not None:
        return resource_container.ActorDefinition
    if resource_container.AdministrableProductDefinition is not None:
        return resource_container.AdministrableProductDefinition
    if resource_container.AdverseEvent is not None:
        return resource_container.AdverseEvent
    if resource_container.AllergyIntolerance is not None:
        return resource_container.AllergyIntolerance
    if resource_container.Appointment is not None:
        return resource_container.Appointment
    if resource_container.AppointmentResponse is not None:
        return resource_container.AppointmentResponse
    if resource_container.ArtifactAssessment is not None:
        return resource_container.ArtifactAssessment
    if resource_container.AuditEvent is not None:
        return resource_container.AuditEvent
    if resource_container.Basic is not None:
        return resource_container.Basic
    if resource_container.Binary is not None:
        return resource_container.Binary
    if resource_container.BiologicallyDerivedProduct is not None:
        return resource_container.BiologicallyDerivedProduct
    if resource_container.BiologicallyDerivedProductDispense is not None:
        return resource_container.BiologicallyDerivedProductDispense
    if resource_container.BodyStructure is not None:
        return resource_container.BodyStructure
    if resource_container.Bundle is not None:
        return resource_container.Bundle
    if resource_container.CapabilityStatement is not None:
        return resource_container.CapabilityStatement
    if resource_container.CarePlan is not None:
        return resource_container.CarePlan
    if resource_container.CareTeam is not None:
        return resource_container.CareTeam
    if resource_container.ChargeItem is not None:
        return resource_container.ChargeItem
    if resource_container.ChargeItemDefinition is not None:
        return resource_container.ChargeItemDefinition
    if resource_container.Citation is not None:
        return resource_container.Citation
    if resource_container.Claim is not None:
        return resource_container.Claim
    if resource_container.ClaimResponse is not None:
        return resource_container.ClaimResponse
    if resource_container.ClinicalImpression is not None:
        return resource_container.ClinicalImpression
    if resource_container.ClinicalUseDefinition is not None:
        return resource_container.ClinicalUseDefinition
    if resource_container.CodeSystem is not None:
        return resource_container.CodeSystem
    if resource_container.Communication is not None:
        return resource_container.Communication
    if resource_container.CommunicationRequest is not None:
        return resource_container.CommunicationRequest
    if resource_container.CompartmentDefinition is not None:
        return resource_container.CompartmentDefinition
    if resource_container.Composition is not None:
        return resource_container.Composition
    if resource_container.ConceptMap is not None:
        return resource_container.ConceptMap
    if resource_container.Condition is not None:
        return resource_container.Condition
    if resource_container.ConditionDefinition is not None:
        return resource_container.ConditionDefinition
    if resource_container.Consent is not None:
        return resource_container.Consent
    if resource_container.Contract is not None:
        return resource_container.Contract
    if resource_container.Coverage is not None:
        return resource_container.Coverage
    if resource_container.CoverageEligibilityRequest is not None:
        return resource_container.CoverageEligibilityRequest
    if resource_container.CoverageEligibilityResponse is not None:
        return resource_container.CoverageEligibilityResponse
    if resource_container.DetectedIssue is not None:
        return resource_container.DetectedIssue
    if resource_container.Device is not None:
        return resource_container.Device
    if resource_container.DeviceAssociation is not None:
        return resource_container.DeviceAssociation
    if resource_container.DeviceDefinition is not None:
        return resource_container.DeviceDefinition
    if resource_container.DeviceDispense is not None:
        return resource_container.DeviceDispense
    if resource_container.DeviceMetric is not None:
        return resource_container.DeviceMetric
    if resource_container.DeviceRequest is not None:
        return resource_container.DeviceRequest
    if resource_container.DeviceUsage is not None:
        return resource_container.DeviceUsage
    if resource_container.DiagnosticReport is not None:
        return resource_container.DiagnosticReport
    if resource_container.DocumentReference is not None:
        return resource_container.DocumentReference
    if resource_container.Encounter is not None:
        return resource_container.Encounter
    if resource_container.EncounterHistory is not None:
        return resource_container.EncounterHistory
    if resource_container.Endpoint is not None:
        return resource_container.Endpoint
    if resource_container.EnrollmentRequest is not None:
        return resource_container.EnrollmentRequest
    if resource_container.EnrollmentResponse is not None:
        return resource_container.EnrollmentResponse
    if resource_container.EpisodeOfCare is not None:
        return resource_container.EpisodeOfCare
    if resource_container.EventDefinition is not None:
        return resource_container.EventDefinition
    if resource_container.Evidence is not None:
        return resource_container.Evidence
    if resource_container.EvidenceReport is not None:
        return resource_container.EvidenceReport
    if resource_container.EvidenceVariable is not None:
        return resource_container.EvidenceVariable
    if resource_container.ExampleScenario is not None:
        return resource_container.ExampleScenario
    if resource_container.ExplanationOfBenefit is not None:
        return resource_container.ExplanationOfBenefit
    if resource_container.FamilyMemberHistory is not None:
        return resource_container.FamilyMemberHistory
    if resource_container.Flag is not None:
        return resource_container.Flag
    if resource_container.FormularyItem is not None:
        return resource_container.FormularyItem
    if resource_container.GenomicStudy is not None:
        return resource_container.GenomicStudy
    if resource_container.Goal is not None:
        return resource_container.Goal
    if resource_container.GraphDefinition is not None:
        return resource_container.GraphDefinition
    if resource_container.Group is not None:
        return resource_container.Group
    if resource_container.GuidanceResponse is not None:
        return resource_container.GuidanceResponse
    if resource_container.HealthcareService is not None:
        return resource_container.HealthcareService
    if resource_container.ImagingSelection is not None:
        return resource_container.ImagingSelection
    if resource_container.ImagingStudy is not None:
        return resource_container.ImagingStudy
    if resource_container.Immunization is not None:
        return resource_container.Immunization
    if resource_container.ImmunizationEvaluation is not None:
        return resource_container.ImmunizationEvaluation
    if resource_container.ImmunizationRecommendation is not None:
        return resource_container.ImmunizationRecommendation
    if resource_container.ImplementationGuide is not None:
        return resource_container.ImplementationGuide
    if resource_container.Ingredient is not None:
        return resource_container.Ingredient
    if resource_container.InsurancePlan is not None:
        return resource_container.InsurancePlan
    if resource_container.InventoryItem is not None:
        return resource_container.InventoryItem
    if resource_container.InventoryReport is not None:
        return resource_container.InventoryReport
    if resource_container.Invoice is not None:
        return resource_container.Invoice
    if resource_container.Library is not None:
        return resource_container.Library
    if resource_container.Linkage is not None:
        return resource_container.Linkage
    if resource_container.List is not None:
        return resource_container.List
    if resource_container.Location is not None:
        return resource_container.Location
    if resource_container.ManufacturedItemDefinition is not None:
        return resource_container.ManufacturedItemDefinition
    if resource_container.Measure is not None:
        return resource_container.Measure
    if resource_container.MeasureReport is not None:
        return resource_container.MeasureReport
    if resource_container.Medication is not None:
        return resource_container.Medication
    if resource_container.MedicationAdministration is not None:
        return resource_container.MedicationAdministration
    if resource_container.MedicationDispense is not None:
        return resource_container.MedicationDispense
    if resource_container.MedicationKnowledge is not None:
        return resource_container.MedicationKnowledge
    if resource_container.MedicationRequest is not None:
        return resource_container.MedicationRequest
    if resource_container.MedicationStatement is not None:
        return resource_container.MedicationStatement
    if resource_container.MedicinalProductDefinition is not None:
        return resource_container.MedicinalProductDefinition
    if resource_container.MessageDefinition is not None:
        return resource_container.MessageDefinition
    if resource_container.MessageHeader is not None:
        return resource_container.MessageHeader
    if resource_container.MolecularSequence is not None:
        return resource_container.MolecularSequence
    if resource_container.NamingSystem is not None:
        return resource_container.NamingSystem
    if resource_container.NutritionIntake is not None:
        return resource_container.NutritionIntake
    if resource_container.NutritionOrder is not None:
        return resource_container.NutritionOrder
    if resource_container.NutritionProduct is not None:
        return resource_container.NutritionProduct
    if resource_container.Observation is not None:
        return resource_container.Observation
    if resource_container.ObservationDefinition is not None:
        return resource_container.ObservationDefinition
    if resource_container.OperationDefinition is not None:
        return resource_container.OperationDefinition
    if resource_container.OperationOutcome is not None:
        return resource_container.OperationOutcome
    if resource_container.Organization is not None:
        return resource_container.Organization
    if resource_container.OrganizationAffiliation is not None:
        return resource_container.OrganizationAffiliation
    if resource_container.PackagedProductDefinition is not None:
        return resource_container.PackagedProductDefinition
    if resource_container.Patient is not None:
        return resource_container.Patient
    if resource_container.PaymentNotice is not None:
        return resource_container.PaymentNotice
    if resource_container.PaymentReconciliation is not None:
        return resource_container.PaymentReconciliation
    if resource_container.Permission is not None:
        return resource_container.Permission
    if resource_container.Person is not None:
        return resource_container.Person
    if resource_container.PlanDefinition is not None:
        return resource_container.PlanDefinition
    if resource_container.Practitioner is not None:
        return resource_container.Practitioner
    if resource_container.PractitionerRole is not None:
        return resource_container.PractitionerRole
    if resource_container.Procedure is not None:
        return resource_container.Procedure
    if resource_container.Provenance is not None:
        return resource_container.Provenance
    if resource_container.Questionnaire is not None:
        return resource_container.Questionnaire
    if resource_container.QuestionnaireResponse is not None:
        return resource_container.QuestionnaireResponse
    if resource_container.RegulatedAuthorization is not None:
        return resource_container.RegulatedAuthorization
    if resource_container.RelatedPerson is not None:
        return resource_container.RelatedPerson
    if resource_container.RequestOrchestration is not None:
        return resource_container.RequestOrchestration
    if resource_container.Requirements is not None:
        return resource_container.Requirements
    if resource_container.ResearchStudy is not None:
        return resource_container.ResearchStudy
    if resource_container.ResearchSubject is not None:
        return resource_container.ResearchSubject
    if resource_container.RiskAssessment is not None:
        return resource_container.RiskAssessment
    if resource_container.Schedule is not None:
        return resource_container.Schedule
    if resource_container.SearchParameter is not None:
        return resource_container.SearchParameter
    if resource_container.ServiceRequest is not None:
        return resource_container.ServiceRequest
    if resource_container.Slot is not None:
        return resource_container.Slot
    if resource_container.Specimen is not None:
        return resource_container.Specimen
    if resource_container.SpecimenDefinition is not None:
        return resource_container.SpecimenDefinition
    if resource_container.StructureDefinition is not None:
        return resource_container.StructureDefinition
    if resource_container.StructureMap is not None:
        return resource_container.StructureMap
    if resource_container.Subscription is not None:
        return resource_container.Subscription
    if resource_container.SubscriptionStatus is not None:
        return resource_container.SubscriptionStatus
    if resource_container.SubscriptionTopic is not None:
        return resource_container.SubscriptionTopic
    if resource_container.Substance is not None:
        return resource_container.Substance
    if resource_container.SubstanceDefinition is not None:
        return resource_container.SubstanceDefinition
    if resource_container.SubstanceNucleicAcid is not None:
        return resource_container.SubstanceNucleicAcid
    if resource_container.SubstancePolymer is not None:
        return resource_container.SubstancePolymer
    if resource_container.SubstanceProtein is not None:
        return resource_container.SubstanceProtein
    if resource_container.SubstanceReferenceInformation is not None:
        return resource_container.SubstanceReferenceInformation
    if resource_container.SubstanceSourceMaterial is not None:
        return resource_container.SubstanceSourceMaterial
    if resource_container.SupplyDelivery is not None:
        return resource_container.SupplyDelivery
    if resource_container.SupplyRequest is not None:
        return resource_container.SupplyRequest
    if resource_container.Task is not None:
        return resource_container.Task
    if resource_container.TerminologyCapabilities is not None:
        return resource_container.TerminologyCapabilities
    if resource_container.TestPlan is not None:
        return resource_container.TestPlan
    if resource_container.TestReport is not None:
        return resource_container.TestReport
    if resource_container.TestScript is not None:
        return resource_container.TestScript
    if resource_container.Transport is not None:
        return resource_container.Transport
    if resource_container.ValueSet is not None:
        return resource_container.ValueSet
    if resource_container.VerificationResult is not None:
        return resource_container.VerificationResult
    if resource_container.VisionPrescription is not None:
        return resource_container.VisionPrescription
    if resource_container.Parameters is not None:
        return resource_container.Parameters
    return None

if __name__ == "__main__":
    parser = init_argparse()
    args = parser.parse_args()
    transform(args.source, args.target, args.source_type, args.target_type)
