package se.inera.ehds.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import se.inera.ehds.config.AppProperties;
import java.time.Instant;
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
            Map<String, Object> entry = Map.of(
                    "type", "AuditEvent",
                    "subtype", "read",
                    "requestId", requestId,
                    "timestamp", Instant.now().toString(),
                    "patientId", patientId,
                    "patientIdSystem", patientSystem,
                    "serviceContract", serviceContract,
                    "logicalAddress", logicalAddress,
                    "resourceType", resourceType,
                    "resultCount", resultCount,
                    "agentHsaId", bridgeHsaId     // bridge as the acting agent
            );
            rest.postForEntity(loggUrl + "/log", entry, Void.class);
        } catch (Exception e) {
            log.warn("Audit log failed: {}", e.getMessage());
        }
    }
}
