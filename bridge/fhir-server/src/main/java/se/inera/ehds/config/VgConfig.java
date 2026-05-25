package se.inera.ehds.config;

public class VgConfig {

    private String vgHsaId;
    private String description;
    private SourceStrategy source = SourceStrategy.SOAP;
    private String fhirBaseUrl; // används om source=FHIR_PASSTHROUGH

    public String getVgHsaId() { return vgHsaId; }
    public void setVgHsaId(String vgHsaId) { this.vgHsaId = vgHsaId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public SourceStrategy getSource() { return source; }
    public void setSource(SourceStrategy source) { this.source = source; }

    public String getFhirBaseUrl() { return fhirBaseUrl; }
    public void setFhirBaseUrl(String fhirBaseUrl) { this.fhirBaseUrl = fhirBaseUrl; }
}
