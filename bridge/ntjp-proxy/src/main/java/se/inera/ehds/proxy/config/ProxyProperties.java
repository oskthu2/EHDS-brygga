package se.inera.ehds.proxy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ntjp")
public class ProxyProperties {
    private String tjanstekatalogUrl = "http://mock-tjanstekatalog:4001";
    private String fedkatalogUrl = "http://mock-fedkatalog:4006";
    private String tokenIssuerUrl = "http://mock-token-issuer:4007";
    private String tokenClientId = "ehds-brygga";
    private String tokenClientSecret = "mock-secret";
    private String orgIdentifierSystem = "urn:oid:1.2.752.29.4.19";
    private String bridgeHsaId = "SE2321000999-EHDS";
    private String auditFhirUrl = "http://audit-db:8080/fhir";

    public String getTjanstekatalogUrl() { return tjanstekatalogUrl; }
    public void setTjanstekatalogUrl(String tjanstekatalogUrl) { this.tjanstekatalogUrl = tjanstekatalogUrl; }
    public String getFedkatalogUrl() { return fedkatalogUrl; }
    public void setFedkatalogUrl(String fedkatalogUrl) { this.fedkatalogUrl = fedkatalogUrl; }
    public String getTokenIssuerUrl() { return tokenIssuerUrl; }
    public void setTokenIssuerUrl(String tokenIssuerUrl) { this.tokenIssuerUrl = tokenIssuerUrl; }
    public String getTokenClientId() { return tokenClientId; }
    public void setTokenClientId(String tokenClientId) { this.tokenClientId = tokenClientId; }
    public String getTokenClientSecret() { return tokenClientSecret; }
    public void setTokenClientSecret(String tokenClientSecret) { this.tokenClientSecret = tokenClientSecret; }
    public String getOrgIdentifierSystem() { return orgIdentifierSystem; }
    public void setOrgIdentifierSystem(String orgIdentifierSystem) { this.orgIdentifierSystem = orgIdentifierSystem; }
    public String getBridgeHsaId() { return bridgeHsaId; }
    public void setBridgeHsaId(String bridgeHsaId) { this.bridgeHsaId = bridgeHsaId; }
    public String getAuditFhirUrl() { return auditFhirUrl; }
    public void setAuditFhirUrl(String auditFhirUrl) { this.auditFhirUrl = auditFhirUrl; }
}
