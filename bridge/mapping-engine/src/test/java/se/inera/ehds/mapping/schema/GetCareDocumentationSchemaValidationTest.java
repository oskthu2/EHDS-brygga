package se.inera.ehds.mapping.schema;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import org.junit.jupiter.api.Test;
import se.inera.ehds.mapping.rivta.caredocumentation.*;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Validates that marshalling our GetCareDocumentation JAXB model produces XML that is
 * schema-valid against the real, published RIVTA GetCareDocumentationResponder:3 XSD
 * (urn:riv:clinicalprocess:healthcond:description:GetCareDocumentationResponder:3).
 *
 * The schema bundled under src/test/resources/riv-schemas/ was downloaded from Inera/SKLTP's
 * open-source "GetAggregatedCareDocumentation.v3" repository
 * (github.com/skltp-aggregerandetjanster), which vendors the producer contract's own
 * core_components + interaction XSDs unmodified. See CoreNamespace.java for the GetDiagnosis
 * equivalent, which could not be located and is therefore not covered here.
 *
 * This test exists so a future regression in namespace handling, field naming or element
 * ordering in the JAXB model fails CI instead of silently producing invalid test data again
 * (the original issue this test guards against).
 */
class GetCareDocumentationSchemaValidationTest {

    private static final String RESPONDER_XSD =
            "riv-schemas/clinicalprocess_healthcond_description_3.0/interactions/"
                    + "GetCareDocumentationInteraction/GetCareDocumentationResponder_3.0.xsd";

    @Test
    void marshallad_getCareDocumentationResponse_validerar_mot_officiellt_xsd() throws Exception {
        GetCareDocumentationResponse response = sampleResponse();

        String xml = marshal(response);
        Validator validator = loadResponderSchema().newValidator();
        assertDoesNotThrow(() -> validator.validate(new StreamSource(new java.io.StringReader(xml))),
                "Marshalled GetCareDocumentationResponse must validate against the official RIVTA XSD:\n" + xml);
    }

    private String marshal(GetCareDocumentationResponse response) throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(GetCareDocumentationResponse.class);
        Marshaller marshaller = ctx.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        marshaller.marshal(response, out);
        return out.toString(java.nio.charset.StandardCharsets.UTF_8);
    }

    private Schema loadResponderSchema() throws Exception {
        URL resource = getClass().getClassLoader().getResource(RESPONDER_XSD);
        if (resource == null) {
            throw new IllegalStateException("Missing bundled XSD: " + RESPONDER_XSD);
        }
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        return factory.newSchema(new File(resource.toURI()));
    }

    /** One careDocumentation with fritext, one with multimedia, plus hasMore and result. */
    private GetCareDocumentationResponse sampleResponse() {
        PersonIdType patientId = iid("1.2.752.129.2.1.3.1", "191212121212");
        PersonIdType hsa = iid("1.2.752.129.2.1.4.1", "SE2321000016-4HK5");

        AccessControlHeader ach = new AccessControlHeader();
        ach.setAccountableHealthcareProvider(hsa);
        ach.setAccountableCareUnit(hsa);
        ach.setPatientId(patientId);
        ach.setCareProcessId(iid("1.2.752.129.2.1.4.1", "process-1"));
        ach.setBlockComparisonTime("20240315090000");
        ach.setApprovedForPatient(true);

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

        CVType noteCode = new CVType();
        noteCode.setCode("ins");
        noteCode.setCodeSystem("1.2.752.129.2.2.2.11");
        noteCode.setDisplayName("Inskrivningsanteckning");

        Body body = new Body();
        body.setClinicalDocumentNoteCode(noteCode);
        body.setClinicalDocumentNoteTitle("Inskrivningsanteckning akutmottagning");
        body.setClinicalDocumentNoteText("Patienten mår bra.");

        CareDocumentation textEntry = new CareDocumentation();
        textEntry.setHeader(header);
        textEntry.setBody(body);

        MultimediaEntry media = new MultimediaEntry();
        media.setMediaType("application/pdf");
        media.setReference("https://producent.example/doc/123");

        Body mediaBody = new Body();
        mediaBody.setClinicalDocumentNoteCode(noteCode);
        mediaBody.setMultimediaEntry(media);

        CareDocumentation mediaEntry = new CareDocumentation();
        mediaEntry.setHeader(header);
        mediaEntry.setBody(mediaBody);

        HasMore hasMore = new HasMore();
        hasMore.setLogicalAddress("SE2321000016-4HK5");
        hasMore.setReference("page-2");

        ResultType result = new ResultType();
        result.setResultCode("OK");

        GetCareDocumentationResponse response = new GetCareDocumentationResponse();
        response.setCareDocumentation(List.of(textEntry, mediaEntry));
        response.setHasMore(List.of(hasMore));
        response.setResult(result);
        return response;
    }

    private static PersonIdType iid(String root, String extension) {
        PersonIdType id = new PersonIdType();
        id.setRoot(root);
        id.setExtension(extension);
        return id;
    }
}
