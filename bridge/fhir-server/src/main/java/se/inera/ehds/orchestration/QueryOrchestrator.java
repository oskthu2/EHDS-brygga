package se.inera.ehds.orchestration;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.inera.ehds.config.AppProperties;
import se.inera.ehds.config.VgConfig;
import se.inera.ehds.config.VgConfigLoader;
import se.inera.ehds.model.EiEngagement;
import se.inera.ehds.service.EiService;
import se.inera.ehds.service.FhirProxyClient;
import se.inera.ehds.service.LoggService;
import se.inera.ehds.service.SparrFilterService;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Orkestrerar FHIR Condition-sökning:
 *   EI → parallella FHIR-anrop till VG-endpoints → post-query Sparr → Logg
 *
 * Bryggan vet inte om VG-endpointen är en ntjp-proxy eller ett nativt FHIR-API.
 * Det styrs enbart av fhirEndpointUrl i vg-config.yaml.
 */
@Service
public class QueryOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(QueryOrchestrator.class);

    private final EiService ei;
    private final FhirProxyClient fhirClient;
    private final SparrFilterService sparr;
    private final LoggService logg;
    private final List<VgConfig> vgConfigs;
    private final VgConfigLoader vgConfigLoader;
    private final AppProperties props;

    public QueryOrchestrator(EiService ei,
                              FhirProxyClient fhirClient,
                              SparrFilterService sparr,
                              LoggService logg,
                              List<VgConfig> vgConfigs,
                              VgConfigLoader vgConfigLoader,
                              AppProperties props) {
        this.ei = ei;
        this.fhirClient = fhirClient;
        this.sparr = sparr;
        this.logg = logg;
        this.vgConfigs = vgConfigs;
        this.vgConfigLoader = vgConfigLoader;
        this.props = props;
    }

    public Bundle searchConditions(String vgHsaId, String patientSystem, String patientValue) {
        String requestId = UUID.randomUUID().toString();

        List<VgConfig> targets = resolveTargets(vgHsaId, patientSystem, patientValue);
        if (targets.isEmpty()) {
            log.warn("Inga VG-endpoints att anropa för vgHsaId={}", vgHsaId);
        }

        // Parallella FHIR-anrop
        List<CompletableFuture<List<Condition>>> futures = targets.stream()
                .map(vg -> CompletableFuture.supplyAsync(
                        () -> fhirClient.fetchConditions(vg, patientSystem, patientValue)))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        List<Condition> all = futures.stream()
                .flatMap(f -> f.join().stream())
                .collect(Collectors.toList());

        // Post-query Sparr
        all = sparr.filter(all, patientSystem, patientValue);

        // Logg (fire-and-forget)
        logg.logAccess(requestId, patientValue, patientSystem,
                "FHIR/Condition", vgHsaId != null ? vgHsaId : "global",
                "Condition", all.size(), props.getBridgeHsaId());

        return buildBundle(requestId, all);
    }

    private List<VgConfig> resolveTargets(String vgHsaId, String patientSystem, String patientValue) {
        if (vgHsaId != null) {
            return vgConfigLoader.findByHsaId(vgConfigs, vgHsaId)
                    .map(List::of).orElse(List.of());
        }
        // Globalt: EI filtrerar vilka VG:er som har data (null namespace = alla informationsmängder)
        List<EiEngagement> engagements = ei.getEngagements(patientSystem, patientValue, null);
        if (engagements.isEmpty()) {
            return vgConfigs; // EI gav inget → anropa alla konfigurerade VG:er
        }
        Set<String> withData = engagements.stream()
                .map(EiEngagement::getLogicalAddress).collect(Collectors.toSet());
        return vgConfigs.stream()
                .filter(v -> withData.contains(v.getVgHsaId()))
                .toList();
    }

    private Bundle buildBundle(String requestId, List<Condition> conditions) {
        Bundle bundle = new Bundle();
        bundle.setId(requestId);
        bundle.getMeta().setLastUpdated(new Date());
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.setTotal(conditions.size());
        for (Condition c : conditions) {
            bundle.addEntry()
                    .setFullUrl("urn:uuid:" + c.getId())
                    .setResource(c)
                    .getSearch().setMode(Bundle.SearchEntryMode.MATCH);
        }
        return bundle;
    }
}
