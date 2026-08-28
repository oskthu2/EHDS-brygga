package se.inera.ehds.proxy.discovery;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.OrganizationAffiliation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import se.inera.ehds.proxy.config.ProxyProperties;

import java.net.URI;
import java.util.Optional;

/**
 * T1 (tjänstesökning) och F1 (medlemsverifiering) mot katalogtjänsterna, enligt mönstret
 * i Ineras "T2-katalogtjänster"-demomiljö: bryggan slår själv upp producentens fysiska
 * adress och verifierar federationsmedlemskap innan anrop görs — istället för att förlita
 * sig på en implicit NTjP-routingtabell.
 */
@Service
public class CatalogDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(CatalogDiscoveryService.class);

    private final RestTemplate restTemplate;
    private final FhirContext fhirContext;
    private final ProxyProperties props;

    public CatalogDiscoveryService(RestTemplate restTemplate, FhirContext fhirContext, ProxyProperties props) {
        this.restTemplate = restTemplate;
        this.fhirContext = fhirContext;
        this.props = props;
    }

    /**
     * T1 — slår upp den fysiska adressen för en VG:s endpoint som implementerar
     * ett givet RIVTA-tjänstekontrakt.
     */
    public Optional<String> resolveEndpointAddress(String vgHsaId, String rivtaNamespace) {
        URI uri = UriComponentsBuilder.fromHttpUrl(props.getTjanstekatalogUrl())
                .path("/Endpoint")
                .queryParam("organization.identifier", props.getOrgIdentifierSystem() + "|" + vgHsaId)
                .queryParam("status", "active")
                .queryParam("implements", rivtaNamespace)
                .build().encode().toUri();
        try {
            String body = restTemplate.getForObject(uri, String.class);
            Bundle bundle = (Bundle) fhirContext.newJsonParser().parseResource(body);
            return bundle.getEntry().stream()
                    .filter(e -> e.getResource() instanceof Endpoint)
                    .map(e -> (Endpoint) e.getResource())
                    .map(Endpoint::getAddress)
                    .findFirst();
        } catch (Exception e) {
            log.error("T1-tjänstesökning mot tjänstekatalogen misslyckades för vgHsaId={}: {}", vgHsaId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * F1 — verifierar att VG:n är en aktiv medlem i federationen innan anrop görs.
     */
    public boolean verifyActiveMembership(String vgHsaId) {
        URI uri = UriComponentsBuilder.fromHttpUrl(props.getFedkatalogUrl())
                .path("/OrganizationAffiliation")
                .queryParam("participating-organization.identifier", props.getOrgIdentifierSystem() + "|" + vgHsaId)
                .queryParam("active", "true")
                .build().encode().toUri();
        try {
            String body = restTemplate.getForObject(uri, String.class);
            Bundle bundle = (Bundle) fhirContext.newJsonParser().parseResource(body);
            return bundle.getEntry().stream()
                    .anyMatch(e -> e.getResource() instanceof OrganizationAffiliation);
        } catch (Exception e) {
            log.error("F1-medlemsverifiering mot federationsmedlemskatalogen misslyckades för vgHsaId={}: {}", vgHsaId, e.getMessage());
            return false;
        }
    }
}
