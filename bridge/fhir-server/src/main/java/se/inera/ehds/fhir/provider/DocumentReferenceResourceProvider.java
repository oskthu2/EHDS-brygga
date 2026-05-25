package se.inera.ehds.fhir.provider;

import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.springframework.stereotype.Component;
import se.inera.ehds.orchestration.DocumentQueryOrchestrator;

@Component
public class DocumentReferenceResourceProvider implements IResourceProvider {

    private final DocumentQueryOrchestrator orchestrator;

    public DocumentReferenceResourceProvider(DocumentQueryOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Override
    public Class<DocumentReference> getResourceType() {
        return DocumentReference.class;
    }

    /**
     * GET /DocumentReference?patient.identifier=<system>|<value>
     *
     * When called via /fhir/{vgHsaId}/DocumentReference, the TenantInterceptor
     * sets the "vgHsaId" attribute on RequestDetails.
     */
    @Search
    public Bundle searchDocumentReferences(
            @RequiredParam(name = "patient.identifier") TokenParam patientIdentifier,
            RequestDetails requestDetails
    ) {
        String vgHsaId = (String) requestDetails.getAttribute("vgHsaId");
        String system = patientIdentifier.getSystem() != null && !patientIdentifier.getSystem().isBlank()
                ? patientIdentifier.getSystem()
                : "urn:oid:1.2.752.129.2.1.3.1";
        return orchestrator.searchDocumentReferences(vgHsaId, system, patientIdentifier.getValue());
    }
}
