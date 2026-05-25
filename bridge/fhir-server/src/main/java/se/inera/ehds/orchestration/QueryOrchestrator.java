package se.inera.ehds.orchestration;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.inera.ehds.config.AppProperties;
import se.inera.ehds.config.ServiceContractConfig;
import se.inera.ehds.config.SourceStrategy;
import se.inera.ehds.config.VgConfig;
import se.inera.ehds.config.VgConfigLoader;
import se.inera.ehds.mapping.rivta.GetDiagnosisResponse;
import se.inera.ehds.mapping.tk.MapperContext;
import se.inera.ehds.mapping.tk.getdiagnosis.GetDiagnosisMapper;
import se.inera.ehds.model.EiEngagement;
import se.inera.ehds.model.TakRoute;
import se.inera.ehds.service.EiService;
import se.inera.ehds.service.FhirPassthroughClient;
import se.inera.ehds.service.LoggService;
import se.inera.ehds.service.SparrFilterService;
import se.inera.ehds.service.TakService;
import se.inera.ehds.soap.client.GetDiagnosisClient;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Orchestrates the full FHIR Condition search pipeline:
 *
 *   1. TAK  – get all known logical addresses for the contract
 *   2. EI   – get patient-specific logical addresses (overrides TAK list if non-empty)
 *   3. SOAP – call each logical address's physical endpoint (from TAK per logical address)
 *   4. Map  – transform GetDiagnosisResponse → List<Condition>
 *   5. Sparr (POST-QUERY) – filter conditions by source system blocking
 *   6. Log  – fire-and-forget ATNA/BALP audit entry
 *
 * When vgHsaId is provided, only that VG is queried (no EI lookup needed).
 * When vgHsaId is null, all VGs are aggregated using EI/TAK logic.
 */
@Service
public class QueryOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(QueryOrchestrator.class);

    private final TakService tak;
    private final EiService ei;
    private final GetDiagnosisClient soapClient;
    private final GetDiagnosisMapper mapper;
    private final SparrFilterService sparr;
    private final LoggService logg;
    private final List<ServiceContractConfig> contracts;
    private final AppProperties props;
    private final List<VgConfig> vgConfigs;
    private final VgConfigLoader vgConfigLoader;
    private final FhirPassthroughClient fhirPassthrough;

    public QueryOrchestrator(TakService tak, EiService ei,
                              GetDiagnosisClient soapClient,
                              GetDiagnosisMapper mapper,
                              SparrFilterService sparr,
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
        this.sparr = sparr;
        this.logg = logg;
        this.contracts = contracts;
        this.props = props;
        this.vgConfigs = vgConfigs;
        this.vgConfigLoader = vgConfigLoader;
        this.fhirPassthrough = fhirPassthrough;
    }

    public Bundle searchConditions(String vgHsaId, String patientSystem, String patientValue) {
        ServiceContractConfig contract = contracts.stream()
                .filter(c -> "Condition".equals(c.getFhirResource()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No contract configured for Condition"));

        String requestId = UUID.randomUUID().toString();

        // If VG-scoped and source is FHIR_PASSTHROUGH, delegate directly
        if (vgHsaId != null) {
            Optional<VgConfig> vgOpt = vgConfigLoader.findByHsaId(vgConfigs, vgHsaId);
            if (vgOpt.isPresent() && vgOpt.get().getSource() == SourceStrategy.FHIR_PASSTHROUGH) {
                Bundle result = fhirPassthrough.searchConditions(vgOpt.get(), patientSystem, patientValue);
                // Still log the access
                if (contract.isUseLogg()) {
                    logg.logAccess(requestId, patientValue, patientSystem, contract.getNamespace(),
                            vgHsaId, "Condition",
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
        List<CompletableFuture<List<Condition>>> futures = logicalAddresses.stream()
                .map(la -> CompletableFuture.supplyAsync(
                        () -> callSoapAndMap(la, contract, patientSystem, patientValue)))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        List<Condition> allConditions = futures.stream()
                .flatMap(f -> f.join().stream())
                .collect(Collectors.toList());

        // Step 5: POST-QUERY Sparr filter
        if (contract.isUseSparr()) {
            allConditions = sparr.filter(allConditions, patientSystem, patientValue);
        }

        // Step 6: Audit log (fire-and-forget)
        if (contract.isUseLogg()) {
            String firstLogical = logicalAddresses.isEmpty() ? "none" : logicalAddresses.get(0);
            logg.logAccess(requestId, patientValue, patientSystem, contract.getNamespace(),
                    firstLogical, "Condition", allConditions.size(), props.getBridgeHsaId());
        }

        return buildBundle(requestId, allConditions);
    }

    private List<Condition> callSoapAndMap(String la, ServiceContractConfig contract,
                                            String patientSystem, String patientValue) {
        String physUrl = tak.getPhysicalAddress(contract.getNamespace(), la);
        if (physUrl == null) {
            log.warn("TAK: no route for {}", la);
            return List.of();
        }
        String root = patientSystem.startsWith("urn:oid:") ? patientSystem.substring(8) : patientSystem;
        try {
            GetDiagnosisResponse r = soapClient.call(physUrl, la, root, patientValue);
            List<Condition> conditions = mapper.map(r, new MapperContext(patientSystem, patientValue));
            log.debug("Got {} Conditions from {}", conditions.size(), la);
            return conditions;
        } catch (Exception e) {
            log.error("SOAP failed for {}: {}", la, e.getMessage());
            return List.of();
        }
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
