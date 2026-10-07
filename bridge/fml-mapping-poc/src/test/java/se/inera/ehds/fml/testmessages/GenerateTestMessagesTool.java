package se.inera.ehds.fml.testmessages;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import se.inera.ehds.mapping.rivta.CVType;
import se.inera.ehds.mapping.rivta.Diagnosis;
import se.inera.ehds.mapping.rivta.DiagnosisBody;
import se.inera.ehds.mapping.rivta.DiagnosisHeader;
import se.inera.ehds.mapping.rivta.GetDiagnosisResponse;
import se.inera.ehds.mapping.rivta.HealthcareProfessionalType;
import se.inera.ehds.mapping.rivta.LegalAuthenticatorType;
import se.inera.ehds.mapping.rivta.PersonIdType;
import se.inera.ehds.mapping.rivta.RelatedDiagnosis;
import se.inera.ehds.mapping.rivta.ResultType;
import se.inera.ehds.mapping.rivta.caredocumentation.AccessControlHeader;
import se.inera.ehds.mapping.rivta.caredocumentation.Author;
import se.inera.ehds.mapping.rivta.caredocumentation.Body;
import se.inera.ehds.mapping.rivta.caredocumentation.CareDocumentation;
import se.inera.ehds.mapping.rivta.caredocumentation.GetCareDocumentationResponse;
import se.inera.ehds.mapping.rivta.caredocumentation.Header;
import se.inera.ehds.mapping.rivta.caredocumentation.MultimediaEntry;
import se.inera.ehds.mapping.rivta.caredocumentation.RecordType;
import se.inera.ehds.mapping.rivta.caredocumentation.Signature;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Not a test — a standalone generator for {@code FmlTestClient}'s bundled SOAP test messages
 * under {@code fml-mapping-poc/src/main/resources/testmessages/}. Marshals real RIVTA JAXB
 * objects (same classes {@code GetDiagnosisSchemaValidationTest}/
 * {@code GetCareDocumentationSchemaValidationTest} validate against the official XSDs) rather
 * than hand-writing XML, so the fixtures are guaranteed schema-shaped.
 *
 * Run manually with {@code mvn -pl fml-mapping-poc test-compile exec:java
 * -Dexec.mainClass=se.inera.ehds.fml.testmessages.GenerateTestMessagesTool
 * -Dexec.classpathScope=test} whenever the fixtures need regenerating (e.g. after a schema
 * change) — not wired into the build or run by surefire.
 */
public final class GenerateTestMessagesTool {

    public static void main(String[] args) throws Exception {
        Path targetDir = Path.of("src/main/resources/testmessages");
        Files.createDirectories(targetDir);

        write(targetDir.resolve("get-diagnosis-huvuddiagnos.xml"),
                marshal(diagnosisResponse("191212121212", "Huvuddiagnos", true)));
        write(targetDir.resolve("get-diagnosis-ogiltigt-personnummer.xml"),
                marshal(diagnosisResponse("ABC123", "Bidiagnos", false)));
        write(targetDir.resolve("get-caredocumentation-plattfritext.xml"),
                marshal(careDocumentationResponse(plainTextBody())));
        write(targetDir.resolve("get-caredocumentation-multimedia.xml"),
                marshal(careDocumentationResponse(multimediaBody())));

        System.out.println("Skrev testmeddelanden till " + targetDir.toAbsolutePath());
    }

    private static GetDiagnosisResponse diagnosisResponse(String patientId, String typeOfDiagnosis, boolean chronic) {
        PersonIdType pid = new PersonIdType();
        pid.setId(patientId);
        pid.setType("1.2.752.129.2.1.3.1");

        HealthcareProfessionalType ahp = new HealthcareProfessionalType();
        ahp.setAuthorTime("20240315090000");
        ahp.setHealthcareProfessionalHSAId("SE2321000016-4HK5");
        ahp.setHealthcareProfessionalName("Anna Andersson");
        ahp.setHealthcareProfessionalCareUnitHSAId("SE2321000016-1001");
        ahp.setHealthcareProfessionalCareGiverHSAId("SE2321000016-4HK5");

        LegalAuthenticatorType la = new LegalAuthenticatorType();
        la.setSignatureTime("20240315091500");
        la.setLegalAuthenticatorHSAId("SE2321000016-AUTH");
        la.setLegalAuthenticatorName("Bo Bengtsson");

        DiagnosisHeader header = new DiagnosisHeader();
        header.setDocumentId("doc-1");
        header.setSourceSystemHSAId("SE2321000016-4HK5");
        header.setPatientId(pid);
        header.setAccountableHealthcareProfessional(ahp);
        header.setLegalAuthenticator(la);

        CVType diagnosisCode = new CVType();
        diagnosisCode.setCode("J45.9");
        diagnosisCode.setCodeSystem("1.2.752.116.1.1.1.1.3");
        diagnosisCode.setCodeSystemName("ICD-10-SE");
        diagnosisCode.setDisplayName("Astma, ospecificerad");

        RelatedDiagnosis related = new RelatedDiagnosis();
        related.setDocumentId("doc-0");

        DiagnosisBody body = new DiagnosisBody();
        body.setTypeOfDiagnosis(typeOfDiagnosis);
        body.setChronicDiagnosis(chronic);
        body.setDiagnosisTime("20240315090000");
        body.setDiagnosisCode(diagnosisCode);
        body.setRelatedDiagnosis(List.of(related));

        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setDiagnosisHeader(header);
        diagnosis.setDiagnosisBody(body);

        ResultType result = new ResultType();
        result.setResultCode("OK");

        GetDiagnosisResponse response = new GetDiagnosisResponse();
        response.setDiagnosis(List.of(diagnosis));
        response.setResult(result);
        return response;
    }

    private static Body plainTextBody() {
        se.inera.ehds.mapping.rivta.caredocumentation.CVType noteCode =
                new se.inera.ehds.mapping.rivta.caredocumentation.CVType();
        noteCode.setCode("ins");
        noteCode.setCodeSystem("1.2.752.129.2.2.2.11");
        noteCode.setDisplayName("Inskrivningsanteckning");

        Body body = new Body();
        body.setClinicalDocumentNoteCode(noteCode);
        body.setClinicalDocumentNoteTitle("Inskrivningsanteckning akutmottagning");
        body.setClinicalDocumentNoteText("Patienten mår bra, ingen åtgärd krävs.");
        return body;
    }

    private static Body multimediaBody() {
        se.inera.ehds.mapping.rivta.caredocumentation.CVType noteCode =
                new se.inera.ehds.mapping.rivta.caredocumentation.CVType();
        noteCode.setCode("rtg");
        noteCode.setCodeSystem("1.2.752.129.2.2.2.11");
        noteCode.setDisplayName("Röntgenbild");

        MultimediaEntry media = new MultimediaEntry();
        media.setMediaType("application/pdf");
        media.setReference("https://producent.example/doc/123");

        Body body = new Body();
        body.setClinicalDocumentNoteCode(noteCode);
        body.setClinicalDocumentNoteTitle("Lungröntgen");
        body.setMultimediaEntry(media);
        return body;
    }

    private static GetCareDocumentationResponse careDocumentationResponse(Body body) {
        se.inera.ehds.mapping.rivta.caredocumentation.PersonIdType patientId =
                iid("1.2.752.129.2.1.3.1", "191212121212");
        se.inera.ehds.mapping.rivta.caredocumentation.PersonIdType hsa =
                iid("1.2.752.129.2.1.4.1", "SE2321000016-4HK5");

        AccessControlHeader ach = new AccessControlHeader();
        ach.setAccountableHealthcareProvider(hsa);
        ach.setAccountableCareUnit(hsa);
        ach.setPatientId(patientId);
        ach.setCareProcessId(iid("1.2.752.129.2.1.4.1", "process-1"));
        ach.setBlockComparisonTime("20240315090000");

        RecordType record = new RecordType();
        record.setId(iid("SE2321000016-4HK5", "rec-001"));
        record.setTimestamp("20240315090000");

        Author author = new Author();
        author.setId(hsa);
        author.setName("Anna Andersson");
        author.setTimestamp("20240315090000");

        Signature signature = new Signature();
        signature.setId(hsa);
        signature.setName("Bo Bengtsson");
        signature.setTimestamp("20240315091500");

        Header header = new Header();
        header.setAccessControlHeader(ach);
        header.setSourceSystemId(hsa);
        header.setRecord(record);
        header.setAuthor(author);
        header.setSignature(signature);

        CareDocumentation entry = new CareDocumentation();
        entry.setHeader(header);
        entry.setBody(body);

        se.inera.ehds.mapping.rivta.caredocumentation.ResultType result =
                new se.inera.ehds.mapping.rivta.caredocumentation.ResultType();
        result.setResultCode("OK");

        GetCareDocumentationResponse response = new GetCareDocumentationResponse();
        response.setCareDocumentation(List.of(entry));
        response.setResult(result);
        return response;
    }

    private static se.inera.ehds.mapping.rivta.caredocumentation.PersonIdType iid(String root, String extension) {
        se.inera.ehds.mapping.rivta.caredocumentation.PersonIdType id =
                new se.inera.ehds.mapping.rivta.caredocumentation.PersonIdType();
        id.setRoot(root);
        id.setExtension(extension);
        return id;
    }

    private static String marshal(Object response) throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(response.getClass());
        Marshaller marshaller = ctx.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        marshaller.marshal(response, out);
        return out.toString(StandardCharsets.UTF_8);
    }

    private static void write(Path path, String xml) throws IOException {
        Files.writeString(path, xml, StandardCharsets.UTF_8);
    }
}
