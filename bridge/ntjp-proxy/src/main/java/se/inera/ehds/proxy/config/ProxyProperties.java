package se.inera.ehds.proxy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ntjp")
public class ProxyProperties {
    private String ntjpUrl = "http://mock-ntjp:4001";
    private String bridgeHsaId = "SE2321000999-EHDS";

    public String getNtjpUrl() { return ntjpUrl; }
    public void setNtjpUrl(String ntjpUrl) { this.ntjpUrl = ntjpUrl; }
    public String getBridgeHsaId() { return bridgeHsaId; }
    public void setBridgeHsaId(String bridgeHsaId) { this.bridgeHsaId = bridgeHsaId; }
}
