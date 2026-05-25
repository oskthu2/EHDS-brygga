package se.inera.ehds.service;

import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import se.inera.ehds.config.AppProperties;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Post-query Sparr filter.
 *
 * MUST be called AFTER all SOAP responses are collected and transformed to Condition resources.
 * Filters out Conditions whose source system (sourceSystemHSAId, from ext-source-system extension)
 * is blocked by Sparrtjansten for this patient.
 *
 * Results per HSA-id are cached for the duration of the request to avoid duplicate calls.
 */
@Service
public class SparrFilterService {

    private static final Logger log = LoggerFactory.getLogger(SparrFilterService.class);
    private static final String EXT_SOURCE_SYSTEM =
            "https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-source-system";

    private final RestTemplate rest;
    private final String sparrUrl;

    public SparrFilterService(RestTemplate rest, AppProperties props) {
        this.rest = rest;
        this.sparrUrl = props.getSparrUrl();
    }

    public List<Condition> filter(List<Condition> conditions, String patientSystem, String patientId) {
        if (conditions.isEmpty()) return conditions;

        // Per-request cache: HSA-id → blocked
        Map<String, Boolean> blockedCache = new HashMap<>();

        return conditions.stream()
                .filter(c -> {
                    String hsaId = extractSourceHsaId(c);
                    if (hsaId == null) return true; // no source system info → include (fail-open)

                    boolean blocked = blockedCache.computeIfAbsent(hsaId,
                            id -> checkBlocked(patientSystem, patientId, id));

                    if (blocked) {
                        log.info("Sparr: blocking Condition from {} for patient {}",
                                hsaId, patientId);
                    }
                    return !blocked;
                })
                .collect(Collectors.toList());
    }

    private String extractSourceHsaId(Condition condition) {
        Extension ext = condition.getExtensionByUrl(EXT_SOURCE_SYSTEM);
        if (ext == null || !(ext.getValue() instanceof Identifier id)) return null;
        return id.getValue();
    }

    @SuppressWarnings("unchecked")
    private boolean checkBlocked(String patientSystem, String patientId, String sourceSystem) {
        try {
            Map<String, String> req = Map.of(
                    "patientSystem", patientSystem,
                    "patientId", patientId,
                    "sourceSystem", sourceSystem);
            Map<String, Object> resp = rest.postForObject(sparrUrl + "/check", req, Map.class);
            return Boolean.TRUE.equals(resp != null ? resp.get("blocked") : Boolean.FALSE);
        } catch (Exception e) {
            log.warn("Sparr check failed for {}: {} — failing open", sourceSystem, e.getMessage());
            return false; // fail-open: never hide data due to infrastructure errors
        }
    }
}
