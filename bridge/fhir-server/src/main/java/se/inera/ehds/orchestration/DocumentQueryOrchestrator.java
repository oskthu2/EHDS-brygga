package se.inera.ehds.orchestration;

import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.inera.ehds.config.AppProperties;
import se.inera.ehds.config.VgConfig;
import se.inera.ehds.config.VgConfigLoader;
import se.inera.ehds.mapping.tk.MappedDocumentEntry;
import se.inera.ehds.model.EiEngagement;
import se.inera.ehds.service.EiService;
import se.inera.ehds.service.FhirProxyClient;
import se.inera.ehds.service.LoggService;
import se.inera.ehds.service.SparrFilterService;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class DocumentQueryOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(DocumentQueryOrchestrator.class);

    private final EiService ei;
    private final FhirProxyClient fhirClient;
    private final SparrFilterService sparr;
    private final LoggService logg;
    private final List<VgConfig> vgConfigs;
    private final VgConfigLoader vgConfigLoader;
    private final AppProperties props;

    public DocumentQueryOrchestrator(EiService ei,
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

    public Bundle searchDocumentReferences(String vgHsaId, String patientSystem, String patientValue) {
        String requestId = UUID.randomUUID().toString();

        List<VgConfig> targets = resolveTargets(vgHsaId, patientSystem, patientValue);

        List<CompletableFuture<List<MappedDocumentEntry>>> futures = targets.stream()
                .map(vg -> CompletableFuture.supplyAsync(
                        () -> fhirClient.fetchDocumentReferences(vg, patientSystem, patientValue)))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        List<MappedDocumentEntry> all = futures.stream()
                .flatMap(f -> f.join().stream())
                .collect(Collectors.toList());

        // Post-query Sparr (yttre + inre spärr via Provenance)
        all = sparr.filterDocumentReferences(all, patientSystem, patientValue);

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

    private Bundle buildBundle(String requestId, List<MappedDocumentEntry> entries) {
        Bundle b = new Bundle();
        b.setId(requestId);
        b.getMeta().setLastUpdated(new Date());
        b.setType(Bundle.BundleType.SEARCHSET);
        b.setTotal(entries.size());
        for (MappedDocumentEntry entry : entries) {
            DocumentReference dr = entry.documentReference();
            b.addEntry().setFullUrl("urn:uuid:" + dr.getId()).setResource(dr)
             .getSearch().setMode(Bundle.SearchEntryMode.MATCH);
            if (entry.provenance() != null) {
                Provenance prov = entry.provenance();
                b.addEntry().setFullUrl("urn:uuid:" + prov.getId()).setResource(prov)
                 .getSearch().setMode(Bundle.SearchEntryMode.INCLUDE);
            }
        }
        return b;
    }
}
