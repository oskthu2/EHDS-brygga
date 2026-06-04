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
    private static final String CLIN_STATUS_SYS = "http://terminology.hl7.org/CodeSystem/condition-clinical";
    private static final String VER_STATUS_SYS = "http://terminology.hl7.org/CodeSystem/condition-ver-status";
    private static final String EXT_ASSERTED_DATE  = CANONICAL_BASE + "/StructureDefinition/ext-asserted-date";
    private static final String PROFILE_URL = CANONICAL_BASE + "/StructureDefinition/se-ehds-condition";
    private static final String PROFILE_URL_EU_EPS =
            "http://hl7.eu/fhir/eps/StructureDefinition/condition-obl-eu-eps";

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

        Condition c = new Condition();
        c.setId(UUID.randomUUID().toString());
        c.getMeta().addProfile(PROFILE_URL);
        c.getMeta().addProfile(PROFILE_URL_EU_EPS);

        // clinicalStatus: active unless there is an end date
        boolean resolved = body.getDiagnosisTimePeriod() != null
                && body.getDiagnosisTimePeriod().getEnd() != null;
        c.setClinicalStatus(codeable(CLIN_STATUS_SYS, resolved ? "resolved" : "active"));

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

        // recordedDate from documentTime
        if (header.getDocumentTime() != null) {
            c.setRecordedDateElement(new DateTimeType(RivDateParser.parse(header.getDocumentTime())));
        }

        String hsaSystem = namingSystem.oidToUri(HSA_OID_INERA);

        // meta.source: sourceSystemHSAId as URI (urn:oid:{HSA_OID}#{hsaId})
        String sourceHsaId = header.getSourceSystemHSAId();
        if (sourceHsaId != null) {
            c.getMeta().setSource(hsaSystem + "#" + sourceHsaId);
        }

        // ext-asserted-date: EPS extension – administrative assertion date (author-time)
        if (body.getAssertedDate() != null) {
            Extension extAd = new Extension(EXT_ASSERTED_DATE);
            extAd.setValue(new DateTimeType(RivDateParser.parse(body.getAssertedDate())));
            c.addExtension(extAd);
        }

        Provenance prov = ProvenanceBuilder.build(
                c.getId(),
                header.getCareProviderHSAId(),
                header.getCareUnitHSAId(),
                header.getDocumentTime(),
                hsaSystem,
                ctx);

        return new MappedDiagnosisEntry(c, prov);
    }

    private CodeableConcept codeable(String system, String code) {
        return new CodeableConcept().addCoding(new Coding().setSystem(system).setCode(code));
    }

    private CodeableConcept codeableWithDisplay(String system, String code, String display) {
        return new CodeableConcept().addCoding(
                new Coding().setSystem(system).setCode(code).setDisplay(display));
    }
}
