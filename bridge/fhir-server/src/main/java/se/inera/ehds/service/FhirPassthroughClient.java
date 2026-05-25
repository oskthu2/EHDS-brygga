package se.inera.ehds.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DocumentReference;
import org.springframework.stereotype.Service;
import se.inera.ehds.config.VgConfig;

@Service
public class FhirPassthroughClient {

    private final FhirContext ctx = FhirContext.forR4Cached();

    public Bundle searchConditions(VgConfig vgConfig, String patientSystem, String patientValue) {
        IGenericClient client = ctx.newRestfulGenericClient(vgConfig.getFhirBaseUrl());
        return client.search()
                .forResource(Condition.class)
                .where(Condition.PATIENT.hasChainedProperty("identifier",
                        new TokenClientParam("identifier")
                                .exactly().systemAndCode(patientSystem, patientValue)))
                .returnBundle(Bundle.class)
                .execute();
    }

    public Bundle searchDocumentReferences(VgConfig vgConfig, String patientSystem, String patientValue) {
        IGenericClient client = ctx.newRestfulGenericClient(vgConfig.getFhirBaseUrl());
        return client.search()
                .forResource(DocumentReference.class)
                .where(DocumentReference.PATIENT.hasChainedProperty("identifier",
                        new TokenClientParam("identifier")
                                .exactly().systemAndCode(patientSystem, patientValue)))
                .returnBundle(Bundle.class)
                .execute();
    }
}
