package se.inera.ehds.orchestration;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import se.inera.ehds.config.AppProperties;
import se.inera.ehds.config.OutputMode;
import se.inera.ehds.config.ServiceContractConfig;
import se.inera.ehds.config.SourceStrategy;
import se.inera.ehds.config.VgConfig;
import se.inera.ehds.config.VgConfigLoader;
import se.inera.ehds.mapping.rivta.doclist.GetDocumentListResponse;
import se.inera.ehds.mapping.tk.MapperContext;
import se.inera.ehds.mapping.tk.getdocumentlist.GetDocumentListMapper;
import se.inera.ehds.model.EiEngagement;
import se.inera.ehds.model.TakRoute;
import se.inera.ehds.service.EiService;
import se.inera.ehds.service.FhirPassthroughClient;
import se.inera.ehds.service.LoggService;
import se.inera.ehds.service.TakService;
import se.inera.ehds.soap.client.GetDocumentListClient;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Orchestrates the full FHIR DocumentReference search pipeline:
 *
 *   1. TAK  – get all known logical addresses for the contract
 *   2. EI   – get patient-specific logical addresses (overrides TAK list if non-empty)
 *   3. SOAP – call each logical address's physical endpoint (from TAK per logical address)
 *   4. Map  – transform GetDocumentListResponse → List<DocumentReference>
 *   5. Sparr (POST-QUERY) – filter DocumentReferences by source system blocking
 *   6. Log  – fire-and-forget ATNA/BALP audit entry
 *
 * When vgHsaId is provided, only that VG is queried (no EI lookup needed).
 * When vgHsaId is null, all VGs are aggregated using EI/TAK logic.
 */
@Service
public class DocumentQueryOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(DocumentQueryOrchestrator.class);
    private static final String EXT_SOURCE_SYSTEM =
            "https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-source-system";

    private final TakService tak;
    private final EiService ei;
    private final GetDocumentListClient soapClient;
    private final GetDocumentListMapper mapper;
    private final RestTemplate rest;
    private final LoggService logg;
    private final List<ServiceContractConfig> contracts;
    private final AppProperties props;
    private final List<VgConfig> vgConfigs;
    private final VgConfigLoader vgConfigLoader;
    private final FhirPassthroughClient fhirPassthrough;

    public DocumentQueryOrchestrator(TakService tak, EiService ei,
                                      GetDocumentListClient soapClient,
                                      GetDocumentListMapper mapper,
                                      RestTemplate rest,
                                      LoggService logg,
                                      List<ServiceContractConfig> contracts,
                                      AppProperties props,
                                      List<VgConfig> vgConfigs,
                                      VgConfigLoader vgConfigLoader,
                                      FhirPassthroughClient fhirPassthrough) {
        this.tak = tak;
        this.ei = ei;
        this.soapClient = soapClient;
        this.mapper = mapper;
        this.rest = rest;
        this.logg = logg;
        this.contracts = contracts;
        this.props = props;
        this.vgConfigs = vgConfigs;
        this.vgConfigLoader = vgConfigLoader;
        this.fhirPassthrough = fhirPassthrough;
    }

    public Bundle searchDocumentReferences(String vgHsaId, String patientSystem, String patientValue) {
        ServiceContractConfig contract = contracts.stream()
                .filter(c -> "DocumentReference".equals(c.getFhirResource()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No contract configured for DocumentReference"));

        if (contract.getOutputMode() == OutputMode.COMPOSITION_ASSEMBLY) {
            throw new UnsupportedOperationException(
                    "outputMode COMPOSITION_ASSEMBLY är inte implementerat för " + contract.getId()
                    + " – aktivera när GetCareDocumentationMapper finns");
        }

        String requestId = UUID.randomUUID().toString();

        // If VG-scoped and source is FHIR_PASSTHROUGH, delegate directly
        if (vgHsaId != null) {
            Optional<VgConfig> vgOpt = vgConfigLoader.findByHsaId(vgConfigs, vgHsaId);
            if (vgOpt.isPresent() && vgOpt.get().getSource() == SourceStrategy.FHIR_PASSTHROUGH) {
                Bundle result = fhirPassthrough.searchDocumentReferences(vgOpt.get(), patientSystem, patientValue);
                if (contract.isUseLogg()) {
                    logg.logAccess(requestId, patientValue, patientSystem, contract.getNamespace(),
                            vgHsaId, "DocumentReference",
                            result.getTotal(), props.getBridgeHsaId());
                }
                return result;
            }
        }

        // Determine logical addresses to query
        List<String> logicalAddresses;
        if (vgHsaId != null) {
            // VG-scoped SOAP: TAK is authoritative, skip EI
            logicalAddresses = List.of(vgHsaId);
        } else {
            // Global: use TAK + EI
            List<TakRoute> takRoutes = contract.isUseTAK()
                    ? tak.getRoutes(contract.getNamespace())
                    : List.of();

            if (contract.isUseEI()) {
                List<EiEngagement> engagements = ei.getEngagements(patientSystem, patientValue, contract.getNamespace());
                logicalAddresses = engagements.isEmpty()
                        ? takRoutes.stream().map(TakRoute::getLogicalAddress).toList()
                        : engagements.stream().map(EiEngagement::getLogicalAddress).toList();
            } else {
                logicalAddresses = takRoutes.stream().map(TakRoute::getLogicalAddress).toList();
            }
        }

        // Async SOAP calls + mapping
        List<CompletableFuture<List<DocumentReference>>> futures = logicalAddresses.stream()
                .map(la -> CompletableFuture.supplyAsync(
                        () -> callSoapAndMap(la, contract, patientSystem, patientValue)))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        List<DocumentReference> allDocuments = futures.stream()
                .flatMap(f -> f.join().stream())
                .collect(Collectors.toList());

        // Step 5: POST-QUERY Sparr filter
        if (contract.isUseSparr()) {
            allDocuments = filterBySparr(allDocuments, patientSystem, patientValue);
        }

        // Step 6: Audit log (fire-and-forget)
        if (contract.isUseLogg()) {
            String firstLogical = logicalAddresses.isEmpty() ? "none" : logicalAddresses.get(0);
            logg.logAccess(requestId, patientValue, patientSystem, contract.getNamespace(),
                    firstLogical, "DocumentReference", allDocuments.size(), props.getBridgeHsaId());
        }

        return buildBundle(requestId, allDocuments);
    }

    private List<DocumentReference> callSoapAndMap(String la, ServiceContractConfig contract,
                                                    String patientSystem, String patientValue) {
        String physUrl = tak.getPhysicalAddress(contract.getNamespace(), la);
        if (physUrl == null) {
            log.warn("TAK: no route for {}", la);
            return List.of();
        }
        String root = patientSystem.startsWith("urn:oid:") ? patientSystem.substring(8) : patientSystem;
        try {
            GetDocumentListResponse r = soapClient.call(physUrl, la, root, patientValue);
            List<DocumentReference> docs = mapper.map(r, new MapperContext(patientSystem, patientValue));
            log.debug("Got {} DocumentReferences from {}", docs.size(), la);
            return docs;
        } catch (Exception e) {
            log.error("SOAP failed for {}: {}", la, e.getMessage());
            return List.of();
        }
    }

    private List<DocumentReference> filterBySparr(List<DocumentReference> documents,
                                                    String patientSystem, String patientValue) {
        if (documents.isEmpty()) return documents;
        Map<String, Boolean> blockedCache = new HashMap<>();

        return documents.stream()
                .filter(dr -> {
                    String hsaId = extractSourceHsaId(dr);
                    if (hsaId == null) return true; // fail-open

                    boolean blocked = blockedCache.computeIfAbsent(hsaId,
                            id -> checkBlocked(patientSystem, patientValue, id));
                    if (blocked) {
                        log.info("Sparr: blocking DocumentReference from {} for patient {}",
                                hsaId, patientValue);
                    }
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
            Map<String, String> req = Map.of(
                    "patientSystem", patientSystem,
                    "patientId", patientId,
                    "sourceSystem", sourceSystem);
            Map<String, Object> resp = rest.postForObject(
                    props.getSparrUrl() + "/check", req, Map.class);
            return Boolean.TRUE.equals(resp != null ? resp.get("blocked") : Boolean.FALSE);
        } catch (Exception e) {
            log.warn("Sparr check failed for {}: {} — failing open", sourceSystem, e.getMessage());
            return false;
        }
    }

    private Bundle buildBundle(String requestId, List<DocumentReference> documents) {
        Bundle bundle = new Bundle();
        bundle.setId(requestId);
        bundle.getMeta().setLastUpdated(new Date());
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.setTotal(documents.size());
        for (DocumentReference dr : documents) {
            bundle.addEntry()
                    .setFullUrl("urn:uuid:" + dr.getId())
                    .setResource(dr)
                    .getSearch().setMode(Bundle.SearchEntryMode.MATCH);
        }
        return bundle;
    }
}
