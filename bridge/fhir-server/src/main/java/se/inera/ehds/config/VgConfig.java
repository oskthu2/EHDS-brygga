package se.inera.ehds.config;

public class VgConfig {
    private String vgHsaId;
    private String description;
    // FHIR-endpoint för denna VG: ntjp-proxy, fristående proxy nära VG, eller VG:ns egna FHIR-server.
    // Ändra URL för att flytta ansvaret utan att röra brygg-koden.
    private String fhirEndpointUrl;

    public String getVgHsaId() { return vgHsaId; }
    public void setVgHsaId(String vgHsaId) { this.vgHsaId = vgHsaId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getFhirEndpointUrl() { return fhirEndpointUrl; }
    public void setFhirEndpointUrl(String fhirEndpointUrl) { this.fhirEndpointUrl = fhirEndpointUrl; }
}
