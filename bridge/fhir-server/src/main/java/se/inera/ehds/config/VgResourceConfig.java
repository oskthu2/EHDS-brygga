package se.inera.ehds.config;

public class VgResourceConfig {
    /** "tk" = via ntjp-proxy (RIVTA/SOAP), "fhir" = nativt FHIR-API */
    private String access;
    private String endpointUrl;

    public String getAccess() { return access; }
    public void setAccess(String access) { this.access = access; }
    public String getEndpointUrl() { return endpointUrl; }
    public void setEndpointUrl(String endpointUrl) { this.endpointUrl = endpointUrl; }
}
