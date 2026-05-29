package se.inera.ehds.proxy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ntjp")
public class ProxyProperties {
    private String takUrl = "http://mock-tak:4001";
    private String bridgeHsaId = "SE2321000999-EHDS";

    public String getTakUrl() { return takUrl; }
    public void setTakUrl(String takUrl) { this.takUrl = takUrl; }
    public String getBridgeHsaId() { return bridgeHsaId; }
    public void setBridgeHsaId(String bridgeHsaId) { this.bridgeHsaId = bridgeHsaId; }
}
