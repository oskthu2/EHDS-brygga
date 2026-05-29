package se.inera.ehds.proxy.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import se.inera.ehds.proxy.config.ProxyProperties;

import java.util.Map;

@Service
public class ProxyTakService {

    private static final Logger log = LoggerFactory.getLogger(ProxyTakService.class);
    private final RestTemplate rest;
    private final String takUrl;

    public ProxyTakService(RestTemplate rest, ProxyProperties props) {
        this.rest = rest;
        this.takUrl = props.getTakUrl();
    }

    @SuppressWarnings("unchecked")
    public String getPhysicalAddress(String namespace, String logicalAddress) {
        try {
            String url = takUrl + "/routing/address?namespace=" + namespace
                    + "&logicalAddress=" + logicalAddress;
            Map<String, String> body = rest.getForObject(url, Map.class);
            return body != null ? body.get("physicalAddress") : null;
        } catch (Exception e) {
            log.warn("TAK lookup failed for {}/{}: {}", namespace, logicalAddress, e.getMessage());
            return null;
        }
    }
}
