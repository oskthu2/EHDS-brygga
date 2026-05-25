package se.inera.ehds.orchestration;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.inera.ehds.config.AppProperties;
import se.inera.ehds.config.ServiceContractConfig;
import se.inera.ehds.mapping.rivta.GetDiagnosisResponse;
import se.inera.ehds.mapping.tk.MapperContext;
import se.inera.ehds.mapping.tk.getdiagnosis.GetDiagnosisMapper;
import se.inera.ehds.model.EiEngagement;
import se.inera.ehds.model.TakRoute;
import se.inera.ehds.service.EiService;
import se.inera.ehds.service.LoggService;
import se.inera.ehds.service.SparrFilterService;
import se.inera.ehds.service.TakService;
import se.inera.ehds.soap.client.GetDiagnosisClient;

import java.util.*;

/**
 * Orchestrates the full FHIR Condition search pipeline:
 *
 *   1. TAK  – get all known logical addresses for the contract
 *   2. EI   – get patient-specific logical addresses (overrides TAK list if non-empty)
 *   3. SOAP – call each logical address's physical endpoint (from TAK per logical address)
 *   4. Map  – transform GetDiagnosisResponse → List<Condition>
 *   5. Sparr (POST-QUERY) – filter conditions by source system blocking
 *   6. Log  – fire-and-forget ATNA/BALP audit entry
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

    public QueryOrchestrator(TakService tak, EiService ei,
                              GetDiagnosisClient soapClient,
                              GetDiagnosisMapper mapper,
                              SparrFilterService sparr,
                              LoggService logg,
                              List<ServiceContractConfig> contracts,
                              AppProperties props) {
        this.tak = tak;
        this.ei = ei;
        this.soapClient = soapClient;
        this.mapper = mapper;
        this.sparr = sparr;
        this.logg = logg;
        this.contracts = contracts;
        this.props = props;
    }

    public Bundle searchConditions(String patientSystem, String patientValue) {
        ServiceContractConfig contract = contracts.stream()
                .filter(c -> "Condition".equals(c.getFhirResource()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No contract configured for Condition"));

        String requestId = UUID.randomUUID().toString();
        List<Condition> allConditions = new ArrayList<>();

        // Step 1: TAK – all registered logical addresses for this contract
        List<TakRoute> takRoutes = contract.isUseTAK()
                ? tak.getRoutes(contract.getNamespace())
                : List.of();

        // Step 2: EI – patient-specific logical addresses
        // EI tells us which VGs actually have data for this patient.
        // TAK is used as fallback if EI returns nothing.
        List<String> logicalAddresses;
        if (contract.isUseEI()) {
            List<EiEngagement> engagements = ei.getEngagements(patientSystem, patientValue, contract.getNamespace());
            logicalAddresses = engagements.isEmpty()
                    ? takRoutes.stream().map(TakRoute::getLogicalAddress).toList()
                    : engagements.stream().map(EiEngagement::getLogicalAddress).toList();
        } else {
            logicalAddresses = takRoutes.stream().map(TakRoute::getLogicalAddress).toList();
        }

        // Steps 3+4: SOAP calls + mapping – NO Sparr check here
        for (String logicalAddress : logicalAddresses) {
            // TAK is authoritative for physical URL resolution
            String physUrl = tak.getPhysicalAddress(contract.getNamespace(), logicalAddress);
            if (physUrl == null) {
                log.warn("TAK: no physical address for {} – skipping", logicalAddress);
                continue;
            }

            String patientRoot = patientSystem.startsWith("urn:oid:")
                    ? patientSystem.substring(8) : patientSystem;

            try {
                GetDiagnosisResponse response = soapClient.call(
                        physUrl, logicalAddress, patientRoot, patientValue);
                MapperContext ctx = new MapperContext(patientSystem, patientValue);
                List<Condition> conditions = mapper.map(response, ctx);
                allConditions.addAll(conditions);
                log.debug("Got {} Conditions from {}", conditions.size(), logicalAddress);
            } catch (Exception e) {
                log.error("SOAP call failed for {} ({}): {}", logicalAddress, physUrl, e.getMessage());
            }
        }

        // Step 5: POST-QUERY Sparr filter
        // This is the correct position per architecture: all data collected first, then filtered.
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
