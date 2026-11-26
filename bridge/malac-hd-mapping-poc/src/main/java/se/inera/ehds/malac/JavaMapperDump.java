package se.inera.ehds.malac;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Provenance;
import se.inera.ehds.mapping.concept.ConceptMapRegistry;
import se.inera.ehds.mapping.naming.NamingSystemRegistry;
import se.inera.ehds.mapping.rivta.*;
import se.inera.ehds.mapping.tk.MappedDiagnosisEntry;
import se.inera.ehds.mapping.tk.MapperContext;
import se.inera.ehds.mapping.tk.getdiagnosis.GetDiagnosisMapper;

import java.util.List;

/**
 * Comparison-harness helper only: builds the SAME two GetDiagnosisResponse fixtures the
 * MaLaC-HD/Python track exercises (see ../../python/, testmessages/*.xml) as POJOs - the
 * actual RIVTA model classes in se.inera.ehds.mapping.rivta have no XML (de)serialization of
 * their own (see GetDiagnosisMapperTest.java, which builds every fixture the same way) - runs
 * them through the unmodified production GetDiagnosisMapper, and serializes the result to
 * FHIR JSON with the same HAPI JsonParser the real server uses. Produces NOTHING the
 * production mapper itself doesn't already produce; this class only dumps it to a file for
 * run_comparison.py to diff against the MaLaC-HD-generated output.
 */
public final class JavaMapperDump {

    private JavaMapperDump() {}

    /** Mirrors testmessages/get-diagnosis-huvuddiagnos.xml field for field. */
    public static GetDiagnosisResponse huvuddiagnosFixture() {
        DiagnosisHeader header = new DiagnosisHeader();
        header.setDocumentId("doc-1");
        header.setSourceSystemHSAId("SE2321000016-SRC1");

        PersonIdType pid = new PersonIdType();
        pid.setId("191212121212");
        pid.setType("1.2.752.129.2.1.3.1");
        header.setPatientId(pid);

        HealthcareProfessionalType ahp = new HealthcareProfessionalType();
        ahp.setAuthorTime("20240315090000");
        ahp.setHealthcareProfessionalHSAId("SE2321000016-4HK5");
        ahp.setHealthcareProfessionalName("Anna Andersson");
        ahp.setHealthcareProfessionalCareUnitHSAId("SE2321000016-1001");
        ahp.setHealthcareProfessionalCareGiverHSAId("SE2321000016-CG99");
        header.setAccountableHealthcareProfessional(ahp);

        LegalAuthenticatorType la = new LegalAuthenticatorType();
        la.setSignatureTime("20240315091500");
        la.setLegalAuthenticatorHSAId("SE2321000016-AUTH");
        la.setLegalAuthenticatorName("Bo Bengtsson");
        header.setLegalAuthenticator(la);

        DiagnosisBody body = new DiagnosisBody();
        body.setTypeOfDiagnosis("Huvuddiagnos");
        body.setChronicDiagnosis(true);
        body.setDiagnosisTime("20240310080000");

        CVType cv = new CVType();
        cv.setCode("J45.9");
        cv.setCodeSystem("1.2.752.116.1.1.1.1.3");
        cv.setCodeSystemName("ICD-10-SE");
        cv.setDisplayName("Astma, ospecificerad");
        body.setDiagnosisCode(cv);

        RelatedDiagnosis related = new RelatedDiagnosis();
        related.setDocumentId("doc-0");
        body.setRelatedDiagnosis(List.of(related));

        Diagnosis diag = new Diagnosis();
        diag.setDiagnosisHeader(header);
        diag.setDiagnosisBody(body);

        ResultType result = new ResultType();
        result.setResultCode("OK");

        GetDiagnosisResponse response = new GetDiagnosisResponse();
        response.setDiagnosis(List.of(diag));
        response.setResult(result);
        return response;
    }

    /** Mirrors testmessages/get-diagnosis-ogiltigt-personnummer.xml: same as above but with
     * an invalid (non-12-digit) personnummer, which must stop the whole record (DIAG-001). */
    public static GetDiagnosisResponse ogiltigtPersonnummerFixture() {
        GetDiagnosisResponse response = huvuddiagnosFixture();
        Diagnosis diag = response.getDiagnosis().get(0);
        diag.getDiagnosisHeader().getPatientId().setId("ABC123");
        diag.getDiagnosisBody().setTypeOfDiagnosis("Bidiagnos");
        diag.getDiagnosisBody().setChronicDiagnosis(false);
        return response;
    }

    public static MapperContext defaultContext() {
        return new MapperContext(
                "http://electronichealth.se/identifier/personnummer",
                "190101011234",
                "SE2321000999-EHDS");
    }

    public static List<MappedDiagnosisEntry> map(GetDiagnosisResponse response) {
        NamingSystemRegistry namingSystem = new NamingSystemRegistry();
        ConceptMapRegistry conceptMaps = new ConceptMapRegistry();
        GetDiagnosisMapper mapper = new GetDiagnosisMapper(namingSystem, conceptMaps);
        return mapper.map(response, defaultContext());
    }

    /** JSON array of {"condition": ..., "provenance": ...} objects, one per mapped entry -
     * same shape as run_pipeline.py's output, for a direct diff. */
    public static String mapToJson(GetDiagnosisResponse response) {
        FhirContext ctx = FhirContext.forR4();
        IParser parser = ctx.newJsonParser().setPrettyPrint(true);
        List<MappedDiagnosisEntry> entries = map(response);

        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < entries.size(); i++) {
            MappedDiagnosisEntry entry = entries.get(i);
            Condition condition = entry.condition();
            Provenance provenance = entry.provenance();
            sb.append("  {\n");
            sb.append("    \"condition\": ").append(indent(parser.encodeResourceToString(condition), 4)).append(",\n");
            sb.append("    \"provenance\": ").append(indent(parser.encodeResourceToString(provenance), 4)).append("\n");
            sb.append("  }");
            if (i < entries.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]\n");
        return sb.toString();
    }

    private static String indent(String json, int spaces) {
        String pad = " ".repeat(spaces);
        return json.replace("\n", "\n" + pad);
    }
}
