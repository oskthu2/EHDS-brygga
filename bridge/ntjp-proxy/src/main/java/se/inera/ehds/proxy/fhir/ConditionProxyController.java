package se.inera.ehds.proxy.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.inera.ehds.mapping.naming.NamingSystemRegistry;
import se.inera.ehds.mapping.rivta.GetDiagnosisResponse;
import se.inera.ehds.mapping.tk.MapperContext;
import se.inera.ehds.mapping.tk.MappedDiagnosisEntry;
import se.inera.ehds.mapping.tk.getdiagnosis.GetDiagnosisMapper;
import se.inera.ehds.proxy.config.ProxyProperties;
import se.inera.ehds.proxy.discovery.AccessTokenService;
import se.inera.ehds.proxy.discovery.CatalogDiscoveryService;
import se.inera.ehds.proxy.service.ProxyAuditService;
import se.inera.ehds.soap.client.GetDiagnosisClient;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/fhir/{vgHsaId}")
public class ConditionProxyController {

    private static final Logger log = LoggerFactory.getLogger(ConditionProxyController.class);
    private static final String NS_RIV = "urn:riv:clinicalprocess:activity:conditions:GetDiagnosisResponder:2";

    private final GetDiagnosisClient soapClient;
    private final GetDiagnosisMapper mapper;
    private final NamingSystemRegistry namingRegistry;
    private final IParser fhirParser;
    private final ProxyProperties proxyProps;
    private final CatalogDiscoveryService discovery;
    private final AccessTokenService tokenService;
    private final ProxyAuditService audit;

    public ConditionProxyController(GetDiagnosisClient soapClient,
                                     GetDiagnosisMapper mapper,
                                     NamingSystemRegistry namingRegistry,
                                     FhirContext fhirContext,
                                     ProxyProperties proxyProps,
                                     CatalogDiscoveryService discovery,
                                     AccessTokenService tokenService,
                                     ProxyAuditService audit) {
        this.soapClient = soapClient;
        this.mapper = mapper;
        this.namingRegistry = namingRegistry;
        this.fhirParser = fhirContext.newJsonParser().setPrettyPrint(true);
        this.proxyProps = proxyProps;
        this.discovery = discovery;
        this.tokenService = tokenService;
        this.audit = audit;
    }

    @GetMapping(value = "/Condition", produces = "application/fhir+json")
    public ResponseEntity<String> searchConditions(
            @PathVariable String vgHsaId,
            @RequestParam("patient.identifier") String patientIdentifier) {

        String[] parts = patientIdentifier.split("\\|", 2);
        String patientSystem = parts.length == 2 ? parts[0] : "http://electronichealth.se/identifier/personnummer";
        String patientValue  = parts.length == 2 ? parts[1] : parts[0];

        // F1 — verifiera aktivt federationsmedlemskap innan anrop görs.
        if (!discovery.verifyActiveMembership(vgHsaId)) {
            log.warn("F1-medlemsverifiering nekade anrop mot vgHsaId={}", vgHsaId);
            audit.logProxyFetch(UUID.randomUUID().toString(), patientValue, patientSystem,
                    vgHsaId, "Condition", 0, false, null);
            return ResponseEntity.ok(emptyBundle());
        }

        // T1 — slå upp fysisk adress för GetDiagnosis-endpointen hos vgHsaId.
        Optional<String> endpointAddress = discovery.resolveEndpointAddress(vgHsaId, NS_RIV);
        if (endpointAddress.isEmpty()) {
            log.warn("T1-tjänstesökning hittade ingen GetDiagnosis-endpoint för vgHsaId={}", vgHsaId);
            audit.logProxyFetch(UUID.randomUUID().toString(), patientValue, patientSystem,
                    vgHsaId, "Condition", 0, false, null);
            return ResponseEntity.ok(emptyBundle());
        }

        String root = namingRegistry.uriToOid(patientSystem);
        List<MappedDiagnosisEntry> entries;
        try {
            String accessToken = tokenService.fetchAccessToken();
            GetDiagnosisResponse response = soapClient.call(endpointAddress.get(), vgHsaId, root, patientValue, accessToken);
            MapperContext ctx = new MapperContext(patientSystem, patientValue, proxyProps.getBridgeHsaId());
            entries = mapper.map(response, ctx);
            log.debug("GetDiagnosis för {} returnerade {} Condition(s)", vgHsaId, entries.size());
            audit.logProxyFetch(UUID.randomUUID().toString(), patientValue, patientSystem,
                    vgHsaId, "Condition", entries.size(), true, endpointAddress.get());
        } catch (Exception e) {
            log.error("SOAP-fel mot {}: {}", vgHsaId, e.getMessage());
            audit.logProxyFetch(UUID.randomUUID().toString(), patientValue, patientSystem,
                    vgHsaId, "Condition", 0, false, endpointAddress.get());
            return ResponseEntity.ok(emptyBundle());
        }

        Bundle bundle = buildBundle(entries);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/fhir+json"))
                .body(fhirParser.encodeResourceToString(bundle));
    }

    private Bundle buildBundle(List<MappedDiagnosisEntry> entries) {
        Bundle b = new Bundle();
        b.setId(UUID.randomUUID().toString());
        b.getMeta().setLastUpdated(new Date());
        b.setType(Bundle.BundleType.SEARCHSET);
        b.setTotal(entries.size());
        for (MappedDiagnosisEntry e : entries) {
            b.addEntry().setFullUrl("urn:uuid:" + e.condition().getId()).setResource(e.condition())
             .getSearch().setMode(Bundle.SearchEntryMode.MATCH);
            if (e.provenance() != null) {
                b.addEntry().setFullUrl("urn:uuid:" + e.provenance().getId()).setResource(e.provenance())
                 .getSearch().setMode(Bundle.SearchEntryMode.INCLUDE);
            }
        }
        return b;
    }

    private String emptyBundle() {
        return fhirParser.encodeResourceToString(buildBundle(List.of()));
    }
}
