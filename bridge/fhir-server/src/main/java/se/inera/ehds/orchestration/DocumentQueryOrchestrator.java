package se.inera.ehds.orchestration;

import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.inera.ehds.config.AppProperties;
import se.inera.ehds.config.VgConfig;
import se.inera.ehds.config.VgConfigLoader;
import se.inera.ehds.mapping.tk.MappedDocumentEntry;
import se.inera.ehds.service.FilterResult;
import se.inera.ehds.service.FhirProxyClient;
import se.inera.ehds.service.LoggService;
import se.inera.ehds.service.SparrFilterService;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class DocumentQueryOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(DocumentQueryOrchestrator.class);

    private final FhirProxyClient fhirClient;
    private final SparrFilterService sparr;
    private final LoggService logg;
    private final List<VgConfig> vgConfigs;
    private final VgConfigLoader vgConfigLoader;
    private final AppProperties props;

    public DocumentQueryOrchestrator(FhirProxyClient fhirClient,
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

    public Bundle searchDocumentReferences(String vgHsaId, String patientSystem, String patientValue) {
        String requestId = UUID.randomUUID().toString();

        List<VgConfig> targets = resolveTargets(vgHsaId);

        List<CompletableFuture<List<MappedDocumentEntry>>> futures = targets.stream()
                .map(vg -> CompletableFuture.supplyAsync(
                        () -> fhirClient.fetchDocumentReferences(vg, patientSystem, patientValue)))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        List<MappedDocumentEntry> all = futures.stream()
                .flatMap(f -> f.join().stream())
                .collect(Collectors.toList());

        FilterResult<MappedDocumentEntry> filtered = sparr.filterDocumentReferences(all, patientSystem, patientValue);

        logg.logSparrFilter(requestId, patientValue, patientSystem, vgHsaId, "DocumentReference",
                filtered.entries().size(), filtered.failClosed(), props.getBridgeHsaId());
        logg.logAccess(requestId, patientValue, patientSystem,
                "FHIR/DocumentReference", vgHsaId, "DocumentReference", filtered.entries().size(), props.getBridgeHsaId());

        return buildBundle(requestId, filtered.entries());
    }

    private List<VgConfig> resolveTargets(String vgHsaId) {
        if (vgHsaId == null) return List.of();
        return vgConfigLoader.findByHsaId(vgConfigs, vgHsaId)
                .filter(vg -> vg.getResource("DocumentReference").isPresent())
                .map(List::of).orElse(List.of());
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
