package se.inera.ehds.service;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import se.inera.ehds.config.VgConfig;
import se.inera.ehds.mapping.tk.MappedDiagnosisEntry;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FHIR-klient mot VG:ens endpoint – oavsett om det är en ntjp-proxy,
 * en fristående proxy nära VG, eller ett nativt FHIR-API.
 * Bryggan behöver inte veta vilket.
 *
 * Använder RestTemplate + HAPI JSON-parser direkt för att undvika att
 * HAPI generic client gör metadata-anrop (som misslyckas mot ntjp-proxy)
 * och för att bevara samtliga bundle-entries inklusive Provenance.
 */
@Service
public class FhirProxyClient {

    private static final Logger log = LoggerFactory.getLogger(FhirProxyClient.class);
    private final FhirContext ctx = FhirContext.forR4Cached();
    private final RestTemplate restTemplate = new RestTemplate();

    public List<MappedDiagnosisEntry> fetchConditions(VgConfig vg, String patientSystem, String patientValue) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(vg.getFhirEndpointUrl())
                    .path("/Condition")
                    .queryParam("patient.identifier", patientSystem + "|" + patientValue)
                    .build().encode().toUri();
            String body = restTemplate.getForObject(uri, String.class);
            Bundle bundle = (Bundle) ctx.newJsonParser().parseResource(body);
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
                    // With direct JSON parsing, id comes from the resource.id JSON field (plain UUID)
                    String condId = c.getIdElement().getIdPart();
                    if (condId != null && condId.startsWith("urn:uuid:")) {
                        condId = condId.substring(9);
                    }
                    return new MappedDiagnosisEntry(c, provByConditionId.get(condId));
                })
                .toList();
    }

    public List<DocumentReference> fetchDocumentReferences(VgConfig vg, String patientSystem, String patientValue) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(vg.getFhirEndpointUrl())
                    .path("/DocumentReference")
                    .queryParam("patient.identifier", patientSystem + "|" + patientValue)
                    .build().encode().toUri();
            String body = restTemplate.getForObject(uri, String.class);
            Bundle bundle = (Bundle) ctx.newJsonParser().parseResource(body);
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
