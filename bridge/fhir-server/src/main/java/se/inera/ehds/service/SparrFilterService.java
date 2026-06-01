package se.inera.ehds.service;

import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import se.inera.ehds.config.AppProperties;
import se.inera.ehds.mapping.tk.MappedDiagnosisEntry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Post-query Sparr filter (organisationsnivå).
 *
 * Sparrtjänsten spärrar på organisationsnivå – kontrollen ska göras mot
 * careProviderHSAId (ansvarig vårdgivare), inte sourceSystemHSAId.
 * careProviderHSAId finns i ext-care-provider-extensionen på Condition.
 *
 * Fail-open: infrastrukturfel döljer aldrig data.
 *
 * PoC-begränsning: en vårdgivare från en spärrad enhet som ändå har rätt
 * att ta del av informationen (break-the-glass) hanteras inte. Se
 * architecture.md § Kända begränsningar.
 */
@Service
public class SparrFilterService {

    private static final Logger log = LoggerFactory.getLogger(SparrFilterService.class);
    private static final String EXT_CARE_PROVIDER =
            "https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-care-provider";

    private final RestTemplate rest;
    private final String sparrUrl;

    public SparrFilterService(RestTemplate rest, AppProperties props) {
        this.rest = rest;
        this.sparrUrl = props.getSparrUrl();
    }

    public List<MappedDiagnosisEntry> filter(List<MappedDiagnosisEntry> entries,
                                              String patientSystem, String patientId) {
        if (entries.isEmpty()) return entries;

        // Per-request cache: careProviderHSAId → blocked
        Map<String, Boolean> blockedCache = new HashMap<>();

        return entries.stream()
                .filter(entry -> {
                    String careProviderHsaId = extractCareProviderHsaId(entry.condition());
                    if (careProviderHsaId == null) return true; // fail-open

                    boolean blocked = blockedCache.computeIfAbsent(careProviderHsaId,
                            id -> checkBlocked(patientSystem, patientId, id));

                    if (blocked) {
                        log.info("Sparr: blocking Condition from vårdgivare {} for patient {}",
                                careProviderHsaId, patientId);
                    }
                    return !blocked;
                })
                .collect(Collectors.toList());
    }

    private String extractCareProviderHsaId(Condition condition) {
        Extension ext = condition.getExtensionByUrl(EXT_CARE_PROVIDER);
        if (ext == null || !(ext.getValue() instanceof Identifier id)) return null;
        return id.getValue();
    }

    @SuppressWarnings("unchecked")
    private boolean checkBlocked(String patientSystem, String patientId, String careProviderHsaId) {
        try {
            Map<String, String> req = Map.of(
                    "patientSystem", patientSystem,
                    "patientId", patientId,
                    "careProviderHsaId", careProviderHsaId);
            Map<String, Object> resp = rest.postForObject(sparrUrl + "/check", req, Map.class);
            return Boolean.TRUE.equals(resp != null ? resp.get("blocked") : Boolean.FALSE);
        } catch (Exception e) {
            log.warn("Sparr check failed for {}: {} — failing open", careProviderHsaId, e.getMessage());
            return false;
        }
    }
}
