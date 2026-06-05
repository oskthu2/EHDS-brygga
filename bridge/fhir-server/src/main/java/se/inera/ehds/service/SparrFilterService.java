package se.inera.ehds.service;

import org.hl7.fhir.r4.model.Provenance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import se.inera.ehds.config.AppProperties;
import se.inera.ehds.mapping.tk.MappedDiagnosisEntry;
import se.inera.ehds.mapping.tk.MappedDocumentEntry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Post-query Sparr-filter: yttre spärr (careProviderHSAId) och inre spärr (careUnitHSAId).
 *
 * Läser careProviderHSAId från Provenance.agent[role=custodian] och
 * careUnitHSAId från Provenance.agent[role=author].
 *
 * Fail-closed: om careProviderHSAId saknas i Provenance, är ogiltigt HSA-id,
 * eller om spärrtjänsten inte kan nås, filtreras posten bort.
 *
 * PoC-begränsning: break-the-glass hanteras inte. Se architecture.md § Kända begränsningar.
 */
@Service
public class SparrFilterService {

    private static final Logger log = LoggerFactory.getLogger(SparrFilterService.class);

    // HSA-id: SE + digits + hyphen + alphanumeric, e.g. SE2321000016-4HK5
    private static final Pattern HSA_ID_PATTERN = Pattern.compile("^SE[0-9]+-[A-Za-z0-9]+$");

    private final RestTemplate rest;
    private final String sparrUrl;

    public SparrFilterService(RestTemplate rest, AppProperties props) {
        this.rest = rest;
        this.sparrUrl = props.getSparrUrl();
    }

    public FilterResult<MappedDiagnosisEntry> filterConditions(List<MappedDiagnosisEntry> entries,
                                                                String patientSystem, String patientId) {
        if (entries.isEmpty()) return new FilterResult<>(entries, false);
        Map<String, Boolean> cache = new HashMap<>();
        AtomicBoolean failClosed = new AtomicBoolean(false);
        List<MappedDiagnosisEntry> result = entries.stream()
                .filter(e -> !isBlocked(e.provenance(), patientSystem, patientId, cache, "Condition", failClosed))
                .collect(Collectors.toList());
        return new FilterResult<>(result, failClosed.get());
    }

    public FilterResult<MappedDocumentEntry> filterDocumentReferences(List<MappedDocumentEntry> entries,
                                                                       String patientSystem, String patientId) {
        if (entries.isEmpty()) return new FilterResult<>(entries, false);
        Map<String, Boolean> cache = new HashMap<>();
        AtomicBoolean failClosed = new AtomicBoolean(false);
        List<MappedDocumentEntry> result = entries.stream()
                .filter(e -> !isBlocked(e.provenance(), patientSystem, patientId, cache, "DocumentReference", failClosed))
                .collect(Collectors.toList());
        return new FilterResult<>(result, failClosed.get());
    }

    private boolean isBlocked(Provenance provenance, String patientSystem, String patientId,
                               Map<String, Boolean> cache, String resourceType, AtomicBoolean failClosed) {
        String careProviderHsaId = extractHsaId(provenance, "custodian");

        if (!isValidHsaId(careProviderHsaId)) {
            log.warn("Sparr: filtrerar bort {} utan giltigt careProviderHSAId i Provenance (patient {})",
                    resourceType, patientId);
            return true;
        }

        String careUnitHsaId = extractHsaId(provenance, "author");
        String cacheKey = careProviderHsaId + "|" + (careUnitHsaId != null ? careUnitHsaId : "");

        boolean blocked = cache.computeIfAbsent(cacheKey,
                k -> checkSparr(patientSystem, patientId, careProviderHsaId, careUnitHsaId, failClosed));

        if (blocked) {
            log.info("Sparr: blockerar {} från vårdgivare {} / enhet {} för patient {}",
                    resourceType, careProviderHsaId, careUnitHsaId, patientId);
        }
        return blocked;
    }

    private String extractHsaId(Provenance prov, String roleCode) {
        if (prov == null) return null;
        return prov.getAgent().stream()
                .filter(a -> a.hasType() && a.getType().getCoding().stream()
                        .anyMatch(c -> roleCode.equals(c.getCode())))
                .findFirst()
                .map(a -> a.getWho().hasIdentifier() ? a.getWho().getIdentifier().getValue() : null)
                .orElse(null);
    }

    static boolean isValidHsaId(String hsaId) {
        return hsaId != null && HSA_ID_PATTERN.matcher(hsaId).matches();
    }

    @SuppressWarnings("unchecked")
    private boolean checkSparr(String patientSystem, String patientId,
                                String careProviderHsaId, String careUnitHsaId, AtomicBoolean failClosed) {
        try {
            Map<String, String> req = new HashMap<>();
            req.put("patientSystem", patientSystem);
            req.put("patientId", patientId);
            req.put("careProviderHsaId", careProviderHsaId);
            if (careUnitHsaId != null) {
                req.put("careUnitHsaId", careUnitHsaId);
            }
            Map<String, Object> resp = rest.postForObject(sparrUrl + "/check", req, Map.class);
            if (resp == null) {
                log.warn("Sparr check returnerade null för vårdgivare {} — filtrerar bort", careProviderHsaId);
                failClosed.set(true);
                return true;
            }
            return Boolean.TRUE.equals(resp.get("blocked"));
        } catch (Exception e) {
            log.warn("Sparr check misslyckades för vårdgivare {} / enhet {}: {} — filtrerar bort",
                    careProviderHsaId, careUnitHsaId, e.getMessage());
            failClosed.set(true);
            return true;
        }
    }
}
