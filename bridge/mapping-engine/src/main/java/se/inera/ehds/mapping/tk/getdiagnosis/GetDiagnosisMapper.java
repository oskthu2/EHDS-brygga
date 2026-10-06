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
import java.util.regex.Pattern;
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
    private static final String DATA_ABSENT_REASON_EXT =
            "http://hl7.org/fhir/StructureDefinition/data-absent-reason";
    // Personnummer/samordningsnummer utan bindestreck: ÅÅÅÅMMDD + 4 siffror.
    private static final Pattern PERSONNUMMER_PATTERN = Pattern.compile("^\\d{12}$");

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

        HealthcareProfessionalType ahp = header.getAccountableHealthcareProfessional();

        // VG-scopat anrop: filtrera bort poster som tillhör en annan vårdgivare än den
        // efterfrågade — skydd om bakomliggande system returnerar flera vårdgivares poster.
        // Den riktiga PatientSummaryHeaderType har inget eget careProviderHSAId-fält;
        // vårdgivaren hämtas i stället från accountableHealthcareProfessional. Null-safe
        // eftersom accountableHealthcareProfessional är obligatoriskt i schemat men ett
        // felformat svar ändå inte ska ge NPE.
        String careGiverHsaId = ahp != null ? ahp.getHealthcareProfessionalCareGiverHSAId() : null;
        if (ctx != null && ctx.getRequestedVgHsaId() != null
                && !ctx.getRequestedVgHsaId().equals(careGiverHsaId)) {
            return null;
        }

        // Saknat eller felaktigt personnummer/samordningsnummer stoppar hela posten –
        // en Condition utan en tillförlitlig patientidentifierare kan inte levereras.
        PersonIdType patientId = header.getPatientId();
        if (patientId == null || !isValidPersonId(patientId.getId())) {
            return null;
        }

        Condition c = new Condition();
        c.setId(UUID.randomUUID().toString());
        c.getMeta().addProfile(PROFILE_URL);
        c.getMeta().addProfile(PROFILE_URL_EU_EPS);

        // clinicalStatus: alltid active för RIVTA-källa GetDiagnosis – den riktiga
        // PatientSummaryHeaderType/DiagnosisBodyType har inget slutdatum/resolution-koncept
        // (diagnosisTime är en enda tidpunkt, ingen period), till skillnad från den tidigare,
        // overifierade diagnosisTimePeriod som denna mappning uppfanns mot.
        c.setClinicalStatus(codeable(CLIN_STATUS_SYS, "active"));

        // verificationStatus: always confirmed for RIVTA-sourced data
        c.setVerificationStatus(codeable(VER_STATUS_SYS, "confirmed"));

        // category: typeOfDiagnosis ("Huvuddiagnos"/"Bidiagnos") via ConceptMap mot kv_diagnostyp.
        // Saknar typeOfDiagnosis en känd mappning fylls category[diagnostyp] inte i – i stället
        // anges data-absent-reason=unknown, se DIAG-003 i mapping-getdiagnosis.md.
        conceptMaps.translateDiagnosisType(body.getTypeOfDiagnosis())
                .ifPresentOrElse(
                        cat -> c.addCategory(
                                codeableWithDisplay(cat.getTargetSystem(), cat.getTargetCode(), cat.getDisplay())),
                        () -> c.addCategory(dataAbsentReasonUnknown()));

        // chronicDiagnosis: extension[chronicDiagnosis] med boolean
        if (body.getChronicDiagnosis() != null) {
            c.addExtension(new Extension(EXT_CHRONIC_CONDITION)
                    .setValue(new BooleanType(body.getChronicDiagnosis())));
        }

        // relatedDiagnosis: 0..unbounded i det riktiga schemat – en extension per post
        // (extensions kan upprepas på ett FHIR-element).
        for (RelatedDiagnosis related : body.getRelatedDiagnosis()) {
            if (related != null && related.getDocumentId() != null) {
                Reference relatedRef = new Reference()
                        .setIdentifier(new Identifier().setValue(related.getDocumentId()));
                c.addExtension(new Extension(EXT_RELATED_CONDITION).setValue(relatedRef));
            }
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

        // subject: patient identifier (validerad ovan – patientId finns och har giltigt format)
        c.setSubject(new Reference().setIdentifier(
                new Identifier()
                        .setSystem(namingSystem.oidToUri(patientId.getType()))
                        .setValue(patientId.getId())));

        // onset from diagnosisTime (en enda tidpunkt – inget abatement för GetDiagnosis,
        // eftersom det riktiga schemat inte har något slutdatum/period-koncept)
        if (body.getDiagnosisTime() != null) {
            c.setOnset(new DateTimeType(RivDateParser.parse(body.getDiagnosisTime())));
        }

        String hsaSystem = namingSystem.oidToUri(HSA_OID_INERA);

        // meta.source: källsystemets HSA-id som Endpoint i tjänstekatalogen
        // (urn:oid:...#hsaId är ingen giltig OID – kan inte bära ett HSA-id som fragment)
        String sourceHsaId = header.getSourceSystemHSAId();
        if (sourceHsaId != null) {
            c.getMeta().setSource(TJANSTEKATALOG_BASE + "/Endpoint/" + sourceHsaId);
        }

        // recorder: accountableHealthcareProfessional → PractitionerRole (logical reference)
        // recordedDate: accountableHealthcareProfessional/authorTime
        if (ahp != null) {
            if (ahp.getHealthcareProfessionalHSAId() != null) {
                c.setRecorder(practitionerRoleRef(ahp.getHealthcareProfessionalHSAId(), hsaSystem));
            }
            if (ahp.getAuthorTime() != null) {
                c.setRecordedDateElement(new DateTimeType(RivDateParser.parse(ahp.getAuthorTime())));
            }
        }

        // asserter: legalAuthenticator → PractitionerRole; signatureTime → extension[assertedDate]
        LegalAuthenticatorType la = header.getLegalAuthenticator();
        if (la != null) {
            if (la.getLegalAuthenticatorHSAId() != null) {
                c.setAsserter(practitionerRoleRef(la.getLegalAuthenticatorHSAId(), hsaSystem));
            }
            if (la.getSignatureTime() != null) {
                Extension extAd = new Extension(EXT_ASSERTED_DATE);
                extAd.setValue(new DateTimeType(RivDateParser.parse(la.getSignatureTime())));
                c.addExtension(extAd);
            }
        }

        // Provenance.recorded: samma källa som recordedDate (authorTime).
        // custodian/author: det riktiga schemat har inga careProviderHSAId/careUnitHSAId-fält
        // på headern – vårdgivare/vårdenhet hämtas i stället från accountableHealthcareProfessional.
        Provenance prov = ProvenanceBuilder.build(
                c.getId(),
                careGiverHsaId,
                ahp != null ? ahp.getHealthcareProfessionalCareUnitHSAId() : null,
                ahp != null ? ahp.getAuthorTime() : null,
                hsaSystem,
                ctx);

        return new MappedDiagnosisEntry(c, prov);
    }

    private Reference practitionerRoleRef(String hsaId, String defaultSystem) {
        return new Reference()
                .setType(PRACTITIONER_ROLE_TYPE)
                .setIdentifier(new Identifier().setSystem(defaultSystem).setValue(hsaId));
    }

    private CodeableConcept codeable(String system, String code) {
        return new CodeableConcept().addCoding(new Coding().setSystem(system).setCode(code));
    }

    private CodeableConcept codeableWithDisplay(String system, String code, String display) {
        return new CodeableConcept().addCoding(
                new Coding().setSystem(system).setCode(code).setDisplay(display));
    }

    private boolean isValidPersonId(String id) {
        return id != null && PERSONNUMMER_PATTERN.matcher(id).matches();
    }

    private CodeableConcept dataAbsentReasonUnknown() {
        CodeableConcept cc = new CodeableConcept();
        cc.addExtension(new Extension(DATA_ABSENT_REASON_EXT).setValue(new CodeType("unknown")));
        return cc;
    }
}
