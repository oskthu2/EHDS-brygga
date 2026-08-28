package se.inera.ehds.orchestration;

import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.inera.ehds.config.AppProperties;
import se.inera.ehds.config.VgConfig;
import se.inera.ehds.config.VgConfigLoader;
import se.inera.ehds.mapping.tk.MappedDiagnosisEntry;
import se.inera.ehds.service.EiService;
import se.inera.ehds.service.FilterResult;
import se.inera.ehds.service.FhirProxyClient;
import se.inera.ehds.service.LoggService;
import se.inera.ehds.service.SmartContext;
import se.inera.ehds.service.SparrFilterService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Orkestrerar FHIR Condition-sökning:
 *   VG-scopat anrop  → exakt en VG-endpoint → FHIR-anrop → post-query Sparr → Logg
 *   Oscopat anrop    → EI avgör vilka VG:er som har engagemang → samma pipeline per VG,
 *                       resultaten sammanfogas (ingen ägarskaps-deduplicering; se README)
 */
@Service
public class QueryOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(QueryOrchestrator.class);

    /** RIVTA-tjänstekontraktet som EI:s engagemangsuppslag filtrerar på för Condition. */
    private static final String NS_RIV = "urn:riv:clinicalprocess:activity:conditions:GetDiagnosisResponder:2";

    private final FhirProxyClient fhirClient;
    private final SparrFilterService sparr;
    private final LoggService logg;
    private final EiService ei;
    private final List<VgConfig> vgConfigs;
    private final VgConfigLoader vgConfigLoader;
    private final AppProperties props;

    public QueryOrchestrator(FhirProxyClient fhirClient,
                              SparrFilterService sparr,
                              LoggService logg,
                              EiService ei,
                              List<VgConfig> vgConfigs,
                              VgConfigLoader vgConfigLoader,
                              AppProperties props) {
        this.fhirClient = fhirClient;
        this.sparr = sparr;
        this.logg = logg;
        this.ei = ei;
        this.vgConfigs = vgConfigs;
        this.vgConfigLoader = vgConfigLoader;
        this.props = props;
    }

    public Bundle searchConditions(String vgHsaId, String patientSystem, String patientValue,
                                   SmartContext smartContext) {
        String requestId = UUID.randomUUID().toString();

        if (vgHsaId == null) {
            List<String> engagedVgHsaIds = ei.findEngagedVgHsaIds(patientSystem, patientValue, NS_RIV);
            List<MappedDiagnosisEntry> allEntries = new ArrayList<>();
            for (String engagedVgHsaId : engagedVgHsaIds) {
                allEntries.addAll(fetchAndFilter(engagedVgHsaId, patientSystem, patientValue, smartContext));
            }
            return buildBundle(requestId, allEntries);
        }

        return buildBundle(requestId, fetchAndFilter(vgHsaId, patientSystem, patientValue, smartContext));
    }

    private List<MappedDiagnosisEntry> fetchAndFilter(String vgHsaId, String patientSystem, String patientValue,
                                                        SmartContext smartContext) {
        String requestId = UUID.randomUUID().toString();

        VgConfig vg = vgConfigLoader.findByHsaId(vgConfigs, vgHsaId)
                .filter(v -> v.getResource("Condition").isPresent())
                .orElse(null);

        if (vg == null) {
            log.warn("Ingen Condition-endpoint konfigurerad för vgHsaId={}", vgHsaId);
            return List.of();
        }

        List<MappedDiagnosisEntry> entries = fhirClient.fetchConditions(vg, patientSystem, patientValue);
        FilterResult<MappedDiagnosisEntry> filtered = sparr.filterConditions(entries, patientSystem, patientValue);

        logg.logSparrFilter(requestId, patientValue, patientSystem, vgHsaId, "Condition",
                filtered.entries().size(), filtered.failClosed(), props.getBridgeHsaId(), smartContext);
        logg.logAccess(requestId, patientValue, patientSystem,
                "FHIR/Condition", vgHsaId, "Condition", filtered.entries().size(), props.getBridgeHsaId(), smartContext);

        return filtered.entries();
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
