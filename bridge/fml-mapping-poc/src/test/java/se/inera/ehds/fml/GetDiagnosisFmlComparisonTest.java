package se.inera.ehds.fml;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Provenance;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.inera.ehds.mapping.concept.ConceptMapRegistry;
import se.inera.ehds.mapping.naming.NamingSystemRegistry;
import se.inera.ehds.mapping.rivta.*;
import se.inera.ehds.mapping.tk.MapperContext;
import se.inera.ehds.mapping.tk.MappedDiagnosisEntry;
import se.inera.ehds.mapping.tk.getdiagnosis.GetDiagnosisMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comparative tests: runs the same RIVTA GetDiagnosis fixtures through (a) the production
 * Java mapper (mapping-engine) and (b) the FML StructureMap PoC, and compares the resulting
 * Condition fields. Covers the three decided error-handling behaviours from PR #38
 * (DIAG-001 stop-on-invalid-personnummer, OID-fallback, DIAG-003 data-absent-reason) plus a
 * happy path. See ig/input/pagecontent/fml-evaluation.md for the write-up these tests back.
 */
class GetDiagnosisFmlComparisonTest {

    private static GetDiagnosisMapper javaMapper;
    private static MapperContext ctx;
    private static FmlEngine fml;

    @BeforeAll
    static void setUp() throws Exception {
        javaMapper = new GetDiagnosisMapper(new NamingSystemRegistry(), new ConceptMapRegistry());
        ctx = new MapperContext(
                "http://electronichealth.se/identifier/personnummer",
                "190101011234",
                "SE2321000999-EHDS");
        fml = new FmlEngine();
        if (System.getenv("FML_DEBUG") != null) {
            System.out.println("=== rendered map ===\n" + fml.render() + "\n=== end ===");
        }
    }

    @Test
    void happyPath_sammaKodsystemOchKategori() {
        Diagnosis d = diagnosis("190101011234", "HD", "1.2.752.116.1.1.1.1.3", "J45", "Astma", "20240101", "20240601");

        Condition javaResult = runJava(d);
        Condition fmlResult = runFml(d);

        assertEquals("https://www.icd10.se/", javaResult.getCode().getCodingFirstRep().getSystem());
        assertEquals(javaResult.getCode().getCodingFirstRep().getSystem(), fmlResult.getCode().getCodingFirstRep().getSystem());
        assertEquals(javaResult.getCode().getCodingFirstRep().getCode(), fmlResult.getCode().getCodingFirstRep().getCode());
        assertEquals("HD", fmlResult.getCategoryFirstRep().getCodingFirstRep().getCode());
        assertEquals(javaResult.getCategoryFirstRep().getCodingFirstRep().getCode(),
                fmlResult.getCategoryFirstRep().getCodingFirstRep().getCode());
        assertEquals("resolved", javaResult.getClinicalStatus().getCodingFirstRep().getCode());
        assertEquals("resolved", fmlResult.getClinicalStatus().getCodingFirstRep().getCode());
    }

    @Test
    void recorderOchAsserter_matchar_java() {
        Diagnosis d = diagnosis("190101011234", "HD", "1.2.752.116.1.1.1.1.3", "J45", "Astma", "20240101", "20240601");
        withRecorderAndAsserter(d);

        Condition javaResult = runJava(d);
        Condition fmlResult = runFml(d);

        assertEquals(javaResult.getRecorder().getIdentifier().getValue(),
                fmlResult.getRecorder().getIdentifier().getValue());
        assertEquals(javaResult.getRecordedDateElement().getValueAsString(),
                fmlResult.getRecordedDateElement().getValueAsString());
        assertEquals(javaResult.getAsserter().getIdentifier().getValue(),
                fmlResult.getAsserter().getIdentifier().getValue());
        assertEquals(javaResult.getExtensionByUrl("https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-asserted-date")
                        .getValue().primitiveValue(),
                fmlResult.getExtensionByUrl("https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-asserted-date")
                        .getValue().primitiveValue());
    }

    @Test
    void chronicConditionOchRelatedDiagnosis_matchar_java() {
        Diagnosis d = diagnosis("190101011234", "HD", "1.2.752.116.1.1.1.1.3", "J45", "Astma", "20240101", null);
        d.getDiagnosisBody().setChronicCondition(Boolean.TRUE);
        RelatedDiagnosis related = new RelatedDiagnosis();
        related.setDocumentId("doc-123");
        d.getDiagnosisBody().setRelatedDiagnosis(related);

        Condition javaResult = runJava(d);
        Condition fmlResult = runFml(d);

        assertEquals(
                javaResult.getExtensionByUrl("https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-chronic-condition")
                        .getValue().primitiveValue(),
                fmlResult.getExtensionByUrl("https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-chronic-condition")
                        .getValue().primitiveValue());
        Reference javaRelated = (Reference) javaResult
                .getExtensionByUrl("https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-related-condition")
                .getValue();
        Reference fmlRelated = (Reference) fmlResult
                .getExtensionByUrl("https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-related-condition")
                .getValue();
        assertEquals(javaRelated.getIdentifier().getValue(), fmlRelated.getIdentifier().getValue());
    }

    @Test
    void provenance_treAgenter_matchar_java() {
        Diagnosis d = diagnosis("190101011234", "HD", "1.2.752.116.1.1.1.1.3", "J45", "Astma", "20240101", "20240601");
        withRecorderAndAsserter(d);

        List<MappedDiagnosisEntry> javaEntries = javaMapper.map(okResponse(d), ctx);
        assertEquals(1, javaEntries.size());
        Provenance javaProv = javaEntries.get(0).provenance();

        Parameters source = RivtaDiagnosisParametersAdapter.toParameters(d, ctx.getBridgeHsaId());
        Provenance fmlProv = fml.transformDiagnosisProvenance(source);

        assertEquals(javaProv.getRecordedElement().getValueAsString(), fmlProv.getRecordedElement().getValueAsString());
        // Fixturen sätter inget careUnitHSAId, så "author"-agenten uteblir i båda - precis
        // som ProvenanceBuilder.addAgent() hoppar över en roll med null-värde.
        assertEquals(javaProv.getAgent().size(), fmlProv.getAgent().size());
        assertEquals(agentHsaId(javaProv, "custodian"), agentHsaId(fmlProv, "custodian"));
        assertEquals(agentHsaId(javaProv, "assembler"), agentHsaId(fmlProv, "assembler"));
    }

    private String agentHsaId(Provenance prov, String role) {
        return prov.getAgent().stream()
                .filter(a -> a.getType().getCodingFirstRep().getCode().equals(role))
                .findFirst()
                .map(a -> a.getWho().getIdentifier().getValue())
                .orElse(null);
    }

    private void withRecorderAndAsserter(Diagnosis d) {
        se.inera.ehds.mapping.rivta.HealthcareProfessionalType ahp = new se.inera.ehds.mapping.rivta.HealthcareProfessionalType();
        PersonIdType recorderId = new PersonIdType();
        recorderId.setExtension("SE2321000016-REC");
        ahp.setPersonId(recorderId);
        ahp.setAuthorTime("20240101120000");
        d.getDiagnosisHeader().setAccountableHealthcareProfessional(ahp);

        se.inera.ehds.mapping.rivta.LegalAuthenticatorType la = new se.inera.ehds.mapping.rivta.LegalAuthenticatorType();
        se.inera.ehds.mapping.rivta.HealthcareProfessionalType asserterHcp = new se.inera.ehds.mapping.rivta.HealthcareProfessionalType();
        PersonIdType asserterId = new PersonIdType();
        asserterId.setExtension("SE2321000016-ASS");
        asserterHcp.setPersonId(asserterId);
        la.setHcProfessional(asserterHcp);
        la.setSignatureDate("20240102120000");
        d.getDiagnosisHeader().setLegalAuthenticator(la);
    }

    @Test
    void diag001_saknatPersonnummer_javaStopparHelaPosten_fmlStopparBaraFaltet() {
        Diagnosis d = diagnosis(null, "HD", "1.2.752.116.1.1.1.1.3", "J45", "Astma", "20240101", null);

        // Java: hela Condition+Provenance-paret filtreras bort - map() returnerar tom lista.
        GetDiagnosisResponse response = okResponse(d);
        assertEquals(List.of(), javaMapper.map(response, ctx), "Java-mappern ska stoppa hela posten");

        // FML: StructureMap-anropet transform() körs alltid på den Condition som skickats in;
        // "stoppa posten" går inte att uttrycka i en enskild regel - det kräver att anroparen
        // (gruppen/koden som itererar över diagnosisList) hoppar över posten baserat på samma
        // villkor, precis som Java-koden gör i sin stream().filter(Objects::nonNull). Detta är
        // den viktigaste skillnaden utvärderingen hittade för "stoppa resurs"-fallet.
        Condition fmlResult = runFml(d);
        assertFalse(fmlResult.hasSubject(), "utan giltigt personnummer ska subject uteblir i FML-resultatet");
    }

    @Test
    void diag003_okandDiagnosTyp_bada_ger_dataAbsentReason() {
        Diagnosis d = diagnosis("190101011234", "OKAND", "1.2.752.116.1.1.1.1.3", "J45", "Astma", "20240101", null);

        Condition javaResult = runJava(d);
        Condition fmlResult = runFml(d);

        assertDataAbsentReasonUnknown(javaResult.getCategoryFirstRep());
        assertDataAbsentReasonUnknown(fmlResult.getCategoryFirstRep());
    }

    @Test
    void okantKodverkOid_bada_ger_urnOidFallback() {
        Diagnosis d = diagnosis("190101011234", "HD", "2.16.840.1.113883.6.999", "X1", "Okänd kod", "20240101", null);

        Condition javaResult = runJava(d);
        Condition fmlResult = runFml(d);

        assertEquals("urn:oid:2.16.840.1.113883.6.999", javaResult.getCode().getCodingFirstRep().getSystem());
        assertEquals(javaResult.getCode().getCodingFirstRep().getSystem(), fmlResult.getCode().getCodingFirstRep().getSystem());
        assertEquals("X1", fmlResult.getCode().getCodingFirstRep().getCode());
    }

    private void assertDataAbsentReasonUnknown(CodeableConcept category) {
        assertTrue(category.getCoding().isEmpty(), "category ska vara utan coding");
        assertEquals(1, category.getExtension().size());
        assertEquals("http://hl7.org/fhir/StructureDefinition/data-absent-reason",
                category.getExtensionFirstRep().getUrl());
        assertEquals("unknown", category.getExtensionFirstRep().getValue().primitiveValue());
    }

    private Condition runJava(Diagnosis d) {
        List<MappedDiagnosisEntry> result = javaMapper.map(okResponse(d), ctx);
        assertEquals(1, result.size(), "förväntade exakt en mappad post från Java-mappern");
        return result.get(0).condition();
    }

    private Condition runFml(Diagnosis d) {
        Parameters source = RivtaDiagnosisParametersAdapter.toParameters(d);
        Condition result = fml.transformDiagnosis(source);
        if (System.getenv("FML_DEBUG") != null) {
            try {
                System.out.println(new org.hl7.fhir.r4.formats.JsonParser().composeString(result));
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    private GetDiagnosisResponse okResponse(Diagnosis d) {
        GetDiagnosisResponse response = new GetDiagnosisResponse();
        ResultType result = new ResultType();
        result.setResultCode("OK");
        response.setResult(result);
        response.setDiagnosis(List.of(d));
        return response;
    }

    private Diagnosis diagnosis(String personnummer, String diagnosisType, String codeSystemOid,
                                 String code, String display, String onsetStart, String onsetEnd) {
        Diagnosis d = new Diagnosis();
        DiagnosisHeader header = new DiagnosisHeader();
        if (personnummer != null) {
            PersonIdType pid = new PersonIdType();
            pid.setRoot("1.2.752.129.2.1.3.1");
            pid.setExtension(personnummer);
            header.setPatientId(pid);
        }
        header.setSourceSystemHSAId("SE2321000016-ABC");
        header.setCareProviderHSAId("SE2321000016-ABC");
        d.setDiagnosisHeader(header);

        DiagnosisBody body = new DiagnosisBody();
        body.setDiagnosisType(diagnosisType);
        CVType dc = new CVType();
        dc.setCodeSystem(codeSystemOid);
        dc.setCode(code);
        dc.setDisplayName(display);
        body.setDiagnosisCode(dc);
        DatePeriodType period = new DatePeriodType();
        period.setStart(onsetStart);
        period.setEnd(onsetEnd);
        body.setDiagnosisTimePeriod(period);
        d.setDiagnosisBody(body);
        return d;
    }
}
