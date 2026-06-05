package se.inera.ehds.fhir.provider;

import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.springframework.stereotype.Component;
import se.inera.ehds.orchestration.QueryOrchestrator;
import se.inera.ehds.service.JwtClaimExtractor;
import se.inera.ehds.service.SmartContext;

@Component
public class ConditionResourceProvider implements IResourceProvider {

    private final QueryOrchestrator orchestrator;

    public ConditionResourceProvider(QueryOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Override
    public Class<Condition> getResourceType() {
        return Condition.class;
    }

    /**
     * GET /Condition?patient.identifier=<system>|<value>
     *
     * patient.identifier uses token format: system|value
     * Example: urn:oid:1.2.752.129.2.1.3.1|191212121212
     *
     * When called via /fhir/{vgHsaId}/Condition, the TenantInterceptor
     * sets the "vgHsaId" attribute on RequestDetails.
     */
    @Search
    public Bundle searchConditions(
            @RequiredParam(name = "patient.identifier") TokenParam patientIdentifier,
            RequestDetails requestDetails
    ) {
        String vgHsaId = (String) requestDetails.getAttribute("vgHsaId");
        String system = (patientIdentifier.getSystem() != null && !patientIdentifier.getSystem().isBlank())
                ? patientIdentifier.getSystem()
                : "http://electronichealth.se/identifier/personnummer";
        SmartContext smartContext = JwtClaimExtractor.extract(requestDetails.getHeader("Authorization"));
        return orchestrator.searchConditions(vgHsaId, system, patientIdentifier.getValue(), smartContext);
    }
}
