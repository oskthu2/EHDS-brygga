package se.inera.ehds.proxy.discovery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;
import se.inera.ehds.proxy.config.ProxyProperties;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccessTokenServiceTest {

    private RestTemplate rest;
    private AccessTokenService service;

    @BeforeEach
    void setUp() {
        rest = Mockito.mock(RestTemplate.class);
        ProxyProperties props = new ProxyProperties();
        props.setTokenIssuerUrl("http://token-issuer-mock");
        props.setTokenClientId("test-client");
        props.setTokenClientSecret("test-secret");
        service = new AccessTokenService(rest, props);
    }

    @Test
    void giltigt_svar_ger_access_token() {
        when(rest.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("access_token", "mock-jwt", "expires_in", 3600));

        assertEquals("mock-jwt", service.fetchAccessToken());
    }

    @Test
    void saknat_access_token_i_svaret_kastar_undantag() {
        when(rest.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("error", "invalid_client"));

        assertThrows(IllegalStateException.class, () -> service.fetchAccessToken());
    }

    @Test
    void giltig_token_cachelagras_och_hamtas_inte_pa_nytt() {
        when(rest.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("access_token", "mock-jwt", "expires_in", 3600));

        service.fetchAccessToken();
        service.fetchAccessToken();

        verify(rest, times(1)).postForObject(anyString(), any(HttpEntity.class), eq(Map.class));
    }
}
