package se.inera.ehds.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import se.inera.ehds.config.AppProperties;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class LoggService {

    private static final Logger log = LoggerFactory.getLogger(LoggService.class);
    private final RestTemplate rest;
    private final String loggUrl;

    public LoggService(RestTemplate rest, AppProperties props) {
        this.rest = rest;
        this.loggUrl = props.getLoggUrl();
    }

    /** Fire-and-forget audit log entry (ATNA/BALP stub). */
    @Async
    public void logAccess(String requestId, String patientId, String patientSystem,
                          String serviceContract, String logicalAddress,
                          String resourceType, int resultCount, String bridgeHsaId) {
        try {
            Map<String, Object> entry = new HashMap<>();
            entry.put("type", "AuditEvent");
            entry.put("subtype", "read");
            entry.put("requestId", requestId);
            entry.put("timestamp", Instant.now().toString());
            entry.put("patientId", patientId);
            entry.put("patientIdSystem", patientSystem);
            entry.put("serviceContract", serviceContract);
            entry.put("logicalAddress", logicalAddress);
            entry.put("resourceType", resourceType);
            entry.put("resultCount", resultCount);
            entry.put("agentHsaId", bridgeHsaId);
            rest.postForEntity(loggUrl + "/log", entry, Void.class);
        } catch (Exception e) {
            log.warn("Audit log failed: {}", e.getMessage());
        }
    }
}
