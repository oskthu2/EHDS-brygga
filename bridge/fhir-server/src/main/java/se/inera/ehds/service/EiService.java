package se.inera.ehds.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import se.inera.ehds.config.AppProperties;
import se.inera.ehds.model.EiEngagement;
import java.util.List;
import java.util.Map;

@Service
public class EiService {

    private static final Logger log = LoggerFactory.getLogger(EiService.class);
    private final RestTemplate rest;
    private final String eiUrl;

    public EiService(RestTemplate rest, AppProperties props) {
        this.rest = rest;
        this.eiUrl = props.getEiUrl();
    }

    @SuppressWarnings("unchecked")
    public List<EiEngagement> getEngagements(String patientSystem, String patientId, String namespace) {
        try {
            StringBuilder url = new StringBuilder(eiUrl)
                .append("/engagement?patientSystem=").append(patientSystem)
                .append("&patientId=").append(patientId);
            if (namespace != null && !namespace.isBlank()) {
                url.append("&namespace=").append(namespace);
            }
            Map<String, Object> body = rest.getForObject(url.toString(), Map.class);
            if (body == null) return List.of();
            List<Map<String, String>> engs = (List<Map<String, String>>) body.get("engagements");
            if (engs == null) return List.of();
            return engs.stream().map(e -> {
                EiEngagement eng = new EiEngagement();
                eng.setPatientId(e.get("patientId"));
                eng.setPatientSystem(e.get("patientSystem"));
                eng.setNamespace(e.get("namespace"));
                eng.setLogicalAddress(e.get("logicalAddress"));
                return eng;
            }).toList();
        } catch (Exception e) {
            log.error("EI getEngagements error: {}", e.getMessage());
            return List.of();
        }
    }
}
