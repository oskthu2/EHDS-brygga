package se.inera.ehds.orchestration;

import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.inera.ehds.config.AppProperties;
import se.inera.ehds.config.VgConfig;
import se.inera.ehds.config.VgConfigLoader;
import se.inera.ehds.mapping.tk.MappedDiagnosisEntry;
import se.inera.ehds.service.FilterResult;
import se.inera.ehds.service.FhirProxyClient;
import se.inera.ehds.service.LoggService;
import se.inera.ehds.service.SmartContext;
import se.inera.ehds.service.SparrFilterService;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Orkestrerar FHIR Condition-sökning för ett VG-scopat anrop:
 *   VG-endpoint → FHIR-anrop → post-query Sparr (organisationsnivå) → Logg
 */
@Service
public class QueryOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(QueryOrchestrator.class);

    private final FhirProxyClient fhirClient;
    private final SparrFilterService sparr;
    private final LoggService logg;
    private final List<VgConfig> vgConfigs;
    private final VgConfigLoader vgConfigLoader;
    private final AppProperties props;

    public QueryOrchestrator(FhirProxyClient fhirClient,
                              SparrFilterService sparr,
                              LoggService logg,
                              List<VgConfig> vgConfigs,
                              VgConfigLoader vgConfigLoader,
                              AppProperties props) {
        this.fhirClient = fhirClient;
        this.sparr = sparr;
        this.logg = logg;
        this.vgConfigs = vgConfigs;
        this.vgConfigLoader = vgConfigLoader;
        this.props = props;
    }

    public Bundle searchConditions(String vgHsaId, String patientSystem, String patientValue,
                                   SmartContext smartContext) {
        String requestId = UUID.randomUUID().toString();

        VgConfig vg = vgConfigLoader.findByHsaId(vgConfigs, vgHsaId)
                .filter(v -> v.getResource("Condition").isPresent())
                .orElse(null);

        if (vg == null) {
            log.warn("Ingen Condition-endpoint konfigurerad för vgHsaId={}", vgHsaId);
            return buildBundle(requestId, List.of());
        }

        List<MappedDiagnosisEntry> entries = fhirClient.fetchConditions(vg, patientSystem, patientValue);
        FilterResult<MappedDiagnosisEntry> filtered = sparr.filterConditions(entries, patientSystem, patientValue);

        logg.logSparrFilter(requestId, patientValue, patientSystem, vgHsaId, "Condition",
                filtered.entries().size(), filtered.failClosed(), props.getBridgeHsaId(), smartContext);
        logg.logAccess(requestId, patientValue, patientSystem,
                "FHIR/Condition", vgHsaId, "Condition", filtered.entries().size(), props.getBridgeHsaId(), smartContext);

        return buildBundle(requestId, filtered.entries());
    }

    private Bundle buildBundle(String requestId, List<MappedDiagnosisEntry> entries) {
        Bundle bundle = new Bundle();
        bundle.setId(requestId);
        bundle.getMeta().setLastUpdated(new Date());
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.setTotal(entries.size());
        for (MappedDiagnosisEntry entry : entries) {
            bundle.addEntry()
                    .setFullUrl(entry.condition().getId())
                    .setResource(entry.condition())
                    .getSearch().setMode(Bundle.SearchEntryMode.MATCH);
            if (entry.provenance() != null) {
                bundle.addEntry()
                        .setFullUrl(entry.provenance().getId())
                        .setResource(entry.provenance())
                        .getSearch().setMode(Bundle.SearchEntryMode.INCLUDE);
            }
        }
        return bundle;
    }
}
