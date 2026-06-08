package se.inera.ehds.fhir;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class VgAwareAddressStrategyTest {

    VgAwareAddressStrategy strategy;
    HttpServletRequest request;

    @BeforeEach
    void setUp() {
        strategy = new VgAwareAddressStrategy();
        request = Mockito.mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getContextPath()).thenReturn("");
    }

    @Nested
    class VgFhirMonster {

        @Test
        void condition_sökväg_ger_vg_fhir_bas() {
            when(request.getRequestURI()).thenReturn("/SE2321000016-4HK5/fhir/Condition");

            assertEquals("http://localhost:8080/SE2321000016-4HK5/fhir",
                    strategy.determineServerBase(null, request));
        }

        @Test
        void metadata_sökväg_ger_vg_fhir_bas() {
            when(request.getRequestURI()).thenReturn("/SE2321000016-4HK5/fhir/metadata");

            assertEquals("http://localhost:8080/SE2321000016-4HK5/fhir",
                    strategy.determineServerBase(null, request));
        }

        @Test
        void enbart_fhir_suffix_utan_resurs_ger_vg_fhir_bas() {
            when(request.getRequestURI()).thenReturn("/SE2321000016-4HK5/fhir");

            assertEquals("http://localhost:8080/SE2321000016-4HK5/fhir",
                    strategy.determineServerBase(null, request));
        }

        @Test
        void standardport_80_inkluderas_inte_i_bas() {
            when(request.getScheme()).thenReturn("http");
            when(request.getServerPort()).thenReturn(80);
            when(request.getRequestURI()).thenReturn("/SE2321000016-4HK5/fhir/Condition");

            assertEquals("http://localhost/SE2321000016-4HK5/fhir",
                    strategy.determineServerBase(null, request));
        }

        @Test
        void standardport_443_inkluderas_inte_i_bas() {
            when(request.getScheme()).thenReturn("https");
            when(request.getServerPort()).thenReturn(443);
            when(request.getRequestURI()).thenReturn("/SE2321000016-4HK5/fhir/Condition");

            assertEquals("https://localhost/SE2321000016-4HK5/fhir",
                    strategy.determineServerBase(null, request));
        }
    }

    @Nested
    class OmatchandeSokvagar {

        @BeforeEach
        void stubRequestUrl() {
            when(request.getRequestURL())
                    .thenReturn(new StringBuffer("http://localhost:8080/Condition"));
        }

        @Test
        void sökväg_utan_vg_prefix_faller_tillbaka_på_fallback() {
            when(request.getRequestURI()).thenReturn("/Condition");

            String base = strategy.determineServerBase(null, request);

            assertNotNull(base);
            assertFalse(base.endsWith("/fhir"), "Fallback ska inte ge /fhir-suffix: " + base);
        }

        @Test
        void well_known_sökväg_faller_tillbaka_på_fallback() {
            when(request.getRequestURL())
                    .thenReturn(new StringBuffer("http://localhost:8080/.well-known/smart-configuration"));
            when(request.getRequestURI()).thenReturn("/.well-known/smart-configuration");

            String base = strategy.determineServerBase(null, request);

            assertNotNull(base);
            assertFalse(base.contains("/fhir"), "Fallback ska inte innehålla /fhir: " + base);
        }
    }

    @Nested
    class KontextsokvasStrippas {

        @Test
        void kontextsökväg_strippas_innan_mönstermatchning() {
            when(request.getContextPath()).thenReturn("/api");
            when(request.getRequestURI()).thenReturn("/api/SE2321000016-4HK5/fhir/Condition");

            assertEquals("http://localhost:8080/SE2321000016-4HK5/fhir",
                    strategy.determineServerBase(null, request));
        }
    }
}
