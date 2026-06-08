package se.inera.ehds.fhir;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TenantInterceptorTest {

    TenantInterceptor interceptor;
    ServletRequestDetails requestDetails;
    HttpServletRequest httpRequest;

    @BeforeEach
    void setUp() {
        interceptor = new TenantInterceptor();
        httpRequest = Mockito.mock(HttpServletRequest.class);
        requestDetails = Mockito.mock(ServletRequestDetails.class);
        when(requestDetails.getServletRequest()).thenReturn(httpRequest);
        when(httpRequest.getContextPath()).thenReturn("");
    }

    @Nested
    class UrlExtraktion {

        @Test
        void vg_hsaid_extraheras_ur_condition_sökväg() {
            when(httpRequest.getRequestURI()).thenReturn("/SE2321000016-4HK5/fhir/Condition");

            interceptor.extractTenant(requestDetails);

            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            verify(requestDetails).setAttribute(eq("vgHsaId"), captor.capture());
            assertEquals("SE2321000016-4HK5", captor.getValue());
        }

        @Test
        void vg_hsaid_extraheras_ur_metadata_sökväg() {
            when(httpRequest.getRequestURI()).thenReturn("/SE2321000098-7XYZ/fhir/metadata");

            interceptor.extractTenant(requestDetails);

            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            verify(requestDetails).setAttribute(eq("vgHsaId"), captor.capture());
            assertEquals("SE2321000098-7XYZ", captor.getValue());
        }

        @Test
        void vg_hsaid_extraheras_ur_document_reference_sökväg() {
            when(httpRequest.getRequestURI()).thenReturn("/SE2321000016-4HK5/fhir/DocumentReference");

            interceptor.extractTenant(requestDetails);

            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            verify(requestDetails).setAttribute(eq("vgHsaId"), captor.capture());
            assertEquals("SE2321000016-4HK5", captor.getValue());
        }
    }

    @Nested
    class OmatchandeSokvagar {

        @Test
        void sökväg_utan_vg_prefix_sätter_inget_attribut() {
            when(httpRequest.getRequestURI()).thenReturn("/Condition");

            interceptor.extractTenant(requestDetails);

            verify(requestDetails, never()).setAttribute(any(), any());
        }

        @Test
        void well_known_sökväg_sätter_inget_attribut() {
            when(httpRequest.getRequestURI()).thenReturn("/.well-known/smart-configuration");

            interceptor.extractTenant(requestDetails);

            verify(requestDetails, never()).setAttribute(any(), any());
        }

        @Test
        void icke_servletrequestdetails_ignoreras() {
            RequestDetails annanTyp = Mockito.mock(RequestDetails.class);

            interceptor.extractTenant(annanTyp);

            verify(annanTyp, never()).setAttribute(any(), any());
        }
    }

    @Nested
    class KontextsokvasStrippas {

        @Test
        void kontextsökväg_strippas_innan_mönstermatchning() {
            when(httpRequest.getContextPath()).thenReturn("/api");
            when(httpRequest.getRequestURI()).thenReturn("/api/SE2321000016-4HK5/fhir/Condition");

            interceptor.extractTenant(requestDetails);

            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            verify(requestDetails).setAttribute(eq("vgHsaId"), captor.capture());
            assertEquals("SE2321000016-4HK5", captor.getValue());
        }
    }
}
