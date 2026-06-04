package se.inera.ehds.mapping.tk.getdocumentlist;

import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Provenance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import se.inera.ehds.mapping.concept.ConceptMapRegistry;
import se.inera.ehds.mapping.naming.NamingSystemRegistry;
import se.inera.ehds.mapping.rivta.doclist.*;
import se.inera.ehds.mapping.tk.MapperContext;
import se.inera.ehds.mapping.tk.MappedDocumentEntry;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GetDocumentListMapperTest {

    private GetDocumentListMapper mapper;
    private MapperContext ctx;

    @BeforeEach
    void setUp() {
        NamingSystemRegistry namingSystem = new NamingSystemRegistry();
        ConceptMapRegistry conceptMaps = new ConceptMapRegistry();
        mapper = new GetDocumentListMapper(namingSystem, conceptMaps);
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
        void null_documentEntry_ger_tom_lista() {
            GetDocumentListResponse r = new GetDocumentListResponse();
            r.setDocumentEntry(null);
            assertEquals(List.of(), mapper.map(r, ctx));
        }

        @Test
        void tom_documentEntryList_ger_tom_lista() {
            GetDocumentListResponse r = new GetDocumentListResponse();
            r.setDocumentEntry(List.of());
            assertEquals(List.of(), mapper.map(r, ctx));
        }

        @Test
        void icke_ok_resultCode_ger_tom_lista() {
            GetDocumentListResponse r = new GetDocumentListResponse();
            ResultType res = new ResultType();
            res.setResultCode("ERROR");
            r.setResult(res);
            r.setDocumentEntry(List.of(minimalEntry()));
            assertEquals(List.of(), mapper.map(r, ctx));
        }

        @Test
        void null_result_behandlas_som_ok() {
            GetDocumentListResponse r = new GetDocumentListResponse();
            r.setResult(null);
            r.setDocumentEntry(List.of(minimalEntry()));
            assertEquals(1, mapper.map(r, ctx).size());
        }
    }

    @Nested
    class Status {
        @Test
        void active_statusCode_ger_CURRENT() {
            DocumentEntry entry = minimalEntry();
            entry.setStatusCode("active");
            DocumentReference dr = mapper.map(responseWith(entry), ctx).get(0).documentReference();
            assertEquals(Enumerations.DocumentReferenceStatus.CURRENT, dr.getStatus());
        }

        @Test
        void inactive_statusCode_ger_SUPERSEDED() {
            DocumentEntry entry = minimalEntry();
            entry.setStatusCode("inactive");
            DocumentReference dr = mapper.map(responseWith(entry), ctx).get(0).documentReference();
            assertEquals(Enumerations.DocumentReferenceStatus.SUPERSEDED, dr.getStatus());
        }

        @Test
        void ACTIVE_med_versaler_ger_CURRENT() {
            DocumentEntry entry = minimalEntry();
            entry.setStatusCode("ACTIVE");
            DocumentReference dr = mapper.map(responseWith(entry), ctx).get(0).documentReference();
            assertEquals(Enumerations.DocumentReferenceStatus.CURRENT, dr.getStatus());
        }

        @Test
        void null_statusCode_ger_SUPERSEDED() {
            DocumentEntry entry = minimalEntry();
            entry.setStatusCode(null);
            DocumentReference dr = mapper.map(responseWith(entry), ctx).get(0).documentReference();
            assertEquals(Enumerations.DocumentReferenceStatus.SUPERSEDED, dr.getStatus());
        }
    }

    @Nested
    class Titel {
        @Test
        void title_mappar_till_description() {
            DocumentEntry entry = minimalEntry();
            entry.setTitle("Epikris 2024-03-15");
            DocumentReference dr = mapper.map(responseWith(entry), ctx).get(0).documentReference();
            assertEquals("Epikris 2024-03-15", dr.getDescription());
        }

        @Test
        void title_sätts_i_attachment() {
            DocumentEntry entry = minimalEntry();
            entry.setTitle("Operationsberättelse");
            DocumentReference dr = mapper.map(responseWith(entry), ctx).get(0).documentReference();
            assertEquals("Operationsberättelse", dr.getContentFirstRep().getAttachment().getTitle());
        }

        @Test
        void attachment_har_alltid_contentType_pdf() {
            DocumentReference dr = mapper.map(responseWith(minimalEntry()), ctx).get(0).documentReference();
            assertEquals("application/pdf", dr.getContentFirstRep().getAttachment().getContentType());
        }
    }

    @Nested
    class TypeCode {
        @Test
        void typeCode_mappar_till_type_coding() {
            DocumentEntry entry = minimalEntry();
            CVType type = new CVType();
            type.setCode("18842-5");
            type.setCodeSystem("2.16.840.1.113883.6.1");
            type.setDisplayName("Discharge summary");
            entry.setTypeCode(type);

            DocumentReference dr = mapper.map(responseWith(entry), ctx).get(0).documentReference();
            assertEquals("18842-5", dr.getType().getCodingFirstRep().getCode());
            assertEquals("Discharge summary", dr.getType().getCodingFirstRep().getDisplay());
        }

        @Test
        void originalText_används_som_type_text_om_present() {
            DocumentEntry entry = minimalEntry();
            CVType type = new CVType();
            type.setCode("34108-1");
            type.setCodeSystem("2.16.840.1.113883.6.1");
            type.setDisplayName("Outpatient note");
            type.setOriginalText("Öppenvårdsanteckning");
            entry.setTypeCode(type);

            DocumentReference dr = mapper.map(responseWith(entry), ctx).get(0).documentReference();
            assertEquals("Öppenvårdsanteckning", dr.getType().getText());
        }

        @Test
        void displayName_används_som_type_text_om_originalText_saknas() {
            DocumentEntry entry = minimalEntry();
            CVType type = new CVType();
            type.setCode("34108-1");
            type.setCodeSystem("2.16.840.1.113883.6.1");
            type.setDisplayName("Outpatient note");
            entry.setTypeCode(type);

            DocumentReference dr = mapper.map(responseWith(entry), ctx).get(0).documentReference();
            assertEquals("Outpatient note", dr.getType().getText());
        }
    }

    @Nested
    class Patient {
        @Test
        void personnummer_oid_konverteras_till_uri() {
            DocumentEntry entry = minimalEntry();
            PersonIdType pid = new PersonIdType();
            pid.setRoot("1.2.752.129.2.1.3.1");
            pid.setExtension("190101011234");
            entry.setPatientId(pid);

            DocumentReference dr = mapper.map(responseWith(entry), ctx).get(0).documentReference();
            assertEquals("http://electronichealth.se/identifier/personnummer",
                    dr.getSubject().getIdentifier().getSystem());
            assertEquals("190101011234", dr.getSubject().getIdentifier().getValue());
        }
    }

    @Nested
    class Author {
        @Test
        void careUnitHsaId_sätts_som_author() {
            DocumentEntry entry = minimalEntry();
            entry.setCareUnitHSAId("SE2321000016-4HK5");

            DocumentReference dr = mapper.map(responseWith(entry), ctx).get(0).documentReference();
            assertEquals("SE2321000016-4HK5", dr.getAuthorFirstRep().getIdentifier().getValue());
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
        void sourceHsaId_sätts_i_meta_source() {
            DocumentEntry entry = minimalEntry();
            entry.setSourceSystemHSAId("SE2321000016-4HK5");

            DocumentReference dr = mapper.map(responseWith(entry), ctx).get(0).documentReference();
            assertTrue(dr.getMeta().getSource().contains("SE2321000016-4HK5"));
        }

        @Test
        void documentTime_sätts_som_date() {
            DocumentEntry entry = minimalEntry();
            entry.setDocumentTime("20240315");

            DocumentReference dr = mapper.map(responseWith(entry), ctx).get(0).documentReference();
            assertNotNull(dr.getDateElement());
        }
    }

    @Nested
    class ProvenanceAgenter {
        @Test
        void provenance_har_custodian_med_careProviderHsaId() {
            DocumentEntry entry = minimalEntry();
            entry.setCareProviderHSAId("SE111-PROV");

            Provenance p = mapper.map(responseWith(entry), ctx).get(0).provenance();
            assertEquals("SE111-PROV", agentValue(p, "custodian"));
        }

        @Test
        void provenance_har_author_med_careUnitHsaId() {
            DocumentEntry entry = minimalEntry();
            entry.setCareUnitHSAId("SE222-UNIT");

            Provenance p = mapper.map(responseWith(entry), ctx).get(0).provenance();
            assertEquals("SE222-UNIT", agentValue(p, "author"));
        }

        @Test
        void provenance_har_assembler_med_bridgeHsaId() {
            Provenance p = mapper.map(responseWith(minimalEntry()), ctx).get(0).provenance();
            assertEquals("SE2321000999-EHDS", agentValue(p, "assembler"));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private DocumentEntry minimalEntry() {
        DocumentEntry entry = new DocumentEntry();
        entry.setDocumentId("doc-001");
        entry.setStatusCode("active");
        entry.setDocumentTime("20240315");
        entry.setCareUnitHSAId("SE2321000016-4HK5");
        entry.setCareProviderHSAId("SE2321000016-PROV");

        PersonIdType pid = new PersonIdType();
        pid.setRoot("1.2.752.129.2.1.3.1");
        pid.setExtension("190101011234");
        entry.setPatientId(pid);

        return entry;
    }

    private GetDocumentListResponse responseWith(DocumentEntry... entries) {
        GetDocumentListResponse r = new GetDocumentListResponse();
        r.setDocumentEntry(List.of(entries));
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
