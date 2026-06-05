package se.inera.ehds.config;

import java.util.Map;
import java.util.Optional;

/**
 * Konfiguration för en ansluten vårdgivare (VG).
 *
 * resources-mappen anger per FHIR-resurstyp vilken accessmetod (tk/fhir)
 * och endpoint-URL som används. En VG som saknar en post för en resurstyp
 * anses inte tillhandahålla den resursen — anrop avvisas med tom Bundle.
 *
 * Fungerar som auktorisationskontroll (okänt vgHsaId → avvisas),
 * routingkälla (endpointUrl styr om anrop går via ntjp-proxy eller direkt FHIR)
 * och underlag för eHM:s tjänstekatalog (vilka VG:er erbjuder vad och hur).
 */
public class VgConfig {
    private String vgHsaId;
    private String description;
    private Map<String, VgResourceConfig> resources;

    public String getVgHsaId() { return vgHsaId; }
    public void setVgHsaId(String vgHsaId) { this.vgHsaId = vgHsaId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Map<String, VgResourceConfig> getResources() { return resources; }
    public void setResources(Map<String, VgResourceConfig> resources) { this.resources = resources; }

    public Optional<VgResourceConfig> getResource(String resourceType) {
        if (resources == null) return Optional.empty();
        return Optional.ofNullable(resources.get(resourceType));
    }
}
