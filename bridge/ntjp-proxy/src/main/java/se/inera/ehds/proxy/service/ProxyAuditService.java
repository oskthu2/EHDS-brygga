package se.inera.ehds.proxy.service;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import se.inera.ehds.proxy.config.ProxyProperties;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Loggar SOAP-hämtning och FHIR-konvertering som AuditEvent (SEEHDSAuditEventProxyFetch-profilen)
 * och POSTar dem asynkront till audit-databasens /AuditEvent-endpoint.
 */
@Service
public class ProxyAuditService {

    private static final Logger log = LoggerFactory.getLogger(ProxyAuditService.class);

    private static final String DCM = "http://dicom.nema.org/resources/ontology/DCM";
    private static final String AUDIT_ENTITY_TYPE = "http://terminology.hl7.org/CodeSystem/audit-entity-type";
    private static final String OBJECT_ROLE = "http://terminology.hl7.org/CodeSystem/object-role";
    private static final String EHDS_SUBTYPE_CS = "https://ehds-brygga.inera.se/fhir/CodeSystem/audit-event-subtype";

    private final FhirContext fhirCtx = FhirContext.forR4Cached();
    private final RestTemplate rest;
    private final String auditFhirUrl;
    private final String bridgeHsaId;

    public ProxyAuditService(RestTemplate rest, ProxyProperties props) {
        this.rest = rest;
        this.auditFhirUrl = props.getAuditFhirUrl();
        this.bridgeHsaId = props.getBridgeHsaId();
    }

    /**
     * @param resolvedAddress Fysisk adress uppslagen via T1 (tjänstekatalogen), eller
     *                        {@code null} om anropet aldrig nådde så långt (t.ex. nekad
     *                        F1-medlemsverifiering eller misslyckad T1-uppslagning).
     */
    @Async
    public void logProxyFetch(String requestId, String patientId, String patientSystem,
                              String vgHsaId, String resourceType, int resultCount, boolean success,
                              String resolvedAddress) {
        try {
            AuditEvent ae = buildProxyFetchEvent(requestId, patientId, patientSystem,
                    vgHsaId, resourceType, resultCount, success, resolvedAddress);
            String json = fhirCtx.newJsonParser().encodeResourceToString(ae);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            rest.postForEntity(auditFhirUrl + "/AuditEvent", new HttpEntity<>(json, headers), Void.class);
        } catch (Exception e) {
            log.warn("AuditEvent (proxy-fetch) misslyckades: {}", e.getMessage());
        }
    }

    private AuditEvent buildProxyFetchEvent(String requestId, String patientId, String patientSystem,
            String vgHsaId, String resourceType, int resultCount, boolean success, String resolvedAddress) {
        AuditEvent ae = new AuditEvent();
        ae.setId(requestId);

        ae.getType().setSystem(DCM).setCode("110112").setDisplay("Query");
        ae.addSubtype().setSystem(EHDS_SUBTYPE_CS).setCode("proxy-fetch").setDisplay("Proxy Fetch & Convert");
        ae.setAction(AuditEvent.AuditEventAction.R);
        ae.setRecorded(Date.from(Instant.now()));
        ae.setOutcome(success ? AuditEvent.AuditEventOutcome._0 : AuditEvent.AuditEventOutcome._8);

        // Agent: fhir-server (the caller, requestor=true)
        AuditEvent.AuditEventAgentComponent callerAgent = ae.addAgent();
        callerAgent.setRequestor(true);
        callerAgent.setWho(new Reference().setDisplay(bridgeHsaId));
        callerAgent.getType().addCoding().setSystem(DCM).setCode("110152").setDisplay("Destination Role ID");

        // Agent: ntjp-proxy itself (requestor=false)
        AuditEvent.AuditEventAgentComponent proxyAgent = ae.addAgent();
        proxyAgent.setRequestor(false);
        proxyAgent.setWho(new Reference().setDisplay(bridgeHsaId + ":ntjp-proxy"));
        proxyAgent.getType().addCoding().setSystem(DCM).setCode("110153").setDisplay("Source Role ID");
        if (resolvedAddress != null) {
            proxyAgent.getNetwork().setAddress(resolvedAddress);
        }

        ae.getSource().getObserver().setDisplay(bridgeHsaId + ":ntjp-proxy");

        // Entity: patient
        AuditEvent.AuditEventEntityComponent patientEntity = ae.addEntity();
        patientEntity.getType().setSystem(AUDIT_ENTITY_TYPE).setCode("1").setDisplay("Person");
        patientEntity.getRole().setSystem(OBJECT_ROLE).setCode("1").setDisplay("Patient");
        patientEntity.setWhat(new Reference()
                .setIdentifier(new Identifier().setSystem(patientSystem).setValue(patientId)));

        // Entity: query (resourceType, VG HSA-id, resultCount)
        String queryStr = resourceType + "?patient.identifier=" + patientSystem + "|" + patientId
                + "&logicalAddress=" + vgHsaId + "&resultCount=" + resultCount;
        AuditEvent.AuditEventEntityComponent queryEntity = ae.addEntity();
        queryEntity.getType().setSystem(AUDIT_ENTITY_TYPE).setCode("2").setDisplay("System Object");
        queryEntity.getRole().setSystem(OBJECT_ROLE).setCode("24").setDisplay("Query");
        queryEntity.setQueryElement(new Base64BinaryType(queryStr.getBytes(StandardCharsets.UTF_8)));

        return ae;
    }
}
