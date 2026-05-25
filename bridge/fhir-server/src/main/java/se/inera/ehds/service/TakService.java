package se.inera.ehds.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import se.inera.ehds.config.AppProperties;
import se.inera.ehds.model.TakRoute;
import java.util.List;
import java.util.Map;

@Service
public class TakService {

    private static final Logger log = LoggerFactory.getLogger(TakService.class);
    private final RestTemplate rest;
    private final String takUrl;

    public TakService(RestTemplate rest, AppProperties props) {
        this.rest = rest;
        this.takUrl = props.getTakUrl();
    }

    @SuppressWarnings("unchecked")
    public List<TakRoute> getRoutes(String namespace) {
        try {
            String url = takUrl + "/routing?namespace=" + namespace;
            Map<String, Object> body = rest.getForObject(url, Map.class);
            if (body == null) return List.of();
            List<Map<String, String>> routes = (List<Map<String, String>>) body.get("routes");
            if (routes == null) return List.of();
            return routes.stream().map(r -> {
                TakRoute route = new TakRoute();
                route.setLogicalAddress(r.get("logicalAddress"));
                route.setPhysicalAddress(r.get("physicalAddress"));
                route.setDescription(r.get("description"));
                return route;
            }).toList();
        } catch (Exception e) {
            log.error("TAK getRoutes error: {}", e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    public String getPhysicalAddress(String namespace, String logicalAddress) {
        try {
            String url = takUrl + "/routing/address?namespace=" + namespace
                    + "&logicalAddress=" + logicalAddress;
            Map<String, String> body = rest.getForObject(url, Map.class);
            return body != null ? body.get("physicalAddress") : null;
        } catch (Exception e) {
            log.warn("TAK getPhysicalAddress error for {}: {}", logicalAddress, e.getMessage());
            return null;
        }
    }
}
