package se.inera.ehds.proxy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ntjp")
public class ProxyProperties {
    private String ntjpUrl = "http://mock-ntjp:4001";
    private String bridgeHsaId = "SE2321000999-EHDS";
    private String auditFhirUrl = "http://audit-db:8080/fhir";

    public String getNtjpUrl() { return ntjpUrl; }
    public void setNtjpUrl(String ntjpUrl) { this.ntjpUrl = ntjpUrl; }
    public String getBridgeHsaId() { return bridgeHsaId; }
    public void setBridgeHsaId(String bridgeHsaId) { this.bridgeHsaId = bridgeHsaId; }
    public String getAuditFhirUrl() { return auditFhirUrl; }
    public void setAuditFhirUrl(String auditFhirUrl) { this.auditFhirUrl = auditFhirUrl; }
}
