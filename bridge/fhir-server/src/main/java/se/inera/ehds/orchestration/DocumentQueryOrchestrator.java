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
import se.inera.ehds.service.SmartContext;
import se.inera.ehds.service.SparrFilterService;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Orkestrerar FHIR DocumentReference-sökning för ett VG-scopat anrop:
 *   VG-endpoint → FHIR-anrop → post-query Sparr (organisationsnivå) → Logg
 */
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

    public Bundle searchDocumentReferences(String vgHsaId, String patientSystem, String patientValue,
                                           SmartContext smartContext) {
        String requestId = UUID.randomUUID().toString();

        VgConfig vg = vgConfigLoader.findByHsaId(vgConfigs, vgHsaId)
                .filter(v -> v.getResource("DocumentReference").isPresent())
                .orElse(null);

        if (vg == null) {
            log.warn("Ingen DocumentReference-endpoint konfigurerad för vgHsaId={}", vgHsaId);
            return buildBundle(requestId, List.of());
        }

        List<MappedDocumentEntry> entries = fhirClient.fetchDocumentReferences(vg, patientSystem, patientValue);
        FilterResult<MappedDocumentEntry> filtered = sparr.filterDocumentReferences(entries, patientSystem, patientValue);

        logg.logSparrFilter(requestId, patientValue, patientSystem, vgHsaId, "DocumentReference",
                filtered.entries().size(), filtered.failClosed(), props.getBridgeHsaId(), smartContext);
        logg.logAccess(requestId, patientValue, patientSystem,
                "FHIR/DocumentReference", vgHsaId, "DocumentReference", filtered.entries().size(), props.getBridgeHsaId(), smartContext);

        return buildBundle(requestId, filtered.entries());
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
