package se.inera.ehds.service;

import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.DomainResource;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Post-query Sparr-filter: yttre spärr (careProviderHSAId) och inre spärr (careUnitHSAId).
 *
 * Fail-closed: om careProviderHSAId saknas, är ogiltigt HSA-id, eller om spärrtjänsten
 * inte kan nås filtreras posten bort — det går inte att avgöra om den är spärrad.
 *
 * PoC-begränsning: break-the-glass hanteras inte. Se architecture.md § Kända begränsningar.
 */
@Service
public class SparrFilterService {

    private static final Logger log = LoggerFactory.getLogger(SparrFilterService.class);

    private static final String EXT_CARE_PROVIDER =
            "https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-care-provider";
    private static final String EXT_CARE_UNIT =
            "https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-care-unit";

    // HSA-id: SE + digits + hyphen + alphanumeric, e.g. SE2321000016-4HK5
    private static final Pattern HSA_ID_PATTERN = Pattern.compile("^SE[0-9]+-[A-Za-z0-9]+$");

    private final RestTemplate rest;
    private final String sparrUrl;

    public SparrFilterService(RestTemplate rest, AppProperties props) {
        this.rest = rest;
        this.sparrUrl = props.getSparrUrl();
    }

    public List<MappedDiagnosisEntry> filterConditions(List<MappedDiagnosisEntry> entries,
                                                        String patientSystem, String patientId) {
        if (entries.isEmpty()) return entries;
        Map<String, Boolean> cache = new HashMap<>();
        return entries.stream()
                .filter(e -> !isBlocked(e.condition(), patientSystem, patientId, cache, "Condition"))
                .collect(Collectors.toList());
    }

    public List<DocumentReference> filterDocumentReferences(List<DocumentReference> docs,
                                                             String patientSystem, String patientId) {
        if (docs.isEmpty()) return docs;
        Map<String, Boolean> cache = new HashMap<>();
        return docs.stream()
                .filter(dr -> !isBlocked(dr, patientSystem, patientId, cache, "DocumentReference"))
                .collect(Collectors.toList());
    }

    private boolean isBlocked(DomainResource resource, String patientSystem, String patientId,
                               Map<String, Boolean> cache, String resourceType) {
        String careProviderHsaId = extractHsaId(resource, EXT_CARE_PROVIDER);

        if (!isValidHsaId(careProviderHsaId)) {
            log.warn("Sparr: filtrerar bort {} utan giltigt careProviderHSAId (patient {})",
                    resourceType, patientId);
            return true;
        }

        // Inre spärr: careUnitHSAId is available on Condition; null on DocumentReference
        String careUnitHsaId = extractHsaId(resource, EXT_CARE_UNIT);
        String cacheKey = careProviderHsaId + "|" + (careUnitHsaId != null ? careUnitHsaId : "");

        boolean blocked = cache.computeIfAbsent(cacheKey,
                k -> checkSparr(patientSystem, patientId, careProviderHsaId, careUnitHsaId));

        if (blocked) {
            log.info("Sparr: blockerar {} från vårdgivare {} / enhet {} för patient {}",
                    resourceType, careProviderHsaId, careUnitHsaId, patientId);
        }
        return blocked;
    }

    private String extractHsaId(DomainResource resource, String extensionUrl) {
        Extension ext = resource.getExtensionByUrl(extensionUrl);
        if (ext == null || !(ext.getValue() instanceof Identifier id)) return null;
        return id.getValue();
    }

    static boolean isValidHsaId(String hsaId) {
        return hsaId != null && HSA_ID_PATTERN.matcher(hsaId).matches();
    }

    @SuppressWarnings("unchecked")
    private boolean checkSparr(String patientSystem, String patientId,
                                String careProviderHsaId, String careUnitHsaId) {
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
                return true;
            }
            return Boolean.TRUE.equals(resp.get("blocked"));
        } catch (Exception e) {
            log.warn("Sparr check misslyckades för vårdgivare {} / enhet {}: {} — filtrerar bort",
                    careProviderHsaId, careUnitHsaId, e.getMessage());
            return true;
        }
    }
}
