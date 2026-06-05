package se.inera.ehds.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Läser claims ur ett JWT Bearer-token utan signaturvalidering.
 * Används enbart för loggning — tilliten bygger på att gatewayen validerat tokenet.
 */
public class JwtClaimExtractor {

    private static final Logger log = LoggerFactory.getLogger(JwtClaimExtractor.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JwtClaimExtractor() {}

    public static SmartContext extract(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return SmartContext.unknown();
        }
        String token = authorizationHeader.substring(7);
        String[] parts = token.split("\\.", -1);
        if (parts.length < 2) return SmartContext.unknown();
        try {
            // Base64url → standard base64 padding
            String pad = parts[1] + "=".repeat((4 - parts[1].length() % 4) % 4);
            String json = new String(Base64.getUrlDecoder().decode(pad), StandardCharsets.UTF_8);
            Map<?, ?> claims = MAPPER.readValue(json, Map.class);

            String clientId = stringClaim(claims, "client_id");
            if (clientId == null) clientId = stringClaim(claims, "azp");

            // fhirUser takes precedence; fall back to sub only when it differs from clientId
            String userId = stringClaim(claims, "fhirUser");
            if (userId == null) {
                String sub = stringClaim(claims, "sub");
                if (sub != null && !sub.equals(clientId)) userId = sub;
            }

            String purpose = stringClaim(claims, "purpose_of_use");
            if (purpose == null) purpose = purposeFromScope(stringClaim(claims, "scope"));

            return new SmartContext(clientId, userId, purpose);
        } catch (Exception e) {
            log.debug("JWT-payload kunde inte avkodas för SMART-kontext: {}", e.getMessage());
            return SmartContext.unknown();
        }
    }

    private static String stringClaim(Map<?, ?> claims, String key) {
        Object v = claims.get(key);
        return v instanceof String s ? s : null;
    }

    /** Enkel mappning av SMART scope-fragment till HL7 PurposeOfUse-kod. */
    private static String purposeFromScope(String scope) {
        if (scope == null) return null;
        if (scope.contains("emergency")) return "ETREAT";
        if (scope.contains("patient/") || scope.contains("user/") || scope.contains("system/")) return "TREAT";
        return null;
    }
}
