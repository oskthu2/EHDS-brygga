package se.inera.ehds.fhir;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import se.inera.ehds.config.AppProperties;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Hanterar GET /.well-known/smart-configuration med ett gemensamt
 * SMART App Launch-konfigurationsobjekt (RFC 8414 / SMART App Launch 2.0).
 *
 * Körs som Servlet-filter med högsta prioritet så att anropet aldrig når
 * HAPI FHIR-servleten. Innehållet är statiskt per applikationsinstans —
 * authBaseUrl konfigureras via {@code ehds.auth-base-url}.
 */
@Component
@Order(1)
public class SmartConfigurationFilter extends OncePerRequestFilter {

    private static final String WELL_KNOWN_PATH = "/.well-known/smart-configuration";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String authBaseUrl;

    public SmartConfigurationFilter(AppProperties props) {
        this.authBaseUrl = props.getAuthBaseUrl();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty()) {
            uri = uri.substring(ctx.length());
        }

        if (WELL_KNOWN_PATH.equals(uri)) {
            Map<String, Object> config = Map.ofEntries(
                Map.entry("issuer",                                    authBaseUrl),
                Map.entry("authorization_endpoint",                    authBaseUrl + "/auth/authorize"),
                Map.entry("token_endpoint",                            authBaseUrl + "/auth/token"),
                Map.entry("introspection_endpoint",                    authBaseUrl + "/auth/introspect"),
                Map.entry("grant_types_supported",                     List.of("client_credentials", "authorization_code")),
                Map.entry("token_endpoint_auth_methods_supported",     List.of("private_key_jwt", "client_secret_basic")),
                Map.entry("token_endpoint_auth_signing_alg_values_supported", List.of("RS256", "ES384")),
                Map.entry("scopes_supported",                          List.of(
                    "system/Condition.read",
                    "system/DocumentReference.read",
                    "user/Condition.read",
                    "user/DocumentReference.read"
                )),
                Map.entry("response_types_supported",                  List.of("code")),
                Map.entry("capabilities",                              List.of(
                    "client-confidential-asymmetric",
                    "context-banner",
                    "sso-openid-connect",
                    "permission-v2"
                ))
            );
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            MAPPER.writeValue(response.getWriter(), config);
            return;
        }

        chain.doFilter(request, response);
    }
}
