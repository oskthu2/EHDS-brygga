package se.inera.ehds.proxy.discovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import se.inera.ehds.proxy.config.ProxyProperties;

import java.time.Instant;
import java.util.Map;

/**
 * Hämtar och cachelagrar åtkomstintyg (OAuth2 client_credentials) från den utfärdare
 * som anslutningspunkten anvisar — i denna PoC en fast konfigurerad utfärdare
 * (mock-token-issuer), i linje med flödet i Ineras "T2-katalogtjänster"-demomiljö.
 */
@Service
public class AccessTokenService {

    private static final Logger log = LoggerFactory.getLogger(AccessTokenService.class);

    /** Förnyar token innan den faktiskt löper ut, för marginal mot klockskillnader. */
    private static final long EXPIRY_MARGIN_SECONDS = 30;

    private final RestTemplate restTemplate;
    private final ProxyProperties props;

    private volatile String cachedToken;
    private volatile Instant cachedTokenExpiry = Instant.EPOCH;

    public AccessTokenService(RestTemplate restTemplate, ProxyProperties props) {
        this.restTemplate = restTemplate;
        this.props = props;
    }

    public synchronized String fetchAccessToken() {
        if (cachedToken != null && Instant.now().isBefore(cachedTokenExpiry.minusSeconds(EXPIRY_MARGIN_SECONDS))) {
            return cachedToken;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", props.getTokenClientId());
        form.add("client_secret", props.getTokenClientSecret());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(
                props.getTokenIssuerUrl() + "/token", new HttpEntity<>(form, headers), Map.class);

        if (response == null || response.get("access_token") == null) {
            throw new IllegalStateException("Åtkomstintygsutfärdaren returnerade inget access_token");
        }

        cachedToken = (String) response.get("access_token");
        long expiresIn = response.get("expires_in") instanceof Number n ? n.longValue() : 0L;
        cachedTokenExpiry = Instant.now().plusSeconds(expiresIn);
        log.debug("Nytt åtkomstintyg hämtat, giltigt i {}s", expiresIn);
        return cachedToken;
    }
}
