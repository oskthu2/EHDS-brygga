package se.inera.ehds.service;

import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestTemplate;
import se.inera.ehds.config.AppProperties;
import se.inera.ehds.mapping.tk.MappedDiagnosisEntry;
import se.inera.ehds.mapping.tk.MappedDocumentEntry;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SparrFilterServiceTest {

    private static final String SPARR_URL = "http://sparr-mock";
    private static final String PROV_SYS = "http://terminology.hl7.org/CodeSystem/provenance-participant-type";
    private static final String HSA_SYS = "urn:oid:1.2.752.129.2.1.4.1";

    private RestTemplate rest;
    private SparrFilterService service;

    @BeforeEach
    void setUp() {
        rest = Mockito.mock(RestTemplate.class);
        AppProperties props = new AppProperties();
        props.setSparrUrl(SPARR_URL);
        service = new SparrFilterService(rest, props);
    }

    @Nested
    class HsaIdValidering {
        @Test
        void giltigt_hsa_id_returnerar_true() {
            assertTrue(SparrFilterService.isValidHsaId("SE2321000016-4HK5"));
        }

        @Test
        void hsa_id_med_flera_alfanumeriska_tecken_returnerar_true() {
            assertTrue(SparrFilterService.isValidHsaId("SE2321000999-EHDS"));
        }

        @Test
        void null_returnerar_false() {
            assertFalse(SparrFilterService.isValidHsaId(null));
        }

        @Test
        void tom_strang_returnerar_false() {
            assertFalse(SparrFilterService.isValidHsaId(""));
        }

        @Test
        void utan_SE_prefix_returnerar_false() {
            assertFalse(SparrFilterService.isValidHsaId("2321000016-4HK5"));
        }

        @Test
        void utan_bindestreck_returnerar_false() {
            assertFalse(SparrFilterService.isValidHsaId("SE23210000164HK5"));
        }

        @Test
        void med_specialtecken_returnerar_false() {
            assertFalse(SparrFilterService.isValidHsaId("SE2321000016-4HK5!"));
        }
    }

    @Nested
    class TomLista {
        @Test
        void tom_lista_condition_returneras_utan_sparr_anrop() {
            List<MappedDiagnosisEntry> result = service.filterConditions(List.of(), "sys", "id");
            assertTrue(result.isEmpty());
            verifyNoInteractions(rest);
        }

        @Test
        void tom_lista_documentreference_returneras_utan_sparr_anrop() {
            List<MappedDocumentEntry> result = service.filterDocumentReferences(List.of(), "sys", "id");
            assertTrue(result.isEmpty());
            verifyNoInteractions(rest);
        }
    }

    @Nested
    class FailClosed {
        @Test
        void saknat_custodian_hsaid_filtreras_bort() {
            Provenance provenance = provenanceWith(null, "SE2321000016-4HK5");
            List<MappedDiagnosisEntry> result = service.filterConditions(
                    List.of(diagnosisEntry(provenance)), "sys", "id");
            assertTrue(result.isEmpty());
            verifyNoInteractions(rest);
        }

        @Test
        void ogiltigt_custodian_hsaid_filtreras_bort() {
            Provenance provenance = provenanceWith("OGILTIGT-FORMAT", "SE2321000016-4HK5");
            List<MappedDiagnosisEntry> result = service.filterConditions(
                    List.of(diagnosisEntry(provenance)), "sys", "id");
            assertTrue(result.isEmpty());
            verifyNoInteractions(rest);
        }

        @Test
        void sparr_undantag_filtrerar_bort_posten() {
            Provenance provenance = provenanceWith("SE2321000016-PROV", "SE2321000016-4HK5");
            when(rest.postForObject(anyString(), any(), eq(Map.class)))
                    .thenThrow(new RuntimeException("Nätverksfel"));

            List<MappedDiagnosisEntry> result = service.filterConditions(
                    List.of(diagnosisEntry(provenance)), "sys", "id");
            assertTrue(result.isEmpty());
        }

        @Test
        void sparr_null_svar_filtrerar_bort_posten() {
            Provenance provenance = provenanceWith("SE2321000016-PROV", "SE2321000016-4HK5");
            when(rest.postForObject(anyString(), any(), eq(Map.class))).thenReturn(null);

            List<MappedDiagnosisEntry> result = service.filterConditions(
                    List.of(diagnosisEntry(provenance)), "sys", "id");
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class SparrBeteende {
        @Test
        void ej_blockerad_post_passerar_igenom() {
            Provenance provenance = provenanceWith("SE2321000016-PROV", "SE2321000016-4HK5");
            when(rest.postForObject(anyString(), any(), eq(Map.class)))
                    .thenReturn(Map.of("blocked", false));

            List<MappedDiagnosisEntry> result = service.filterConditions(
                    List.of(diagnosisEntry(provenance)), "sys", "id");
            assertEquals(1, result.size());
        }

        @Test
        void blockerad_post_filtreras_bort() {
            Provenance provenance = provenanceWith("SE2321000016-PROV", "SE2321000016-4HK5");
            when(rest.postForObject(anyString(), any(), eq(Map.class)))
                    .thenReturn(Map.of("blocked", true));

            List<MappedDiagnosisEntry> result = service.filterConditions(
                    List.of(diagnosisEntry(provenance)), "sys", "id");
            assertTrue(result.isEmpty());
        }

        @Test
        void samma_vardgivare_cachelagras_ett_sparr_anrop() {
            Provenance p1 = provenanceWith("SE111-PROV", "SE111-UNIT");
            Provenance p2 = provenanceWith("SE111-PROV", "SE111-UNIT");
            when(rest.postForObject(anyString(), any(), eq(Map.class)))
                    .thenReturn(Map.of("blocked", false));

            service.filterConditions(
                    List.of(diagnosisEntry(p1), diagnosisEntry(p2)), "sys", "id");

            // Sparr ska bara anropas en gång p.g.a. cache
            verify(rest, times(1)).postForObject(anyString(), any(), eq(Map.class));
        }

        @Test
        void olika_vardgivare_ger_separata_sparr_anrop() {
            Provenance p1 = provenanceWith("SE111-PROV", "SE111-UNIT");
            Provenance p2 = provenanceWith("SE222-PROV", "SE222-UNIT");
            when(rest.postForObject(anyString(), any(), eq(Map.class)))
                    .thenReturn(Map.of("blocked", false));

            service.filterConditions(
                    List.of(diagnosisEntry(p1), diagnosisEntry(p2)), "sys", "id");

            verify(rest, times(2)).postForObject(anyString(), any(), eq(Map.class));
        }
    }

    @Nested
    class DocumentReferenceFilter {
        @Test
        void ej_blockerad_documentreference_passerar_igenom() {
            Provenance provenance = provenanceWith("SE2321000016-PROV", "SE2321000016-4HK5");
            when(rest.postForObject(anyString(), any(), eq(Map.class)))
                    .thenReturn(Map.of("blocked", false));

            List<MappedDocumentEntry> result = service.filterDocumentReferences(
                    List.of(documentEntry(provenance)), "sys", "id");
            assertEquals(1, result.size());
        }

        @Test
        void blockerad_documentreference_filtreras_bort() {
            Provenance provenance = provenanceWith("SE2321000016-PROV", "SE2321000016-4HK5");
            when(rest.postForObject(anyString(), any(), eq(Map.class)))
                    .thenReturn(Map.of("blocked", true));

            List<MappedDocumentEntry> result = service.filterDocumentReferences(
                    List.of(documentEntry(provenance)), "sys", "id");
            assertTrue(result.isEmpty());
        }

        @Test
        void saknat_custodian_documentreference_filtreras_bort() {
            Provenance provenance = provenanceWith(null, "SE2321000016-4HK5");
            List<MappedDocumentEntry> result = service.filterDocumentReferences(
                    List.of(documentEntry(provenance)), "sys", "id");
            assertTrue(result.isEmpty());
            verifyNoInteractions(rest);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Provenance provenanceWith(String careProviderHsaId, String careUnitHsaId) {
        Provenance p = new Provenance();
        if (careProviderHsaId != null) {
            addAgent(p, "custodian", careProviderHsaId);
        }
        if (careUnitHsaId != null) {
            addAgent(p, "author", careUnitHsaId);
        }
        return p;
    }

    private void addAgent(Provenance p, String role, String hsaId) {
        p.addAgent()
                .setType(new CodeableConcept()
                        .addCoding(new Coding().setSystem(PROV_SYS).setCode(role)))
                .setWho(new Reference().setIdentifier(
                        new Identifier().setSystem(HSA_SYS).setValue(hsaId)));
    }

    private MappedDiagnosisEntry diagnosisEntry(Provenance provenance) {
        return new MappedDiagnosisEntry(new Condition(), provenance);
    }

    private MappedDocumentEntry documentEntry(Provenance provenance) {
        return new MappedDocumentEntry(new DocumentReference(), provenance);
    }
}
