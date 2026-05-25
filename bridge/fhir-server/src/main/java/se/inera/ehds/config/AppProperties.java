package se.inera.ehds.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ehds")
public class AppProperties {

    private String takUrl = "http://mock-tak:4001";
    private String eiUrl = "http://mock-ei:4002";
    private String sparrUrl = "http://mock-sparr:4003";
    private String loggUrl = "http://mock-logg:4004";

    /** Bridge's own HSA-id used as service consumer identifier towards NTjP */
    private String bridgeHsaId = "SE2321000999-EHDS";

    public String getTakUrl() { return takUrl; }
    public void setTakUrl(String takUrl) { this.takUrl = takUrl; }
    public String getEiUrl() { return eiUrl; }
    public void setEiUrl(String eiUrl) { this.eiUrl = eiUrl; }
    public String getSparrUrl() { return sparrUrl; }
    public void setSparrUrl(String sparrUrl) { this.sparrUrl = sparrUrl; }
    public String getLoggUrl() { return loggUrl; }
    public void setLoggUrl(String loggUrl) { this.loggUrl = loggUrl; }
    public String getBridgeHsaId() { return bridgeHsaId; }
    public void setBridgeHsaId(String bridgeHsaId) { this.bridgeHsaId = bridgeHsaId; }
}
