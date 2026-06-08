package se.inera.ehds.fhir;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import se.inera.ehds.config.VgConfig;
import se.inera.ehds.config.VgConfigLoader;
import se.inera.ehds.config.VgResourceConfig;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class CapabilityStatementEnricherTest {

    static final String AUTH_BASE = "http://auth.example.com";
    static final String VG_HSA_ID = "SE2321000016-4HK5";
    static final String VG_DESCRIPTION = "VGR – Västra Götalandsregionen";

    VgConfigLoader loader;
    RequestDetails requestDetails;
    CapabilityStatement cs;

    @BeforeEach
    void setUp() {
        loader = new VgConfigLoader();
        requestDetails = Mockito.mock(RequestDetails.class);
        cs = new CapabilityStatement();
        cs.addRest();
    }

    private CapabilityStatementEnricher enricher(List<VgConfig> configs) {
        return new CapabilityStatementEnricher(AUTH_BASE, configs, loader);
    }

    private VgConfig vgMed(String... resourceTypes) {
        VgConfig vg = new VgConfig();
        vg.setVgHsaId(VG_HSA_ID);
        vg.setDescription(VG_DESCRIPTION);
        Map<String, VgResourceConfig> resources = new LinkedHashMap<>();
        for (String rt : resourceTypes) {
            VgResourceConfig rc = new VgResourceConfig();
            rc.setAccess("tk");
            rc.setEndpointUrl("http://ntjp-proxy:8091/fhir/" + VG_HSA_ID);
            resources.put(rt, rc);
        }
        vg.setResources(resources);
        return vg;
    }

    @Nested
    class VgSaknas {

        @Test
        void okänt_vg_hsaid_ger_404() {
            when(requestDetails.getAttribute("vgHsaId")).thenReturn("SE9999-OKÄND");

            assertThrows(ResourceNotFoundException.class,
                    () -> enricher(List.of()).enrich(cs, requestDetails));
        }

        @Test
        void saknat_vg_hsaid_i_url_ger_400() {
            when(requestDetails.getAttribute("vgHsaId")).thenReturn(null);

            assertThrows(InvalidRequestException.class,
                    () -> enricher(List.of()).enrich(cs, requestDetails));
        }

        @Test
        void null_request_details_ger_400() {
            assertThrows(InvalidRequestException.class,
                    () -> enricher(List.of()).enrich(cs, null));
        }
    }

    @Nested
    class ResursFiltrering {

        @Test
        void vg_med_condition_inkluderar_bara_condition() {
            when(requestDetails.getAttribute("vgHsaId")).thenReturn(VG_HSA_ID);
            cs.getRestFirstRep().addResource().setType("Condition");
            cs.getRestFirstRep().addResource().setType("DocumentReference");

            enricher(List.of(vgMed("Condition"))).enrich(cs, requestDetails);

            List<String> typer = cs.getRestFirstRep().getResource().stream()
                    .map(CapabilityStatement.CapabilityStatementRestResourceComponent::getType)
                    .toList();
            assertTrue(typer.contains("Condition"));
            assertFalse(typer.contains("DocumentReference"));
        }

        @Test
        void vg_med_båda_resurserna_behåller_båda() {
            when(requestDetails.getAttribute("vgHsaId")).thenReturn(VG_HSA_ID);
            cs.getRestFirstRep().addResource().setType("Condition");
            cs.getRestFirstRep().addResource().setType("DocumentReference");

            enricher(List.of(vgMed("Condition", "DocumentReference"))).enrich(cs, requestDetails);

            List<String> typer = cs.getRestFirstRep().getResource().stream()
                    .map(CapabilityStatement.CapabilityStatementRestResourceComponent::getType)
                    .toList();
            assertEquals(2, typer.size());
            assertTrue(typer.contains("Condition"));
            assertTrue(typer.contains("DocumentReference"));
        }

        @Test
        void vg_utan_resurser_ger_tomt_resurslista() {
            when(requestDetails.getAttribute("vgHsaId")).thenReturn(VG_HSA_ID);
            VgConfig vg = new VgConfig();
            vg.setVgHsaId(VG_HSA_ID);
            vg.setResources(Map.of());
            cs.getRestFirstRep().addResource().setType("Condition");

            enricher(List.of(vg)).enrich(cs, requestDetails);

            assertTrue(cs.getRestFirstRep().getResource().isEmpty());
        }
    }

    @Nested
    class Berikning {

        @Test
        void condition_resource_får_patient_identifier_sökparam() {
            when(requestDetails.getAttribute("vgHsaId")).thenReturn(VG_HSA_ID);

            enricher(List.of(vgMed("Condition"))).enrich(cs, requestDetails);

            CapabilityStatement.CapabilityStatementRestResourceComponent condResource =
                    cs.getRestFirstRep().getResource().stream()
                            .filter(r -> "Condition".equals(r.getType()))
                            .findFirst().orElseThrow();
            assertTrue(condResource.getSearchParam().stream()
                    .anyMatch(sp -> "patient.identifier".equals(sp.getName())));
        }

        @Test
        void eu_hda_rap_url_läggs_till_i_instantiates() {
            when(requestDetails.getAttribute("vgHsaId")).thenReturn(VG_HSA_ID);

            enricher(List.of(vgMed("Condition"))).enrich(cs, requestDetails);

            assertTrue(cs.getInstantiates().stream()
                    .anyMatch(u -> u.getValue().contains("EEHRxF-ResourceAccessProvider")));
        }

        @Test
        void titeln_innehåller_vg_description() {
            when(requestDetails.getAttribute("vgHsaId")).thenReturn(VG_HSA_ID);

            enricher(List.of(vgMed("Condition"))).enrich(cs, requestDetails);

            assertTrue(cs.getTitle().contains(VG_DESCRIPTION));
        }

        @Test
        void smart_säkerhetstillägg_sätts_på_rest() {
            when(requestDetails.getAttribute("vgHsaId")).thenReturn(VG_HSA_ID);

            enricher(List.of(vgMed("Condition"))).enrich(cs, requestDetails);

            assertTrue(cs.getRestFirstRep().getSecurity().getCors());
            assertFalse(cs.getRestFirstRep().getSecurity().getExtension().isEmpty());
        }
    }
}
