package se.inera.ehds.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.inera.ehds.config.VgConfig;
import se.inera.ehds.mapping.tk.MappedDiagnosisEntry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FHIR-klient mot VG:ens endpoint – oavsett om det är en ntjp-proxy,
 * en fristående proxy nära VG, eller ett nativt FHIR-API.
 * Bryggan behöver inte veta vilket.
 */
@Service
public class FhirProxyClient {

    private static final Logger log = LoggerFactory.getLogger(FhirProxyClient.class);
    private final FhirContext ctx = FhirContext.forR4Cached();

    public List<MappedDiagnosisEntry> fetchConditions(VgConfig vg, String patientSystem, String patientValue) {
        try {
            IGenericClient client = ctx.newRestfulGenericClient(vg.getFhirEndpointUrl());
            Bundle bundle = client.search()
                    .forResource(Condition.class)
                    .where(new TokenClientParam("patient.identifier")
                            .exactly().systemAndCode(patientSystem, patientValue))
                    .returnBundle(Bundle.class)
                    .execute();
            return pairConditionsWithProvenances(bundle);
        } catch (Exception e) {
            log.error("FHIR-anrop mot {} misslyckades: {}", vg.getVgHsaId(), e.getMessage());
            return List.of();
        }
    }

    private List<MappedDiagnosisEntry> pairConditionsWithProvenances(Bundle bundle) {
        // Index provenances by the condition id they target
        Map<String, Provenance> provByConditionId = new HashMap<>();
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.getResource() instanceof Provenance prov) {
                for (Reference target : prov.getTarget()) {
                    String ref = target.getReference();
                    if (ref != null) {
                        // ref is "urn:uuid:{conditionId}" — strip prefix
                        String id = ref.startsWith("urn:uuid:") ? ref.substring(9) : ref;
                        provByConditionId.put(id, prov);
                    }
                }
            }
        }
        return bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Condition)
                .map(e -> {
                    Condition c = (Condition) e.getResource();
                    return new MappedDiagnosisEntry(c, provByConditionId.get(c.getId()));
                })
                .toList();
    }

    public List<DocumentReference> fetchDocumentReferences(VgConfig vg, String patientSystem, String patientValue) {
        try {
            IGenericClient client = ctx.newRestfulGenericClient(vg.getFhirEndpointUrl());
            Bundle bundle = client.search()
                    .forResource(DocumentReference.class)
                    .where(new TokenClientParam("patient.identifier")
                            .exactly().systemAndCode(patientSystem, patientValue))
                    .returnBundle(Bundle.class)
                    .execute();
            return bundle.getEntry().stream()
                    .filter(e -> e.getResource() instanceof DocumentReference)
                    .map(e -> (DocumentReference) e.getResource())
                    .toList();
        } catch (Exception e) {
            log.error("FHIR-anrop mot {} misslyckades: {}", vg.getVgHsaId(), e.getMessage());
            return List.of();
        }
    }
}
