package se.inera.ehds.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import se.inera.ehds.config.AppProperties;
import se.inera.ehds.mapping.naming.NamingSystemRegistry;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class EiServiceTest {

    private static final String NAMESPACE = "urn:riv:clinicalprocess:activity:conditions:GetDiagnosisResponder:2";
    private static final String CANONICAL_PATIENT_SYSTEM = "http://electronichealth.se/identifier/personnummer";

    private RestTemplate rest;
    private EiService service;

    @BeforeEach
    void setUp() {
        rest = Mockito.mock(RestTemplate.class);
        AppProperties props = new AppProperties();
        props.setEiUrl("http://ei-mock");
        service = new EiService(rest, new NamingSystemRegistry(), props);
    }

    @Nested
    class Engagemang {
        @Test
        void engagemang_i_svaret_ger_lista_av_logiska_adresser() {
            when(rest.getForObject(any(URI.class), eq(Map.class))).thenReturn(Map.of(
                    "engagements", List.of(
                            Map.of("logicalAddress", "SE2321000016-4HK5"),
                            Map.of("logicalAddress", "SE2321000098-7XYZ")
                    )));

            List<String> result = service.findEngagedVgHsaIds(CANONICAL_PATIENT_SYSTEM, "191212121212", NAMESPACE);

            assertEquals(List.of("SE2321000016-4HK5", "SE2321000098-7XYZ"), result);
        }

        @Test
        void dubbletter_deduplicieras() {
            when(rest.getForObject(any(URI.class), eq(Map.class))).thenReturn(Map.of(
                    "engagements", List.of(
                            Map.of("logicalAddress", "SE2321000016-4HK5"),
                            Map.of("logicalAddress", "SE2321000016-4HK5")
                    )));

            assertEquals(List.of("SE2321000016-4HK5"),
                    service.findEngagedVgHsaIds(CANONICAL_PATIENT_SYSTEM, "191212121212", NAMESPACE));
        }

        @Test
        void tomt_svar_ger_tom_lista() {
            when(rest.getForObject(any(URI.class), eq(Map.class))).thenReturn(Map.of("engagements", List.of()));

            assertTrue(service.findEngagedVgHsaIds(CANONICAL_PATIENT_SYSTEM, "okänd", NAMESPACE).isEmpty());
        }

        @Test
        void natverksfel_ger_tom_lista() {
            when(rest.getForObject(any(URI.class), eq(Map.class))).thenThrow(new RestClientException("nätverksfel"));

            assertTrue(service.findEngagedVgHsaIds(CANONICAL_PATIENT_SYSTEM, "191212121212", NAMESPACE).isEmpty());
        }
    }

    @Nested
    class PatientSystemNormalisering {
        @Test
        void urn_oid_form_normaliseras_till_kanonisk_uri_i_anropet() {
            when(rest.getForObject(any(URI.class), eq(Map.class))).thenReturn(Map.of("engagements", List.of()));

            service.findEngagedVgHsaIds("urn:oid:1.2.752.129.2.1.3.1", "191212121212", NAMESPACE);

            var captor = org.mockito.ArgumentCaptor.forClass(URI.class);
            org.mockito.Mockito.verify(rest).getForObject(captor.capture(), eq(Map.class));
            String query = captor.getValue().getRawQuery();
            assertTrue(query.contains("patientSystem=http://electronichealth.se/identifier/personnummer"),
                    "förväntade kanonisk URI som patientSystem, fick: " + query);
            assertFalse(query.contains("urn:oid"), "urn:oid-formen ska ha normaliserats bort, fick: " + query);
        }
    }
}
