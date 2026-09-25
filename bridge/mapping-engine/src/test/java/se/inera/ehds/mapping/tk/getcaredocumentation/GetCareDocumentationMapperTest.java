package se.inera.ehds.mapping.tk.getcaredocumentation;

import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import se.inera.ehds.mapping.naming.NamingSystemRegistry;
import se.inera.ehds.mapping.rivta.caredocumentation.*;
import se.inera.ehds.mapping.rivta.caredocumentation.Signature;
import se.inera.ehds.mapping.tk.MapperContext;
import se.inera.ehds.mapping.tk.MappedDocumentEntry;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GetCareDocumentationMapperTest {

    private GetCareDocumentationMapper mapper;
    private MapperContext ctx;

    @BeforeEach
    void setUp() {
        NamingSystemRegistry namingSystem = new NamingSystemRegistry();
        mapper = new GetCareDocumentationMapper(namingSystem);
        ctx = new MapperContext(
                "http://electronichealth.se/identifier/personnummer",
                "190101011234",
                "SE2321000999-EHDS");
    }

    @Nested
    class NullOchTomIndata {
        @Test
        void null_response_ger_tom_lista() {
            assertEquals(List.of(), mapper.map(null, ctx));
        }

        @Test
        void null_careDocumentation_ger_tom_lista() {
            GetCareDocumentationResponse r = new GetCareDocumentationResponse();
            r.setCareDocumentation(null);
            assertEquals(List.of(), mapper.map(r, ctx));
        }

        @Test
        void tom_careDocumentationList_ger_tom_lista() {
            GetCareDocumentationResponse r = new GetCareDocumentationResponse();
            r.setCareDocumentation(List.of());
            assertEquals(List.of(), mapper.map(r, ctx));
        }

        @Test
        void icke_ok_resultCode_ger_tom_lista() {
            GetCareDocumentationResponse r = new GetCareDocumentationResponse();
            ResultType res = new ResultType();
            res.setResultCode("ERROR");
            r.setResult(res);
            r.setCareDocumentation(List.of(minimalEntry()));
            assertEquals(List.of(), mapper.map(r, ctx));
        }

        @Test
        void null_result_behandlas_som_ok() {
            GetCareDocumentationResponse r = new GetCareDocumentationResponse();
            r.setResult(null);
            r.setCareDocumentation(List.of(minimalEntry()));
            assertEquals(1, mapper.map(r, ctx).size());
        }

        @Test
        void saknad_header_hoppas_over() {
            CareDocumentation entry = minimalEntry();
            entry.setHeader(null);
            assertEquals(List.of(), mapper.map(responseWith(entry), ctx));
        }

        @Test
        void saknad_body_hoppas_over() {
            CareDocumentation entry = minimalEntry();
            entry.setBody(null);
            assertEquals(List.of(), mapper.map(responseWith(entry), ctx));
        }
    }

    @Nested
    class Status {
        @Test
        void status_ar_alltid_current() {
            // Body saknar statusfält – källsystemet antas endast returnera aktiva anteckningar.
            DocumentReference dr = mapper.map(responseWith(minimalEntry()), ctx).get(0).documentReference();
            assertEquals(Enumerations.DocumentReferenceStatus.CURRENT, dr.getStatus());
        }
    }

    @Nested
    class MasterIdentifierOchDatum {
        @Test
        void recordId_mappar_till_masterIdentifier() {
            CareDocumentation entry = minimalEntry();
            entry.getHeader().getRecord().setRecordId("rec-001");
            DocumentReference dr = mapper.map(responseWith(entry), ctx).get(0).documentReference();
            assertEquals("rec-001", dr.getMasterIdentifier().getValue());
        }

        @Test
        void record_timestamp_mappar_till_date() {
            CareDocumentation entry = minimalEntry();
            entry.getHeader().getRecord().setTimestamp("20240315103000");
            DocumentReference dr = mapper.map(responseWith(entry), ctx).get(0).documentReference();
            assertNotNull(dr.getDateElement());
        }
    }

    @Nested
    class Titel {
        @Test
        void clinicalDocumentNoteTitle_mappar_till_description() {
            CareDocumentation entry = minimalEntry();
            entry.getBody().setClinicalDocumentNoteTitle("Besöksanteckning kardiologi");
            DocumentReference dr = mapper.map(responseWith(entry), ctx).get(0).documentReference();
            assertEquals("Besöksanteckning kardiologi", dr.getDescription());
        }
    }

    @Nested
    class ClinicalDocumentNoteCode {
        @Test
        void noteCode_mappar_till_type_coding() {
            CareDocumentation entry = minimalEntry();
            CVType code = new CVType();
            code.setCode("bes");
            code.setCodeSystem("1.2.752.129.2.2.2.11");
            code.setDisplayName("Besöksanteckning");
            entry.getBody().setClinicalDocumentNoteCode(code);

            DocumentReference dr = mapper.map(responseWith(entry), ctx).get(0).documentReference();
            assertEquals("bes", dr.getType().getCodingFirstRep().getCode());
            assertEquals("Besöksanteckning", dr.getType().getCodingFirstRep().getDisplay());
        }

        @Test
        void okand_codeSystem_faller_tillbaka_pa_urn_oid() {
            CareDocumentation entry = minimalEntry();
            CVType code = new CVType();
            code.setCode("bes");
            code.setCodeSystem("1.2.752.129.2.2.2.11");
            entry.getBody().setClinicalDocumentNoteCode(code);

            DocumentReference dr = mapper.map(responseWith(entry), ctx).get(0).documentReference();
            assertEquals("urn:oid:1.2.752.129.2.2.2.11", dr.getType().getCodingFirstRep().getSystem());
        }

        @Test
        void originalText_anvands_som_type_text_om_present() {
            CareDocumentation entry = minimalEntry();
            CVType code = new CVType();
            code.setCode("bes");
            code.setDisplayName("Besöksanteckning");
            code.setOriginalText("Läkarbesök öppenvård");
            entry.getBody().setClinicalDocumentNoteCode(code);

            DocumentReference dr = mapper.map(responseWith(entry), ctx).get(0).documentReference();
            assertEquals("Läkarbesök öppenvård", dr.getType().getText());
        }

        @Test
        void displayName_anvands_som_type_text_om_originalText_saknas() {
            CareDocumentation entry = minimalEntry();
            CVType code = new CVType();
            code.setCode("bes");
            code.setDisplayName("Besöksanteckning");
            entry.getBody().setClinicalDocumentNoteCode(code);

            DocumentReference dr = mapper.map(responseWith(entry), ctx).get(0).documentReference();
            assertEquals("Besöksanteckning", dr.getType().getText());
        }
    }

    @Nested
    class Patient {
        @Test
        void patientId_oid_konverteras_till_uri() {
            DocumentReference dr = mapper.map(responseWith(minimalEntry()), ctx).get(0).documentReference();
            assertEquals("http://electronichealth.se/identifier/personnummer",
                    dr.getSubject().getIdentifier().getSystem());
            assertEquals("190101011234", dr.getSubject().getIdentifier().getValue());
        }
    }

    @Nested
    class InnehallXor {
        @Test
        void clinicalDocumentNoteText_kodas_som_base64_text_plain() {
            CareDocumentation entry = minimalEntry();
            entry.getBody().setClinicalDocumentNoteText("Patienten mår bra.");
            DocumentReference dr = mapper.map(responseWith(entry), ctx).get(0).documentReference();

            Attachment attachment = dr.getContentFirstRep().getAttachment();
            assertEquals("text/plain; charset=utf-8", attachment.getContentType());
            assertEquals("Patienten mår bra.", new String(attachment.getData(), StandardCharsets.UTF_8));
        }

        @Test
        void multimediaEntry_value_avkodas_som_binardata() {
            CareDocumentation entry = minimalEntry();
            entry.getBody().setClinicalDocumentNoteText(null);
            byte[] raw = "PDF-INNEHÅLL".getBytes(StandardCharsets.UTF_8);
            MultimediaEntry media = new MultimediaEntry();
            media.setMediaType("application/pdf");
            media.setValue(Base64.getEncoder().encodeToString(raw));
            entry.getBody().setMultimediaEntry(media);

            DocumentReference dr = mapper.map(responseWith(entry), ctx).get(0).documentReference();
            Attachment attachment = dr.getContentFirstRep().getAttachment();
            assertEquals("application/pdf", attachment.getContentType());
            assertArrayEquals(raw, attachment.getData());
        }

        @Test
        void multimediaEntry_reference_mappar_till_attachment_url() {
            CareDocumentation entry = minimalEntry();
            entry.getBody().setClinicalDocumentNoteText(null);
            MultimediaEntry media = new MultimediaEntry();
            media.setMediaType("image/jpeg");
            media.setReference("https://producent.example/doc/123");
            entry.getBody().setMultimediaEntry(media);

            DocumentReference dr = mapper.map(responseWith(entry), ctx).get(0).documentReference();
            Attachment attachment = dr.getContentFirstRep().getAttachment();
            assertEquals("https://producent.example/doc/123", attachment.getUrl());
        }

        @Test
        void ingen_text_eller_multimedia_ger_inget_content() {
            CareDocumentation entry = minimalEntry();
            entry.getBody().setClinicalDocumentNoteText(null);
            entry.getBody().setMultimediaEntry(null);

            DocumentReference dr = mapper.map(responseWith(entry), ctx).get(0).documentReference();
            assertEquals(0, dr.getContent().size());
        }

        @Test
        void docBookFormaterad_clinicalDocumentNoteText_transformeras_till_text_html() {
            CareDocumentation entry = minimalEntry();
            entry.getBody().setClinicalDocumentNoteText(
                    "<article><para>Patienten mår <emphasis role=\"italics\">mycket</emphasis> bra.</para></article>");

            DocumentReference dr = mapper.map(responseWith(entry), ctx).get(0).documentReference();
            Attachment attachment = dr.getContentFirstRep().getAttachment();
            String html = new String(attachment.getData(), StandardCharsets.UTF_8);

            assertEquals("text/html; charset=utf-8", attachment.getContentType());
            assertTrue(html.contains("<em>mycket</em>"));
        }

        @Test
        void vanlig_fritext_transformeras_inte_till_html() {
            CareDocumentation entry = minimalEntry();
            entry.getBody().setClinicalDocumentNoteText("Patienten mår bra.");

            DocumentReference dr = mapper.map(responseWith(entry), ctx).get(0).documentReference();
            Attachment attachment = dr.getContentFirstRep().getAttachment();

            assertEquals("text/plain; charset=utf-8", attachment.getContentType());
        }
    }

    @Nested
    class AuthorOchSignature {
        @Test
        void authorId_mappar_till_author_som_practitionerRole_referens() {
            CareDocumentation entry = minimalEntry();
            Author author = new Author();
            author.setAuthorId("SE-HOS-001");
            author.setName("Anna Andersson");
            entry.getHeader().setAuthor(author);

            DocumentReference dr = mapper.map(responseWith(entry), ctx).get(0).documentReference();
            Reference ref = dr.getAuthorFirstRep();
            assertEquals("PractitionerRole", ref.getType());
            assertEquals("SE-HOS-001", ref.getIdentifier().getValue());
            assertEquals("Anna Andersson", ref.getDisplay());
        }

        @Test
        void signatureId_mappar_till_authenticator() {
            CareDocumentation entry = minimalEntry();
            Signature sig = new Signature();
            sig.setSignatureId("SE-HOS-002");
            sig.setName("Bo Bengtsson");
            entry.getHeader().setSignature(sig);

            DocumentReference dr = mapper.map(responseWith(entry), ctx).get(0).documentReference();
            assertEquals("SE-HOS-002", dr.getAuthenticator().getIdentifier().getValue());
            assertEquals("Bo Bengtsson", dr.getAuthenticator().getDisplay());
        }

        @Test
        void signature_timestamp_mappar_till_extension_signatureTime() {
            CareDocumentation entry = minimalEntry();
            Signature sig = new Signature();
            sig.setSignatureId("SE-HOS-002");
            sig.setTimestamp("20240315113000");
            entry.getHeader().setSignature(sig);

            DocumentReference dr = mapper.map(responseWith(entry), ctx).get(0).documentReference();
            Extension ext = dr.getExtensionByUrl("https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-signature-time");
            assertNotNull(ext);
            assertTrue(ext.getValue() instanceof DateTimeType);
        }

        @Test
        void saknad_author_ger_inget_author_pa_resursen() {
            CareDocumentation entry = minimalEntry();
            entry.getHeader().setAuthor(null);
            DocumentReference dr = mapper.map(responseWith(entry), ctx).get(0).documentReference();
            assertEquals(0, dr.getAuthor().size());
        }
    }

    @Nested
    class BlockComparisonTimeOchCareProcess {
        @Test
        void blockComparisonTime_mappar_till_extension() {
            CareDocumentation entry = minimalEntry();
            entry.getHeader().getAccessControlHeader().setBlockComparisonTime("20240315090000");

            DocumentReference dr = mapper.map(responseWith(entry), ctx).get(0).documentReference();
            Extension ext = dr.getExtensionByUrl("https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-block-comparison-time");
            assertNotNull(ext);
        }

        @Test
        void careProcessId_mappar_till_context_related() {
            CareDocumentation entry = minimalEntry();
            entry.getHeader().getAccessControlHeader().setCareProcessId("process-42");

            DocumentReference dr = mapper.map(responseWith(entry), ctx).get(0).documentReference();
            assertEquals("process-42", dr.getContext().getRelatedFirstRep().getIdentifier().getValue());
        }
    }

    @Nested
    class DissentingOpinion_ {
        @Test
        void dissentingOpinion_mappar_till_extension_med_alla_delfalt() {
            CareDocumentation entry = minimalEntry();
            se.inera.ehds.mapping.rivta.caredocumentation.DissentingOpinion opinion =
                    new se.inera.ehds.mapping.rivta.caredocumentation.DissentingOpinion();
            opinion.setOpinionId("op-1");
            opinion.setAuthorTime("20240315120000");
            opinion.setOpinion("Jag delar inte bedömningen.");
            PersonIdType personId = new PersonIdType();
            personId.setRoot("1.2.752.129.2.1.4.1");
            personId.setExtension("SE-HOS-003");
            opinion.setPersonId(personId);
            opinion.setPersonName("Cecilia Carlsson");
            entry.getBody().getDissentingOpinion().add(opinion);

            DocumentReference dr = mapper.map(responseWith(entry), ctx).get(0).documentReference();
            Extension ext = dr.getExtensionByUrl("https://fhir.inera.se/StructureDefinition/dissenting-opinion");
            assertNotNull(ext);
            assertEquals("op-1", ((StringType) ext.getExtensionByUrl("opinionId").getValue()).getValue());
            assertEquals("Jag delar inte bedömningen.", ((StringType) ext.getExtensionByUrl("opinion").getValue()).getValue());
            assertEquals("Cecilia Carlsson", ((StringType) ext.getExtensionByUrl("personName").getValue()).getValue());
            Identifier personIdValue = (Identifier) ext.getExtensionByUrl("personId").getValue();
            assertEquals("SE-HOS-003", personIdValue.getValue());
        }

        @Test
        void ingen_dissentingOpinion_ger_ingen_extension() {
            DocumentReference dr = mapper.map(responseWith(minimalEntry()), ctx).get(0).documentReference();
            assertNull(dr.getExtensionByUrl("https://fhir.inera.se/StructureDefinition/dissenting-opinion"));
        }
    }

    @Nested
    class MetaOchProfil {
        @Test
        void se_ehds_document_reference_profil_deklareras() {
            DocumentReference dr = mapper.map(responseWith(minimalEntry()), ctx).get(0).documentReference();
            assertTrue(dr.getMeta().getProfile().stream()
                    .anyMatch(p -> p.getValue().contains("se-ehds-document-reference")));
        }

        @Test
        void sourceSystemId_satts_i_meta_source() {
            CareDocumentation entry = minimalEntry();
            entry.getHeader().setSourceSystemId("SE2321000016-4HK5");

            DocumentReference dr = mapper.map(responseWith(entry), ctx).get(0).documentReference();
            assertTrue(dr.getMeta().getSource().contains("SE2321000016-4HK5"));
        }
    }

    @Nested
    class ProvenanceAgenter {
        @Test
        void provenance_har_custodian_med_accountableHealthcareProvider() {
            CareDocumentation entry = minimalEntry();
            entry.getHeader().getAccessControlHeader().setAccountableHealthcareProvider("SE111-PROV");

            Provenance p = mapper.map(responseWith(entry), ctx).get(0).provenance();
            assertEquals("SE111-PROV", agentValue(p, "custodian"));
        }

        @Test
        void provenance_har_author_med_accountableCareUnit() {
            CareDocumentation entry = minimalEntry();
            entry.getHeader().getAccessControlHeader().setAccountableCareUnit("SE222-UNIT");

            Provenance p = mapper.map(responseWith(entry), ctx).get(0).provenance();
            assertEquals("SE222-UNIT", agentValue(p, "author"));
        }

        @Test
        void provenance_har_assembler_med_bridgeHsaId() {
            Provenance p = mapper.map(responseWith(minimalEntry()), ctx).get(0).provenance();
            assertEquals("SE2321000999-EHDS", agentValue(p, "assembler"));
        }

        @Test
        void provenance_recorded_anvander_author_timestamp_om_author_finns() {
            CareDocumentation entry = minimalEntry();
            Author author = new Author();
            author.setAuthorId("SE-HOS-001");
            author.setTimestamp("20240101120000");
            entry.getHeader().setAuthor(author);
            entry.getHeader().getRecord().setTimestamp("20240315090000");

            Provenance p = mapper.map(responseWith(entry), ctx).get(0).provenance();
            assertEquals("2024-01-01T12:00:00Z", p.getRecordedElement().asStringValue());
        }

        @Test
        void provenance_recorded_faller_tillbaka_pa_record_timestamp_om_author_saknas() {
            CareDocumentation entry = minimalEntry();
            entry.getHeader().setAuthor(null);
            entry.getHeader().getRecord().setTimestamp("20240315090000");

            Provenance p = mapper.map(responseWith(entry), ctx).get(0).provenance();
            assertEquals("2024-03-15T09:00:00Z", p.getRecordedElement().asStringValue());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CareDocumentation minimalEntry() {
        CareDocumentation entry = new CareDocumentation();

        PersonIdType pid = new PersonIdType();
        pid.setRoot("1.2.752.129.2.1.3.1");
        pid.setExtension("190101011234");

        AccessControlHeader ach = new AccessControlHeader();
        ach.setPatientId(pid);
        ach.setAccountableHealthcareProvider("SE2321000016-PROV");
        ach.setAccountableCareUnit("SE2321000016-4HK5");

        RecordType record = new RecordType();
        record.setRecordId("rec-default");
        record.setTimestamp("20240315090000");

        Header header = new Header();
        header.setAccessControlHeader(ach);
        header.setSourceSystemId("SE2321000016-4HK5");
        header.setRecord(record);
        entry.setHeader(header);

        Body body = new Body();
        body.setClinicalDocumentNoteText("Standardtext");
        entry.setBody(body);

        return entry;
    }

    private GetCareDocumentationResponse responseWith(CareDocumentation... entries) {
        GetCareDocumentationResponse r = new GetCareDocumentationResponse();
        r.setCareDocumentation(List.of(entries));
        return r;
    }

    private String agentValue(Provenance p, String role) {
        return p.getAgent().stream()
                .filter(a -> a.getType().getCoding().stream().anyMatch(c -> role.equals(c.getCode())))
                .findFirst()
                .map(a -> a.getWho().getIdentifier().getValue())
                .orElse(null);
    }
}
