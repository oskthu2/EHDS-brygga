package se.inera.ehds.mapping.tk.getdiagnosis;

import org.hl7.fhir.r4.model.*;
import se.inera.ehds.mapping.concept.ConceptMapEntry;
import se.inera.ehds.mapping.concept.ConceptMapRegistry;
import se.inera.ehds.mapping.naming.NamingSystemRegistry;
import se.inera.ehds.mapping.rivta.*;
import se.inera.ehds.mapping.tk.MapperContext;
import se.inera.ehds.mapping.tk.TkMapper;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class GetDiagnosisMapper implements TkMapper<GetDiagnosisResponse, Condition> {

    private static final String CANONICAL_BASE = "https://ehds-brygga.inera.se/fhir";
    private static final String HSA_OID = "1.2.752.129.2.1.4.1";
    private static final String CLIN_STATUS_SYS = "http://terminology.hl7.org/CodeSystem/condition-clinical";
    private static final String VER_STATUS_SYS = "http://terminology.hl7.org/CodeSystem/condition-ver-status";
    private static final String EXT_SOURCE_SYSTEM = CANONICAL_BASE + "/StructureDefinition/ext-source-system";
    private static final String PROFILE_URL = CANONICAL_BASE + "/StructureDefinition/se-ehds-condition";

    private final NamingSystemRegistry namingSystem;
    private final ConceptMapRegistry conceptMaps;

    public GetDiagnosisMapper(NamingSystemRegistry namingSystem, ConceptMapRegistry conceptMaps) {
        this.namingSystem = namingSystem;
        this.conceptMaps = conceptMaps;
    }

    @Override
    public List<Condition> map(GetDiagnosisResponse response, MapperContext ctx) {
        if (response == null || response.getDiagnosis() == null) return List.of();
        ResultType result = response.getResult();
        if (result != null && !"OK".equals(result.getResultCode())) return List.of();

        return response.getDiagnosis().stream()
                .map(d -> mapDiagnosis(d, ctx))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Condition mapDiagnosis(Diagnosis diag, MapperContext ctx) {
        if (diag == null) return null;
        DiagnosisHeader header = diag.getDiagnosisHeader();
        DiagnosisBody body = diag.getDiagnosisBody();
        if (header == null || body == null) return null;

        Condition c = new Condition();
        c.setId(UUID.randomUUID().toString());
        c.getMeta().addProfile(PROFILE_URL);

        // clinicalStatus: active unless there is an end date
        boolean resolved = body.getDiagnosisTimePeriod() != null
                && body.getDiagnosisTimePeriod().getEnd() != null;
        c.setClinicalStatus(codeable(CLIN_STATUS_SYS, resolved ? "resolved" : "active"));

        // verificationStatus: always confirmed for RIVTA diagnoses
        c.setVerificationStatus(codeable(VER_STATUS_SYS, "confirmed"));

        // category: HD → encounter-diagnosis, BY → bi-diagnos via ConceptMap
        ConceptMapEntry cat = conceptMaps.translateDiagnosisType(body.getDiagnosisType())
                .orElseGet(() -> new ConceptMapEntry(
                        body.getDiagnosisType(),
                        CANONICAL_BASE + "/CodeSystem/DiagnosisType",
                        body.getDiagnosisType(),
                        body.getDiagnosisType()));
        c.addCategory(codeableWithDisplay(cat.getTargetSystem(), cat.getTargetCode(), cat.getDisplay()));

        // code: ICD-10-SE (or other coding system)
        CVType dc = body.getDiagnosisCode();
        if (dc != null) {
            String codeSystem = namingSystem.oidToUri(dc.getCodeSystem());
            c.setCode(new CodeableConcept()
                    .addCoding(new Coding()
                            .setSystem(codeSystem)
                            .setCode(dc.getCode())
                            .setDisplay(dc.getDisplayName()))
                    .setText(dc.getDisplayName()));
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
                c.setOnset(new DateTimeType(parseRivDate(period.getStart())));
            }
            if (period.getEnd() != null) {
                c.setAbatement(new DateTimeType(parseRivDate(period.getEnd())));
            }
        }

        // recordedDate from documentTime
        if (header.getDocumentTime() != null) {
            c.setRecordedDateElement(new DateTimeType(parseRivDate(header.getDocumentTime())));
        }

        // recorder: source system HSA-id
        String sourceHsaId = header.getSourceSystemHSAId();
        if (sourceHsaId != null) {
            String hsaSystem = namingSystem.oidToUri(HSA_OID);
            c.setRecorder(new Reference().setIdentifier(
                    new Identifier().setSystem(hsaSystem).setValue(sourceHsaId)));

            // extension: ext-source-system (used by SparrFilterService to identify the source)
            Extension ext = new Extension(EXT_SOURCE_SYSTEM);
            ext.setValue(new Identifier().setSystem(hsaSystem).setValue(sourceHsaId));
            c.addExtension(ext);
        }

        return c;
    }

    /** Convert RIVTA date string to ISO 8601.
     *  YYYYMMDD → YYYY-MM-DD
     *  YYYYMMDDHHmmss → YYYY-MM-DDTHH:mm:ss
     */
    private String parseRivDate(String d) {
        if (d == null || d.isBlank()) return null;
        String s = d.trim();
        if (s.length() == 8) {
            return s.substring(0, 4) + "-" + s.substring(4, 6) + "-" + s.substring(6, 8);
        }
        if (s.length() >= 14) {
            return s.substring(0, 4) + "-" + s.substring(4, 6) + "-" + s.substring(6, 8)
                    + "T" + s.substring(8, 10) + ":" + s.substring(10, 12) + ":" + s.substring(12, 14);
        }
        return s;
    }

    private CodeableConcept codeable(String system, String code) {
        return new CodeableConcept().addCoding(new Coding().setSystem(system).setCode(code));
    }

    private CodeableConcept codeableWithDisplay(String system, String code, String display) {
        return new CodeableConcept().addCoding(
                new Coding().setSystem(system).setCode(code).setDisplay(display));
    }
}
