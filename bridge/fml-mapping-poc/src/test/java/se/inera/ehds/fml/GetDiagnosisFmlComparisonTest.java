package se.inera.ehds.fml;

import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Provenance;
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
 * Comparative tests for the GetDiagnosis FML translation, rebuilt against the current
 * (post-PR#41) RIVTA schema and the logical-model source (see GetDiagnosisJsonSourceBuilder,
 * FmlEngine, get-diagnosis-to-condition.map / get-diagnosis-to-provenance.map).
 */
class GetDiagnosisFmlComparisonTest {

    private static GetDiagnosisMapper javaMapper;
    private static MapperContext ctx;
    private static FmlEngine fml;
    private static GetDiagnosisJsonSourceBuilder sourceBuilder;

    @BeforeAll
    static void setUp() throws Exception {
        javaMapper = new GetDiagnosisMapper(new NamingSystemRegistry(), new ConceptMapRegistry());
        ctx = new MapperContext("http://electronichealth.se/identifier/personnummer", "190101011234", "SE2321000999-EHDS");
        fml = new FmlEngine();
        sourceBuilder = new GetDiagnosisJsonSourceBuilder(new NamingSystemRegistry());
    }

    @Test
    void grundlaggandeFalt_matchar_java() throws Exception {
        Diagnosis diag = fullDiagnosis("Huvuddiagnos", true);

        Condition javaResult = runJavaCondition(diag);
        Condition fmlResult = runFmlCondition(diag);

        assertEquals(javaResult.getVerificationStatus().getCodingFirstRep().getCode(),
                fmlResult.getVerificationStatus().getCodingFirstRep().getCode());
        assertEquals("confirmed", javaResult.getVerificationStatus().getCodingFirstRep().getCode());

        assertEquals(javaResult.getClinicalStatus().getCodingFirstRep().getCode(),
                fmlResult.getClinicalStatus().getCodingFirstRep().getCode());
        assertEquals("active", javaResult.getClinicalStatus().getCodingFirstRep().getCode());

        assertEquals(javaResult.getSubject().getIdentifier().getValue(), fmlResult.getSubject().getIdentifier().getValue());
        assertEquals(javaResult.getSubject().getIdentifier().getSystem(), fmlResult.getSubject().getIdentifier().getSystem());

        assertEquals(javaResult.getOnsetDateTimeType().getValueAsString(), fmlResult.getOnsetDateTimeType().getValueAsString());
        assertEquals(javaResult.getMeta().getSource(), fmlResult.getMeta().getSource());

        assertEquals(javaResult.getRecorder().getIdentifier().getValue(), fmlResult.getRecorder().getIdentifier().getValue());
        assertEquals(javaResult.getRecordedDateElement().getValueAsString(), fmlResult.getRecordedDateElement().getValueAsString());

        assertEquals(javaResult.getAsserter().getIdentifier().getValue(), fmlResult.getAsserter().getIdentifier().getValue());
        assertEquals(
                javaResult.getExtensionByUrl("https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-asserted-date").getValue().primitiveValue(),
                fmlResult.getExtensionByUrl("https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-asserted-date").getValue().primitiveValue());

        assertEquals(javaResult.getCode().getCodingFirstRep().getSystem(), fmlResult.getCode().getCodingFirstRep().getSystem());
        assertEquals(javaResult.getCode().getCodingFirstRep().getCode(), fmlResult.getCode().getCodingFirstRep().getCode());
    }

    @Test
    void kategoriMappad_HuvuddiagnosOchBidiagnos_matchar_java() throws Exception {
        for (String typeOfDiagnosis : List.of("Huvuddiagnos", "Bidiagnos")) {
            Diagnosis diag = fullDiagnosis(typeOfDiagnosis, false);
            Condition javaResult = runJavaCondition(diag);
            Condition fmlResult = runFmlCondition(diag);
            assertEquals(javaResult.getCategoryFirstRep().getCodingFirstRep().getCode(),
                    fmlResult.getCategoryFirstRep().getCodingFirstRep().getCode(), typeOfDiagnosis);
            assertEquals(javaResult.getCategoryFirstRep().getCodingFirstRep().getSystem(),
                    fmlResult.getCategoryFirstRep().getCodingFirstRep().getSystem(), typeOfDiagnosis);
        }
    }

    @Test
    void kategoriOkandTyp_dataAbsentReason_matchar_java() throws Exception {
        Diagnosis diag = fullDiagnosis("NagonAnnanTyp", false);
        Condition javaResult = runJavaCondition(diag);
        Condition fmlResult = runFmlCondition(diag);

        String darUrl = "http://hl7.org/fhir/StructureDefinition/data-absent-reason";
        assertFalse(javaResult.getCategoryFirstRep().hasCoding());
        assertFalse(fmlResult.getCategoryFirstRep().hasCoding());
        assertEquals(javaResult.getCategoryFirstRep().getExtensionByUrl(darUrl).getValue().primitiveValue(),
                fmlResult.getCategoryFirstRep().getExtensionByUrl(darUrl).getValue().primitiveValue());
        assertEquals("unknown", javaResult.getCategoryFirstRep().getExtensionByUrl(darUrl).getValue().primitiveValue());
    }

    @Test
    void chronicDiagnosisOchRelatedDiagnosis_matchar_java() throws Exception {
        Diagnosis diag = fullDiagnosis("Huvuddiagnos", true);
        diag.getDiagnosisBody().getRelatedDiagnosis().add(relatedDiagnosis("doc-related-1"));
        diag.getDiagnosisBody().getRelatedDiagnosis().add(relatedDiagnosis("doc-related-2"));

        Condition javaResult = runJavaCondition(diag);
        Condition fmlResult = runFmlCondition(diag);

        String chronicUrl = "https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-chronic-condition";
        assertEquals(javaResult.getExtensionByUrl(chronicUrl).getValue().primitiveValue(),
                fmlResult.getExtensionByUrl(chronicUrl).getValue().primitiveValue());
        assertEquals("true", javaResult.getExtensionByUrl(chronicUrl).getValue().primitiveValue());

        String relatedUrl = "https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-related-condition";
        List<org.hl7.fhir.r4.model.Extension> javaRelated = javaResult.getExtensionsByUrl(relatedUrl);
        List<org.hl7.fhir.r4.model.Extension> fmlRelated = fmlResult.getExtensionsByUrl(relatedUrl);
        assertEquals(javaRelated.size(), fmlRelated.size());
        assertEquals(2, javaRelated.size());
        for (int i = 0; i < javaRelated.size(); i++) {
            org.hl7.fhir.r4.model.Reference javaRef = (org.hl7.fhir.r4.model.Reference) javaRelated.get(i).getValue();
            org.hl7.fhir.r4.model.Reference fmlRef = (org.hl7.fhir.r4.model.Reference) fmlRelated.get(i).getValue();
            assertEquals(javaRef.getIdentifier().getValue(), fmlRef.getIdentifier().getValue());
        }
    }

    @Test
    void diagnoskod_kandOchOkandOid_matchar_java() throws Exception {
        for (String oid : List.of("1.2.752.116.1.1.1.1.3", "9.9.9.9.9.9")) {
            Diagnosis diag = fullDiagnosis("Huvuddiagnos", false);
            diag.getDiagnosisBody().getDiagnosisCode().setCodeSystem(oid);
            Condition javaResult = runJavaCondition(diag);
            Condition fmlResult = runFmlCondition(diag);
            assertEquals(javaResult.getCode().getCodingFirstRep().getSystem(),
                    fmlResult.getCode().getCodingFirstRep().getSystem(), oid);
            assertEquals(javaResult.getCode().getCodingFirstRep().getCode(),
                    fmlResult.getCode().getCodingFirstRep().getCode(), oid);
        }
    }

    @Test
    void stoppaResursen_felaktigtPersonnummer_ingenResursAlls() throws Exception {
        // DIAG-001: Java-mappern returnerar null för hela posten. Till skillnad från den
        // tidigare Parameters-baserade varianten (som bara kunde hoppa över subject-fältet)
        // kan FmlEngine nu stoppa HELA anropet via registry-postens FHIRPath-guard - samma
        // beteende som Java, inte en kompromiss. Se FmlEngine-javadoc.
        Diagnosis diag = fullDiagnosis("Huvuddiagnos", true);
        diag.getDiagnosisHeader().getPatientId().setId("fel-format");

        List<MappedDiagnosisEntry> javaResult = javaMapper.map(okResponse(diag), ctx);
        assertTrue(javaResult.isEmpty(), "Java ska returnera en tom lista för ogiltigt personnummer");

        Base fmlResult = fml.transform("GetDiagnosisToCondition", parseSource(diag));
        assertNull(fmlResult, "FmlEngine ska returnera null (stoppa resursen) för ogiltigt personnummer, precis som Java");
    }

    @Test
    void provenance_tvaAgenter_matchar_java() throws Exception {
        Diagnosis diag = fullDiagnosis("Huvuddiagnos", true);

        MappedDiagnosisEntry javaEntry = javaMapper.map(okResponse(diag), ctx).get(0);
        Provenance javaProv = javaEntry.provenance();

        Base provenanceSource = fml.parseSource("/fhir/lm-diagnosis.json", sourceBuilder.toJson(diag, true));
        Provenance fmlProv = (Provenance) fml.transform("GetDiagnosisToProvenance", provenanceSource);

        assertEquals(agentHsaId(javaProv, "custodian"), agentHsaId(fmlProv, "custodian"));
        assertEquals(agentHsaId(javaProv, "author"), agentHsaId(fmlProv, "author"));
        assertEquals(javaProv.getRecordedElement().getValueAsString(), fmlProv.getRecordedElement().getValueAsString());
    }

    private String agentHsaId(Provenance prov, String role) {
        return prov.getAgent().stream()
                .filter(a -> a.getType().getCodingFirstRep().getCode().equals(role))
                .findFirst()
                .map(a -> a.getWho().getIdentifier().getValue())
                .orElse(null);
    }

    private Condition runJavaCondition(Diagnosis diag) {
        List<MappedDiagnosisEntry> result = javaMapper.map(okResponse(diag), ctx);
        assertEquals(1, result.size(), "förväntade exakt en mappad post från Java-mappern");
        return result.get(0).condition();
    }

    private Condition runFmlCondition(Diagnosis diag) throws Exception {
        return (Condition) fml.transform("GetDiagnosisToCondition", parseSource(diag));
    }

    private Base parseSource(Diagnosis diag) throws Exception {
        return fml.parseSource("/fhir/lm-diagnosis.json", sourceBuilder.toJson(diag));
    }

    private GetDiagnosisResponse okResponse(Diagnosis diag) {
        GetDiagnosisResponse response = new GetDiagnosisResponse();
        ResultType result = new ResultType();
        result.setResultCode("OK");
        response.setResult(result);
        response.setDiagnosis(List.of(diag));
        return response;
    }

    private RelatedDiagnosis relatedDiagnosis(String documentId) {
        RelatedDiagnosis rel = new RelatedDiagnosis();
        rel.setDocumentId(documentId);
        return rel;
    }

    private Diagnosis fullDiagnosis(String typeOfDiagnosis, boolean chronic) {
        Diagnosis diag = new Diagnosis();

        DiagnosisHeader header = new DiagnosisHeader();
        header.setDocumentId("doc-1");
        header.setSourceSystemHSAId("SE2321000016-ABC");

        PersonIdType patientId = new PersonIdType();
        patientId.setId("190101011234");
        patientId.setType("1.2.752.129.2.1.3.1");
        header.setPatientId(patientId);

        HealthcareProfessionalType ahp = new HealthcareProfessionalType();
        ahp.setAuthorTime("20240101120000");
        ahp.setHealthcareProfessionalHSAId("SE2321000016-REC");
        ahp.setHealthcareProfessionalCareUnitHSAId("SE2321000016-ENHET");
        ahp.setHealthcareProfessionalCareGiverHSAId("SE2321000016-ABC");
        header.setAccountableHealthcareProfessional(ahp);

        LegalAuthenticatorType la = new LegalAuthenticatorType();
        la.setSignatureTime("20240102120000");
        la.setLegalAuthenticatorHSAId("SE2321000016-ASS");
        header.setLegalAuthenticator(la);

        diag.setDiagnosisHeader(header);

        DiagnosisBody body = new DiagnosisBody();
        body.setTypeOfDiagnosis(typeOfDiagnosis);
        body.setChronicDiagnosis(chronic);
        body.setDiagnosisTime("20240103120000");
        CVType dc = new CVType();
        dc.setCode("A09");
        dc.setCodeSystem("1.2.752.116.1.1.1.1.3");
        dc.setDisplayName("Diarre");
        body.setDiagnosisCode(dc);
        diag.setDiagnosisBody(body);

        return diag;
    }
}
