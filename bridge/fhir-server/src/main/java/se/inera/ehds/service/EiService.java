package se.inera.ehds.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import se.inera.ehds.config.AppProperties;
import se.inera.ehds.mapping.naming.NamingSystemRegistry;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Engagemangsindex — talar om vilka VG:er (logiska adresser) som har data för en patient
 * inom ett givet tjänstekontrakt. Används av orkestrerarna för oscopade ("alla VG")
 * sökningar, där vgHsaId inte är känt från URL:en, för att avgöra vilka VG-endpoints
 * som ska frågas — istället för att fråga samtliga konfigurerade VG:er blint.
 *
 * PoC-begränsning: mock-ei exponerar ett förenklat HTTP-API. Produktion ska använda
 * RIVTA-kontraktet GetEngagements:1.
 */
@Service
public class EiService {

    private static final Logger log = LoggerFactory.getLogger(EiService.class);

    private final RestTemplate rest;
    private final NamingSystemRegistry namingRegistry;
    private final String eiUrl;

    public EiService(RestTemplate rest, NamingSystemRegistry namingRegistry, AppProperties props) {
        this.rest = rest;
        this.namingRegistry = namingRegistry;
        this.eiUrl = props.getEiUrl();
    }

    /**
     * @return distinkta logiska adresser (VG HSA-id:n) som har ett engagemang för
     *         patienten inom det angivna tjänstekontraktet. Tom lista om inget hittas
     *         eller om engagemangsindexet inte kan nås (fail-safe: ingen aggregerad
     *         sökning görs blint mot alla VG:er).
     */
    @SuppressWarnings("unchecked")
    public List<String> findEngagedVgHsaIds(String patientSystem, String patientValue, String rivtaNamespace) {
        // Konsumenten kan skicka patientSystem antingen som kanonisk FHIR-URI eller
        // urn:oid:-form (eller ren OID) — normalisera till kanonisk URI, samma form
        // som EI:s katalogdata använder.
        String canonicalPatientSystem = namingRegistry.oidToUri(namingRegistry.uriToOid(patientSystem));

        URI uri = UriComponentsBuilder.fromHttpUrl(eiUrl)
                .path("/engagement")
                .queryParam("patientSystem", canonicalPatientSystem)
                .queryParam("patientId", patientValue)
                .queryParam("namespace", rivtaNamespace)
                .build().encode().toUri();
        try {
            Map<String, Object> response = rest.getForObject(uri, Map.class);
            List<Map<String, Object>> engagements = response != null
                    ? (List<Map<String, Object>>) response.getOrDefault("engagements", List.of())
                    : List.of();
            return engagements.stream()
                    .map(e -> (String) e.get("logicalAddress"))
                    .filter(hsaId -> hsaId != null && !hsaId.isBlank())
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Engagemangsindex-uppslag misslyckades för namespace={}: {} — ingen VG frågas", rivtaNamespace, e.getMessage());
            return List.of();
        }
    }
}
