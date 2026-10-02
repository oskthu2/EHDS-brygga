package se.inera.ehds.mapping.tk.getdiagnosis;

import org.hl7.fhir.r4.model.*;
import se.inera.ehds.mapping.concept.ConceptMapEntry;
import se.inera.ehds.mapping.concept.ConceptMapRegistry;
import se.inera.ehds.mapping.naming.NamingSystemRegistry;
import se.inera.ehds.mapping.rivta.*;
import se.inera.ehds.mapping.tk.MapperContext;
import se.inera.ehds.mapping.tk.MappedDiagnosisEntry;
import se.inera.ehds.mapping.tk.ProvenanceBuilder;
import se.inera.ehds.mapping.tk.RivDateParser;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class GetDiagnosisMapper {

    private static final String CANONICAL_BASE = "https://ehds-brygga.inera.se/fhir";
    private static final String HSA_OID_INERA = "1.2.752.129.2.1.4.1";
    private static final String DIAGNOSIS_TYPE_CS =
            "https://terminologitjansten.inera.se/inera-kodverksforvaltning/kodverk/kv_diagnostyp";
    private static final String TJANSTEKATALOG_BASE = "https://tjanstekatalogen.inera.se";
    private static final String CLIN_STATUS_SYS = "http://terminology.hl7.org/CodeSystem/condition-clinical";
    private static final String VER_STATUS_SYS = "http://terminology.hl7.org/CodeSystem/condition-ver-status";
    private static final String EXT_ASSERTED_DATE = CANONICAL_BASE + "/StructureDefinition/ext-asserted-date";
    private static final String EXT_CHRONIC_CONDITION = CANONICAL_BASE + "/StructureDefinition/ext-chronic-condition";
    private static final String EXT_RELATED_CONDITION = CANONICAL_BASE + "/StructureDefinition/ext-related-condition";
    private static final String PROFILE_URL = CANONICAL_BASE + "/StructureDefinition/se-ehds-condition";
    private static final String PROFILE_URL_EU_EPS =
            "http://hl7.eu/fhir/eps/StructureDefinition/condition-obl-eu-eps";
    private static final String PRACTITIONER_ROLE_TYPE = "PractitionerRole";

    private final NamingSystemRegistry namingSystem;
    private final ConceptMapRegistry conceptMaps;

    public GetDiagnosisMapper(NamingSystemRegistry namingSystem, ConceptMapRegistry conceptMaps) {
        this.namingSystem = namingSystem;
        this.conceptMaps = conceptMaps;
    }

    public List<MappedDiagnosisEntry> map(GetDiagnosisResponse response, MapperContext ctx) {
        if (response == null || response.getDiagnosis() == null) return List.of();
        ResultType result = response.getResult();
        if (result != null && !"OK".equals(result.getResultCode())) return List.of();

        return response.getDiagnosis().stream()
                .map(d -> mapDiagnosis(d, ctx))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private MappedDiagnosisEntry mapDiagnosis(Diagnosis diag, MapperContext ctx) {
        if (diag == null) return null;
        DiagnosisHeader header = diag.getDiagnosisHeader();
        DiagnosisBody body = diag.getDiagnosisBody();
        if (header == null || body == null) return null;

        // VG-scopat anrop: filtrera bort poster som tillhör en annan vårdgivare än den
        // efterfrågade — skydd om bakomliggande system returnerar flera vårdgivares poster.
        if (ctx != null && ctx.getRequestedVgHsaId() != null
                && !ctx.getRequestedVgHsaId().equals(header.getCareProviderHSAId())) {
            return null;
        }

        Condition c = new Condition();
        c.setId(UUID.randomUUID().toString());
        c.getMeta().addProfile(PROFILE_URL);
        c.getMeta().addProfile(PROFILE_URL_EU_EPS);

        // clinicalStatus: active unless there is an end date
        boolean resolved = body.getDiagnosisTimePeriod() != null
                && body.getDiagnosisTimePeriod().getEnd() != null;
        c.setClinicalStatus(codeable(CLIN_STATUS_SYS, resolved ? "resolved" : "active"));

        // verificationStatus: always confirmed for RIVTA-sourced data
        c.setVerificationStatus(codeable(VER_STATUS_SYS, "confirmed"));

        // category: diagnosisType (HD/BY) via ConceptMap mot kv_diagnostyp
        ConceptMapEntry cat = conceptMaps.translateDiagnosisType(body.getDiagnosisType())
                .orElseGet(() -> new ConceptMapEntry(
                        body.getDiagnosisType(),
                        DIAGNOSIS_TYPE_CS,
                        body.getDiagnosisType(),
                        body.getDiagnosisType()));
        c.addCategory(codeableWithDisplay(cat.getTargetSystem(), cat.getTargetCode(), cat.getDisplay()));

        // chronicCondition: extension[chronicDiagnosis] med boolean
        if (body.getChronicCondition() != null) {
            c.addExtension(new Extension(EXT_CHRONIC_CONDITION)
                    .setValue(new BooleanType(body.getChronicCondition())));
        }

        // relatedDiagnosis.documentId: extension[relatedCondition] med logisk referens
        RelatedDiagnosis related = body.getRelatedDiagnosis();
        if (related != null && related.getDocumentId() != null) {
            Reference relatedRef = new Reference()
                    .setIdentifier(new Identifier().setValue(related.getDocumentId()));
            c.addExtension(new Extension(EXT_RELATED_CONDITION).setValue(relatedRef));
        }

        // code: ICD-10-SE (or other coding system)
        CVType dc = body.getDiagnosisCode();
        if (dc != null) {
            String codeSystem = namingSystem.oidToUri(dc.getCodeSystem());
            String codeText = dc.getOriginalText() != null ? dc.getOriginalText() : dc.getDisplayName();
            c.setCode(new CodeableConcept()
                    .addCoding(new Coding()
                            .setSystem(codeSystem)
                            .setCode(dc.getCode())
                            .setDisplay(dc.getDisplayName()))
                    .setText(codeText));
        }

        // subject: patient identifier
        PersonIdType pid = header.getPatientId();
        if (pid != null) {
            c.setSubject(new Reference().setIdentifier(
                    new Identifier()
                            .setSystem(namingSystem.oidToUri(pid.getRoot()))
                            .setValue(pid.getExtension())));
        }

        // onset / abatement from diagnosisTimePeriod
        DatePeriodType period = body.getDiagnosisTimePeriod();
        if (period != null) {
            if (period.getStart() != null) {
                c.setOnset(new DateTimeType(RivDateParser.parse(period.getStart())));
            }
            if (period.getEnd() != null) {
                c.setAbatement(new DateTimeType(RivDateParser.parse(period.getEnd())));
            }
        }

        String hsaSystem = namingSystem.oidToUri(HSA_OID_INERA);

        // meta.source: källsystemets HSA-id som Endpoint i tjänstekatalogen
        // (urn:oid:...#hsaId är ingen giltig OID – kan inte bära ett HSA-id som fragment)
        String sourceHsaId = header.getSourceSystemHSAId();
        if (sourceHsaId != null) {
            c.getMeta().setSource(TJANSTEKATALOG_BASE + "/Endpoint/" + sourceHsaId);
        }

        // recorder: accountableHealthcareProfessional → PractitionerRole (logical reference)
        // recordedDate: accountableHealthcareProfessional/authorTime (documentTime har kardinalitet 0..0)
        HealthcareProfessionalType ahp = header.getAccountableHealthcareProfessional();
        if (ahp != null) {
            if (ahp.getPersonId() != null) {
                c.setRecorder(practitionerRoleRef(ahp.getPersonId(), hsaSystem));
            }
            if (ahp.getAuthorTime() != null) {
                c.setRecordedDateElement(new DateTimeType(RivDateParser.parse(ahp.getAuthorTime())));
            }
        }

        // asserter: legalAuthenticator → PractitionerRole; signatureDate → extension[assertedDate]
        LegalAuthenticatorType la = header.getLegalAuthenticator();
        if (la != null) {
            if (la.getHcProfessional() != null && la.getHcProfessional().getPersonId() != null) {
                c.setAsserter(practitionerRoleRef(la.getHcProfessional().getPersonId(), hsaSystem));
            }
            if (la.getSignatureDate() != null) {
                Extension extAd = new Extension(EXT_ASSERTED_DATE);
                extAd.setValue(new DateTimeType(RivDateParser.parse(la.getSignatureDate())));
                c.addExtension(extAd);
            }
        }

        // Provenance.recorded: samma källa som recordedDate (authorTime) – documentTime är 0..0
        Provenance prov = ProvenanceBuilder.build(
                c.getId(),
                header.getCareProviderHSAId(),
                header.getCareUnitHSAId(),
                ahp != null ? ahp.getAuthorTime() : null,
                hsaSystem,
                ctx);

        return new MappedDiagnosisEntry(c, prov);
    }

    private Reference practitionerRoleRef(PersonIdType personId, String defaultSystem) {
        String system = personId.getRoot() != null
                ? namingSystem.oidToUri(personId.getRoot())
                : defaultSystem;
        return new Reference()
                .setType(PRACTITIONER_ROLE_TYPE)
                .setIdentifier(new Identifier().setSystem(system).setValue(personId.getExtension()));
    }

    private CodeableConcept codeable(String system, String code) {
        return new CodeableConcept().addCoding(new Coding().setSystem(system).setCode(code));
    }

    private CodeableConcept codeableWithDisplay(String system, String code, String display) {
        return new CodeableConcept().addCoding(
                new Coding().setSystem(system).setCode(code).setDisplay(display));
    }
}
