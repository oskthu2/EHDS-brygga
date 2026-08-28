package se.inera.ehds.proxy.discovery;

import ca.uhn.fhir.context.FhirContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import se.inera.ehds.proxy.config.ProxyProperties;

import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class CatalogDiscoveryServiceTest {

    private static final String VG_HSA_ID = "SE2321000016-4HK5";
    private static final String NAMESPACE = "urn:riv:clinicalprocess:activity:conditions:GetDiagnosisResponder:2";

    private RestTemplate rest;
    private CatalogDiscoveryService service;

    @BeforeEach
    void setUp() {
        rest = Mockito.mock(RestTemplate.class);
        ProxyProperties props = new ProxyProperties();
        props.setTjanstekatalogUrl("http://tjanstekatalog-mock");
        props.setFedkatalogUrl("http://fedkatalog-mock");
        service = new CatalogDiscoveryService(rest, FhirContext.forR4Cached(), props);
    }

    @Nested
    class ResolveEndpointAddress {
        @Test
        void endpoint_i_bundle_ger_fysisk_adress() {
            when(rest.getForObject(any(URI.class), eq(String.class))).thenReturn("""
                {"resourceType":"Bundle","type":"searchset","total":1,"entry":[
                  {"resource":{"resourceType":"Endpoint","id":"e1","status":"active",
                    "address":"http://mock-backend:4005/soap"}}
                ]}
                """);

            Optional<String> result = service.resolveEndpointAddress(VG_HSA_ID, NAMESPACE);

            assertEquals(Optional.of("http://mock-backend:4005/soap"), result);
        }

        @Test
        void tom_bundle_ger_tomt_resultat() {
            when(rest.getForObject(any(URI.class), eq(String.class))).thenReturn(
                    "{\"resourceType\":\"Bundle\",\"type\":\"searchset\",\"total\":0,\"entry\":[]}");

            assertTrue(service.resolveEndpointAddress(VG_HSA_ID, NAMESPACE).isEmpty());
        }

        @Test
        void natverksfel_ger_tomt_resultat() {
            when(rest.getForObject(any(URI.class), eq(String.class)))
                    .thenThrow(new RestClientException("nätverksfel"));

            assertTrue(service.resolveEndpointAddress(VG_HSA_ID, NAMESPACE).isEmpty());
        }
    }

    @Nested
    class VerifyActiveMembership {
        @Test
        void aktiv_medlem_ger_true() {
            when(rest.getForObject(any(URI.class), eq(String.class))).thenReturn("""
                {"resourceType":"Bundle","type":"searchset","total":1,"entry":[
                  {"resource":{"resourceType":"OrganizationAffiliation","id":"a1","active":true}}
                ]}
                """);

            assertTrue(service.verifyActiveMembership(VG_HSA_ID));
        }

        @Test
        void ingen_traff_ger_false() {
            when(rest.getForObject(any(URI.class), eq(String.class))).thenReturn(
                    "{\"resourceType\":\"Bundle\",\"type\":\"searchset\",\"total\":0,\"entry\":[]}");

            assertFalse(service.verifyActiveMembership(VG_HSA_ID));
        }

        @Test
        void natverksfel_ger_false() {
            when(rest.getForObject(any(URI.class), eq(String.class)))
                    .thenThrow(new RestClientException("nätverksfel"));

            assertFalse(service.verifyActiveMembership(VG_HSA_ID));
        }
    }
}
