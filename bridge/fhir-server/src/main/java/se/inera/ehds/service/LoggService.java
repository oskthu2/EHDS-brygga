package se.inera.ehds.service;

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
import se.inera.ehds.config.AppProperties;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Loggar patientdataåtkomst som FHIR AuditEvent-resurser och POSTar dem asynkront
 * till audit-databasens /AuditEvent-endpoint.
 *
 * SmartContext (client_id, userId, purpose) extraheras av resursprovidern från
 * Authorization-headern och trådas vidare utan signaturverifiering — tilliten
 * bygger på att gatewayen redan validerat tokenet.
 */
@Service
public class LoggService {

    private static final Logger log = LoggerFactory.getLogger(LoggService.class);

    private static final String DCM = "http://dicom.nema.org/resources/ontology/DCM";
    private static final String AUDIT_ENTITY_TYPE = "http://terminology.hl7.org/CodeSystem/audit-entity-type";
    private static final String OBJECT_ROLE = "http://terminology.hl7.org/CodeSystem/object-role";
    private static final String EHDS_SUBTYPE_CS = "https://ehds-brygga.inera.se/fhir/CodeSystem/audit-event-subtype";
    private static final String PURPOSE_CS = "http://terminology.hl7.org/CodeSystem/v3-ActReason";
    private static final String EXTRA_ROLE_CS = "http://terminology.hl7.org/CodeSystem/extra-security-role-type";

    private final FhirContext fhirCtx = FhirContext.forR4Cached();
    private final RestTemplate rest;
    private final String auditFhirUrl;

    public LoggService(RestTemplate rest, AppProperties props) {
        this.rest = rest;
        this.auditFhirUrl = props.getAuditFhirUrl();
    }

    @Async
    public void logAccess(String requestId, String patientId, String patientSystem,
                          String serviceContract, String logicalAddress,
                          String resourceType, int resultCount, String bridgeHsaId,
                          SmartContext smartContext) {
        try {
            AuditEvent ae = buildEhmAccessEvent(requestId, patientId, patientSystem,
                    logicalAddress, resourceType, resultCount, bridgeHsaId, smartContext);
            post(ae);
        } catch (Exception e) {
            log.warn("AuditEvent (ehm-access) misslyckades: {}", e.getMessage());
        }
    }

    @Async
    public void logSparrFilter(String requestId, String patientId, String patientSystem,
                               String vgHsaId, String resourceType, int resultCount,
                               boolean failClosed, String bridgeHsaId, SmartContext smartContext) {
        try {
            AuditEvent ae = buildSparrFilterEvent(requestId, patientId, patientSystem,
                    vgHsaId, resourceType, resultCount, failClosed, bridgeHsaId, smartContext);
            post(ae);
        } catch (Exception e) {
            log.warn("AuditEvent (sparr-filter) misslyckades: {}", e.getMessage());
        }
    }

    private void post(AuditEvent ae) {
        String json = fhirCtx.newJsonParser().encodeResourceToString(ae);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        rest.postForEntity(auditFhirUrl + "/AuditEvent", new HttpEntity<>(json, headers), Void.class);
    }

    private AuditEvent buildEhmAccessEvent(String requestId, String patientId, String patientSystem,
            String logicalAddress, String resourceType, int resultCount,
            String bridgeHsaId, SmartContext ctx) {
        AuditEvent ae = new AuditEvent();
        ae.setId(requestId);
        ae.getType().setSystem(DCM).setCode("110112").setDisplay("Query");
        ae.addSubtype().setSystem(EHDS_SUBTYPE_CS).setCode("ehm-access").setDisplay("eHM Access");
        ae.setAction(AuditEvent.AuditEventAction.R);
        ae.setRecorded(Date.from(Instant.now()));
        ae.setOutcome(AuditEvent.AuditEventOutcome._0);

        if (ctx.purpose() != null) {
            ae.addPurposeOfEvent().addCoding()
                    .setSystem(PURPOSE_CS).setCode(ctx.purpose());
        }

        addSmartAgents(ae, ctx, bridgeHsaId);
        ae.getSource().getObserver().setDisplay(bridgeHsaId);

        addPatientEntity(ae, patientSystem, patientId);

        String queryStr = resourceType + "?patient.identifier=" + patientSystem + "|" + patientId
                + "&vg=" + logicalAddress + "&resultCount=" + resultCount;
        AuditEvent.AuditEventEntityComponent queryEntity = ae.addEntity();
        queryEntity.getType().setSystem(AUDIT_ENTITY_TYPE).setCode("2").setDisplay("System Object");
        queryEntity.getRole().setSystem(OBJECT_ROLE).setCode("24").setDisplay("Query");
        queryEntity.setQueryElement(new Base64BinaryType(queryStr.getBytes(StandardCharsets.UTF_8)));

        return ae;
    }

    private AuditEvent buildSparrFilterEvent(String requestId, String patientId, String patientSystem,
            String vgHsaId, String resourceType, int resultCount,
            boolean failClosed, String bridgeHsaId, SmartContext ctx) {
        AuditEvent ae = new AuditEvent();
        ae.setId(requestId + "-sparr");
        ae.getType().setSystem(DCM).setCode("110112").setDisplay("Query");
        ae.addSubtype().setSystem(EHDS_SUBTYPE_CS).setCode("sparr-filter").setDisplay("Spärr Filter Applied");
        ae.setAction(AuditEvent.AuditEventAction.R);
        ae.setRecorded(Date.from(Instant.now()));
        ae.setOutcome(failClosed ? AuditEvent.AuditEventOutcome._4 : AuditEvent.AuditEventOutcome._0);

        if (ctx.purpose() != null) {
            ae.addPurposeOfEvent().addCoding()
                    .setSystem(PURPOSE_CS).setCode(ctx.purpose());
        }

        addSmartAgents(ae, ctx, bridgeHsaId);
        ae.getSource().getObserver().setDisplay(bridgeHsaId);

        addPatientEntity(ae, patientSystem, patientId);

        AuditEvent.AuditEventEntityComponent resultEntity = ae.addEntity();
        resultEntity.getType().setSystem(AUDIT_ENTITY_TYPE).setCode("2").setDisplay("System Object");
        resultEntity.getRole().setSystem(OBJECT_ROLE).setCode("13").setDisplay("Security Granule");
        resultEntity.addDetail().setType("resultCount").setValue(new StringType(String.valueOf(resultCount)));

        return ae;
    }

    /**
     * Lägger till agent[system], eventuellt agent[user], och agent[bridge].
     * requestor=true hamnar på user om present, annars på system.
     */
    private void addSmartAgents(AuditEvent ae, SmartContext ctx, String bridgeHsaId) {
        // agent[system] — eHM-applikationen
        AuditEvent.AuditEventAgentComponent systemAgent = ae.addAgent();
        systemAgent.getType().addCoding().setSystem(DCM).setCode("110150").setDisplay("Application");
        systemAgent.setRequestor(!ctx.hasUser()); // requestor only when no human user
        if (ctx.clientId() != null) {
            systemAgent.setWho(new Reference().setDisplay(ctx.clientId()));
        } else {
            systemAgent.setWho(new Reference().setDisplay("unknown-client"));
        }

        // agent[user] — inloggad vårdpersonal (optional)
        if (ctx.hasUser()) {
            AuditEvent.AuditEventAgentComponent userAgent = ae.addAgent();
            userAgent.getType().addCoding()
                    .setSystem(EXTRA_ROLE_CS).setCode("humanuser").setDisplay("Human User");
            userAgent.setRequestor(true);
            userAgent.setWho(new Reference().setDisplay(ctx.userId()));
        }

        // agent[bridge] — fhir-server
        AuditEvent.AuditEventAgentComponent bridgeAgent = ae.addAgent();
        bridgeAgent.getType().addCoding().setSystem(DCM).setCode("110153").setDisplay("Source Role ID");
        bridgeAgent.setRequestor(false);
        bridgeAgent.setWho(new Reference().setDisplay(bridgeHsaId));
    }

    private void addPatientEntity(AuditEvent ae, String patientSystem, String patientId) {
        AuditEvent.AuditEventEntityComponent patientEntity = ae.addEntity();
        patientEntity.getType().setSystem(AUDIT_ENTITY_TYPE).setCode("1").setDisplay("Person");
        patientEntity.getRole().setSystem(OBJECT_ROLE).setCode("1").setDisplay("Patient");
        patientEntity.setWhat(new Reference()
                .setIdentifier(new Identifier().setSystem(patientSystem).setValue(patientId)));
    }
}
