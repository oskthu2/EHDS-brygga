package se.inera.ehds.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ehds")
public class AppProperties {
    private String sparrUrl = "http://mock-sparr:4003";
    private String auditFhirUrl = "http://audit-db:4004/fhir";
    private String bridgeHsaId = "SE2321000999-EHDS";
    private String authBaseUrl = "http://localhost:8080";

    public String getSparrUrl() { return sparrUrl; }
    public void setSparrUrl(String sparrUrl) { this.sparrUrl = sparrUrl; }
    public String getAuditFhirUrl() { return auditFhirUrl; }
    public void setAuditFhirUrl(String auditFhirUrl) { this.auditFhirUrl = auditFhirUrl; }
    public String getBridgeHsaId() { return bridgeHsaId; }
    public void setBridgeHsaId(String bridgeHsaId) { this.bridgeHsaId = bridgeHsaId; }
    public String getAuthBaseUrl() { return authBaseUrl; }
    public void setAuthBaseUrl(String authBaseUrl) { this.authBaseUrl = authBaseUrl; }
}
