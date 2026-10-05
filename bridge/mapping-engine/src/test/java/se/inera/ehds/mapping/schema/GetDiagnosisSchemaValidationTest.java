package se.inera.ehds.mapping.schema;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import org.junit.jupiter.api.Test;
import se.inera.ehds.mapping.rivta.*;

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
 * Validates that marshalling our GetDiagnosis JAXB model produces XML that is
 * schema-valid against the real, published RIVTA GetDiagnosisResponder:2 XSD
 * (urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2).
 *
 * GetDiagnosis:2 lives in the same RIVTA domain repo as GetCareDocumentation
 * (riv.clinicalprocess.healthcond.description on Bitbucket), not in
 * clinicalprocess:activity:conditions as an earlier, unverified version of the
 * domain model guessed by analogy — see CoreNamespace.java.
 *
 * This test exists so a future regression in namespace handling, field naming or element
 * ordering in the JAXB model fails CI instead of silently producing invalid test data again.
 */
class GetDiagnosisSchemaValidationTest {

    private static final String RESPONDER_XSD =
            "riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/"
                    + "GetDiagnosisInteraction/GetDiagnosisResponder_2.0.xsd";

    @Test
    void marshallad_getDiagnosisResponse_validerar_mot_officiellt_xsd() throws Exception {
        GetDiagnosisResponse response = sampleResponse();

        String xml = marshal(response);
        Validator validator = loadResponderSchema().newValidator();
        assertDoesNotThrow(() -> validator.validate(new StreamSource(new java.io.StringReader(xml))),
                "Marshalled GetDiagnosisResponse must validate against the official RIVTA XSD:\n" + xml);
    }

    private String marshal(GetDiagnosisResponse response) throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(GetDiagnosisResponse.class);
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

    /** One diagnosis with a full header/body, plus result. */
    private GetDiagnosisResponse sampleResponse() {
        PersonIdType patientId = new PersonIdType();
        patientId.setId("191212121212");
        patientId.setType("1.2.752.129.2.1.3.1");

        CVType roleCode = new CVType();
        roleCode.setCode("102510");
        roleCode.setCodeSystem("1.2.752.129.2.2.1.4");
        roleCode.setCodeSystemName("Befattning");
        roleCode.setCodeSystemVersion("3.1");
        roleCode.setDisplayName("Ledning, tandvård");

        OrgUnit orgUnit = new OrgUnit();
        orgUnit.setOrgUnitHSAId("9999");
        orgUnit.setOrgUnitName("Karolinska Huddinge");
        orgUnit.setOrgUnitTelecom("08-1234567");
        orgUnit.setOrgUnitEmail("info@example.se");
        orgUnit.setOrgUnitAddress("Huddinge");
        orgUnit.setOrgUnitLocation("Plan 3");

        HealthcareProfessionalType ahp = new HealthcareProfessionalType();
        ahp.setAuthorTime("20160902111111");
        ahp.setHealthcareProfessionalHSAId("12345");
        ahp.setHealthcareProfessionalName("Anna Andersson");
        ahp.setHealthcareProfessionalRoleCode(roleCode);
        ahp.setHealthcareProfessionalOrgUnit(orgUnit);
        ahp.setHealthcareProfessionalCareUnitHSAId("565656");
        ahp.setHealthcareProfessionalCareGiverHSAId("SE2321000016-4HK5");

        LegalAuthenticatorType la = new LegalAuthenticatorType();
        la.setSignatureTime("20160902111111");
        la.setLegalAuthenticatorHSAId("SE2321000016-AUTH");
        la.setLegalAuthenticatorName("Bo Bengtsson");

        DiagnosisHeader header = new DiagnosisHeader();
        header.setDocumentId("doc-1");
        header.setSourceSystemHSAId("SE2321000016-4HK5");
        header.setDocumentTitle("Diagnosjournal");
        header.setDocumentTime("20160902111111");
        header.setPatientId(patientId);
        header.setAccountableHealthcareProfessional(ahp);
        header.setLegalAuthenticator(la);
        header.setApprovedForPatient(true);
        header.setCareContactId("contact-1");

        CVType diagnosisCode = new CVType();
        diagnosisCode.setCode("123456");
        diagnosisCode.setCodeSystem("1.2.752.129.2.2.1.4");
        diagnosisCode.setCodeSystemName("KSH97");
        diagnosisCode.setDisplayName("Någon");

        RelatedDiagnosis related = new RelatedDiagnosis();
        related.setDocumentId("doc-0");

        DiagnosisBody body = new DiagnosisBody();
        body.setTypeOfDiagnosis("Huvuddiagnos");
        body.setChronicDiagnosis(true);
        body.setDiagnosisTime("20160902111111");
        body.setDiagnosisCode(diagnosisCode);
        body.setRelatedDiagnosis(List.of(related));

        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setDiagnosisHeader(header);
        diagnosis.setDiagnosisBody(body);

        ResultType result = new ResultType();
        result.setResultCode("OK");
        result.setLogId("1");

        GetDiagnosisResponse response = new GetDiagnosisResponse();
        response.setDiagnosis(List.of(diagnosis));
        response.setResult(result);
        return response;
    }
}
