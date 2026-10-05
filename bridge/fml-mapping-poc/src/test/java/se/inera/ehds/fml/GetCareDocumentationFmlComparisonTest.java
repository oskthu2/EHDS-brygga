package se.inera.ehds.fml;

import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.inera.ehds.mapping.naming.NamingSystemRegistry;
import se.inera.ehds.mapping.rivta.caredocumentation.*;
import se.inera.ehds.mapping.tk.MapperContext;
import se.inera.ehds.mapping.tk.MappedDocumentEntry;
import se.inera.ehds.mapping.tk.getcaredocumentation.GetCareDocumentationMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comparative tests for the GetCareDocumentation FML translation: status, masterIdentifier,
 * date, meta.source, subject, context.related, blockComparisonTime, type, description,
 * plain-text content, author and authenticator/signatureTime. See
 * RivtaCareDocumentationParametersAdapter and ig/input/pagecontent/fml-evaluation.md for what
 * is explicitly NOT covered (DocBook narrative/Composition, multimediaEntry, dissentingOpinion,
 * approvedForPatient) and why.
 */
class GetCareDocumentationFmlComparisonTest {

    private static GetCareDocumentationMapper javaMapper;
    private static MapperContext ctx;
    private static FmlEngine fml;

    @BeforeAll
    static void setUp() throws Exception {
        javaMapper = new GetCareDocumentationMapper(new NamingSystemRegistry());
        ctx = new MapperContext(
                "http://electronichealth.se/identifier/personnummer",
                "190101011234",
                "SE2321000999-EHDS");
        fml = new FmlEngine();
    }

    @Test
    void plattFritext_alleRakaFalt_matchar_java() {
        CareDocumentation entry = fullEntry("Det här är fritext utan DocBook-struktur.");

        DocumentReference javaResult = runJava(entry);
        DocumentReference fmlResult = runFml(entry);

        assertEquals("current", javaResult.getStatus().toCode());
        assertEquals(javaResult.getStatus(), fmlResult.getStatus());

        assertEquals(javaResult.getMasterIdentifier().getValue(), fmlResult.getMasterIdentifier().getValue());
        assertEquals(javaResult.getDateElement().getValueAsString(), fmlResult.getDateElement().getValueAsString());
        assertEquals(javaResult.getMeta().getSource(), fmlResult.getMeta().getSource());

        assertEquals(javaResult.getSubject().getIdentifier().getValue(), fmlResult.getSubject().getIdentifier().getValue());
        assertEquals(javaResult.getContext().getRelatedFirstRep().getIdentifier().getValue(),
                fmlResult.getContext().getRelatedFirstRep().getIdentifier().getValue());
        assertEquals(
                javaResult.getExtensionByUrl("https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-block-comparison-time")
                        .getValue().primitiveValue(),
                fmlResult.getExtensionByUrl("https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-block-comparison-time")
                        .getValue().primitiveValue());

        assertEquals(javaResult.getType().getCodingFirstRep().getSystem(), fmlResult.getType().getCodingFirstRep().getSystem());
        assertEquals("urn:oid:1.2.752.129.2.2.2.11", javaResult.getType().getCodingFirstRep().getSystem());
        assertEquals(javaResult.getType().getCodingFirstRep().getCode(), fmlResult.getType().getCodingFirstRep().getCode());
        assertEquals(javaResult.getDescription(), fmlResult.getDescription());

        assertEquals(javaResult.getContentFirstRep().getAttachment().getContentType(),
                fmlResult.getContentFirstRep().getAttachment().getContentType());
        assertArrayEquals(javaResult.getContentFirstRep().getAttachment().getData(),
                fmlResult.getContentFirstRep().getAttachment().getData());
        assertEquals(javaResult.getContentFirstRep().getAttachment().getTitle(),
                fmlResult.getContentFirstRep().getAttachment().getTitle());

        assertEquals(javaResult.getAuthorFirstRep().getIdentifier().getValue(),
                fmlResult.getAuthorFirstRep().getIdentifier().getValue());
        assertEquals(javaResult.getAuthenticator().getIdentifier().getValue(),
                fmlResult.getAuthenticator().getIdentifier().getValue());
        assertEquals(
                javaResult.getExtensionByUrl("https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-signature-time")
                        .getValue().primitiveValue(),
                fmlResult.getExtensionByUrl("https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-signature-time")
                        .getValue().primitiveValue());
    }

    @Test
    void docBookFritext_fmlSaknarNarrativInnehall_javaHarDet() {
        // DOKUMENTERAT FYND (inte tyst överhoppat): DocBook-till-narrative-transformationen
        // (DocBookToNarrativeTransformer) och Strategy B-Compositionen kräver egen Java-kod -
        // adaptern skickar därför aldrig ett DocBook-fritextinnehåll vidare till FML
        // (RivtaCareDocumentationParametersAdapter.toParameters() filtrerar bort det), så FML-
        // resultatet saknar helt enkelt content/attachment i det fallet. Detta test gör
        // skillnaden explicit i stället för att låta den passera obemärkt.
        CareDocumentation entry = fullEntry("<section><title>Anteckning</title><para>Text</para></section>");

        DocumentReference javaResult = runJava(entry);
        assertTrue(javaResult.hasContent(), "Java ska producera text/html-innehåll från DocBook");
        assertEquals("text/html; charset=utf-8", javaResult.getContentFirstRep().getAttachment().getContentType());

        DocumentReference fmlResult = runFml(entry);
        assertFalse(fmlResult.hasContent(), "FML-adaptern skickar inte vidare DocBook-fritext - se fml-evaluation.md");
    }

    @Test
    void multimediaEntry_varde_matchar_java() {
        CareDocumentation entry = fullEntry(null);
        MultimediaEntry media = new MultimediaEntry();
        media.setMediaType("image/png");
        media.setValue(java.util.Base64.getEncoder().encodeToString("en bild".getBytes()));
        entry.getBody().setMultimediaEntry(media);

        DocumentReference javaResult = runJava(entry);
        DocumentReference fmlResult = runFml(entry);

        assertEquals(javaResult.getContentFirstRep().getAttachment().getContentType(),
                fmlResult.getContentFirstRep().getAttachment().getContentType());
        assertArrayEquals(javaResult.getContentFirstRep().getAttachment().getData(),
                fmlResult.getContentFirstRep().getAttachment().getData());
        assertEquals(javaResult.getContentFirstRep().getAttachment().getTitle(),
                fmlResult.getContentFirstRep().getAttachment().getTitle());
    }

    @Test
    void multimediaEntry_referens_matchar_java() {
        CareDocumentation entry = fullEntry(null);
        MultimediaEntry media = new MultimediaEntry();
        media.setMediaType("video/mp4");
        media.setReference("https://example.org/video.mp4");
        entry.getBody().setMultimediaEntry(media);

        DocumentReference javaResult = runJava(entry);
        DocumentReference fmlResult = runFml(entry);

        assertEquals(javaResult.getContentFirstRep().getAttachment().getContentType(),
                fmlResult.getContentFirstRep().getAttachment().getContentType());
        assertEquals(javaResult.getContentFirstRep().getAttachment().getUrl(),
                fmlResult.getContentFirstRep().getAttachment().getUrl());
    }

    @Test
    void dissentingOpinion_forstaPosten_matchar_java() {
        CareDocumentation entry = fullEntry("Fritext.");
        DissentingOpinion dissent = new DissentingOpinion();
        dissent.setOpinionId("op-1");
        dissent.setAuthorTime("20240103120000");
        dissent.setOpinion("Jag håller inte med.");
        PersonIdType dissentPerson = new PersonIdType();
        dissentPerson.setRoot("1.2.752.129.2.1.4.1");
        dissentPerson.setExtension("SE2321000016-OPP");
        dissent.setPersonId(dissentPerson);
        dissent.setPersonName("Opponent Opponentsson");
        entry.getBody().getDissentingOpinion().add(dissent);

        DocumentReference javaResult = runJava(entry);
        DocumentReference fmlResult = runFml(entry);

        String url = "https://fhir.inera.se/StructureDefinition/dissenting-opinion";
        assertEquals(
                javaResult.getExtensionByUrl(url).getExtensionByUrl("opinionId").getValue().primitiveValue(),
                fmlResult.getExtensionByUrl(url).getExtensionByUrl("opinionId").getValue().primitiveValue());
        assertEquals(
                javaResult.getExtensionByUrl(url).getExtensionByUrl("authorTime").getValue().primitiveValue(),
                fmlResult.getExtensionByUrl(url).getExtensionByUrl("authorTime").getValue().primitiveValue());
        assertEquals(
                javaResult.getExtensionByUrl(url).getExtensionByUrl("opinion").getValue().primitiveValue(),
                fmlResult.getExtensionByUrl(url).getExtensionByUrl("opinion").getValue().primitiveValue());
        assertEquals(
                ((org.hl7.fhir.r4.model.Identifier) javaResult.getExtensionByUrl(url).getExtensionByUrl("personId").getValue()).getValue(),
                ((org.hl7.fhir.r4.model.Identifier) fmlResult.getExtensionByUrl(url).getExtensionByUrl("personId").getValue()).getValue());
        assertEquals(
                javaResult.getExtensionByUrl(url).getExtensionByUrl("personName").getValue().primitiveValue(),
                fmlResult.getExtensionByUrl(url).getExtensionByUrl("personName").getValue().primitiveValue());
    }

    private DocumentReference runJava(CareDocumentation entry) {
        GetCareDocumentationResponse response = okResponse(entry);
        List<MappedDocumentEntry> result = javaMapper.map(response, ctx);
        assertEquals(1, result.size(), "förväntade exakt en mappad post från Java-mappern");
        return result.get(0).documentReference();
    }

    private DocumentReference runFml(CareDocumentation entry) {
        Parameters source = RivtaCareDocumentationParametersAdapter.toParameters(entry);
        return fml.transformCareDocumentation(source);
    }

    private GetCareDocumentationResponse okResponse(CareDocumentation entry) {
        GetCareDocumentationResponse response = new GetCareDocumentationResponse();
        ResultType result = new ResultType();
        result.setResultCode("OK");
        response.setResult(result);
        response.setCareDocumentation(List.of(entry));
        return response;
    }

    private CareDocumentation fullEntry(String noteText) {
        CareDocumentation entry = new CareDocumentation();

        Header header = new Header();
        header.setSourceSystemId("SE2321000016-ABC");

        AccessControlHeader ach = new AccessControlHeader();
        PersonIdType pid = new PersonIdType();
        pid.setRoot("1.2.752.129.2.1.3.1");
        pid.setExtension("190101011234");
        ach.setPatientId(pid);
        ach.setAccountableHealthcareProvider("SE2321000016-ABC");
        ach.setAccountableCareUnit("SE2321000016-ENHET");
        ach.setCareProcessId("vardprocess-123");
        ach.setBlockComparisonTime("20240101100000");
        header.setAccessControlHeader(ach);

        RecordType record = new RecordType();
        record.setRecordId("rec-001");
        record.setTimestamp("20240101120000");
        header.setRecord(record);

        Author author = new Author();
        author.setAuthorId("SE2321000016-REC");
        author.setName("Author Authorsson");
        author.setTimestamp("20240101110000");
        header.setAuthor(author);

        Signature signature = new Signature();
        signature.setSignatureId("SE2321000016-ASS");
        signature.setName("Signer Signersson");
        signature.setTimestamp("20240102120000");
        header.setSignature(signature);

        entry.setHeader(header);

        Body body = new Body();
        CVType noteCode = new CVType();
        noteCode.setCode("anteckning");
        noteCode.setCodeSystem("1.2.752.129.2.2.2.11");
        noteCode.setDisplayName("Anteckning");
        body.setClinicalDocumentNoteCode(noteCode);
        body.setClinicalDocumentNoteTitle("Journalanteckning");
        body.setClinicalDocumentNoteText(noteText);
        entry.setBody(body);

        return entry;
    }
}
