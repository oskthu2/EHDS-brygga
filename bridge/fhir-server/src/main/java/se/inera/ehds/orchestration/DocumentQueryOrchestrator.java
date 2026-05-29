package se.inera.ehds.orchestration;

import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import se.inera.ehds.config.AppProperties;
import se.inera.ehds.config.VgConfig;
import se.inera.ehds.config.VgConfigLoader;
import se.inera.ehds.model.EiEngagement;
import se.inera.ehds.service.EiService;
import se.inera.ehds.service.FhirProxyClient;
import se.inera.ehds.service.LoggService;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class DocumentQueryOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(DocumentQueryOrchestrator.class);
    private static final String EXT_SOURCE_SYSTEM =
            "https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-source-system";

    private final EiService ei;
    private final FhirProxyClient fhirClient;
    private final RestTemplate rest;
    private final LoggService logg;
    private final List<VgConfig> vgConfigs;
    private final VgConfigLoader vgConfigLoader;
    private final AppProperties props;

    public DocumentQueryOrchestrator(EiService ei,
                                      FhirProxyClient fhirClient,
                                      RestTemplate rest,
                                      LoggService logg,
                                      List<VgConfig> vgConfigs,
                                      VgConfigLoader vgConfigLoader,
                                      AppProperties props) {
        this.ei = ei;
        this.fhirClient = fhirClient;
        this.rest = rest;
        this.logg = logg;
        this.vgConfigs = vgConfigs;
        this.vgConfigLoader = vgConfigLoader;
        this.props = props;
    }

    public Bundle searchDocumentReferences(String vgHsaId, String patientSystem, String patientValue) {
        String requestId = UUID.randomUUID().toString();

        List<VgConfig> targets = resolveTargets(vgHsaId, patientSystem, patientValue);

        List<CompletableFuture<List<DocumentReference>>> futures = targets.stream()
                .map(vg -> CompletableFuture.supplyAsync(
                        () -> fhirClient.fetchDocumentReferences(vg, patientSystem, patientValue)))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        List<DocumentReference> all = futures.stream()
                .flatMap(f -> f.join().stream())
                .collect(Collectors.toList());

        // Post-query Sparr
        all = filterBySparr(all, patientSystem, patientValue);

        logg.logAccess(requestId, patientValue, patientSystem,
                "FHIR/DocumentReference", vgHsaId != null ? vgHsaId : "global",
                "DocumentReference", all.size(), props.getBridgeHsaId());

        return buildBundle(requestId, all);
    }

    private List<VgConfig> resolveTargets(String vgHsaId, String patientSystem, String patientValue) {
        if (vgHsaId != null) {
            return vgConfigLoader.findByHsaId(vgConfigs, vgHsaId)
                    .map(List::of).orElse(List.of());
        }
        List<EiEngagement> engagements = ei.getEngagements(patientSystem, patientValue, null);
        if (engagements.isEmpty()) return vgConfigs;
        Set<String> withData = engagements.stream()
                .map(EiEngagement::getLogicalAddress).collect(Collectors.toSet());
        return vgConfigs.stream().filter(v -> withData.contains(v.getVgHsaId())).toList();
    }

    private List<DocumentReference> filterBySparr(List<DocumentReference> docs,
                                                    String patientSystem, String patientValue) {
        if (docs.isEmpty()) return docs;
        Map<String, Boolean> cache = new HashMap<>();
        return docs.stream()
                .filter(dr -> {
                    String hsaId = extractSourceHsaId(dr);
                    if (hsaId == null) return true;
                    boolean blocked = cache.computeIfAbsent(hsaId,
                            id -> checkBlocked(patientSystem, patientValue, id));
                    if (blocked) log.info("Sparr: blockerar DocumentReference från {} för patient {}", hsaId, patientValue);
                    return !blocked;
                })
                .collect(Collectors.toList());
    }

    private String extractSourceHsaId(DocumentReference dr) {
        Extension ext = dr.getExtensionByUrl(EXT_SOURCE_SYSTEM);
        if (ext == null || !(ext.getValue() instanceof Identifier id)) return null;
        return id.getValue();
    }

    @SuppressWarnings("unchecked")
    private boolean checkBlocked(String patientSystem, String patientId, String sourceSystem) {
        try {
            Map<String, String> req = Map.of("patientSystem", patientSystem,
                    "patientId", patientId, "sourceSystem", sourceSystem);
            Map<String, Object> resp = rest.postForObject(
                    props.getSparrUrl() + "/check", req, Map.class);
            return Boolean.TRUE.equals(resp != null ? resp.get("blocked") : Boolean.FALSE);
        } catch (Exception e) {
            log.warn("Sparr check failed for {}: {} — failing open", sourceSystem, e.getMessage());
            return false;
        }
    }

    private Bundle buildBundle(String requestId, List<DocumentReference> docs) {
        Bundle b = new Bundle();
        b.setId(requestId);
        b.getMeta().setLastUpdated(new Date());
        b.setType(Bundle.BundleType.SEARCHSET);
        b.setTotal(docs.size());
        for (DocumentReference dr : docs) {
            b.addEntry().setFullUrl("urn:uuid:" + dr.getId()).setResource(dr)
             .getSearch().setMode(Bundle.SearchEntryMode.MATCH);
        }
        return b;
    }
}
