package se.inera.ehds.proxy.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.inera.ehds.mapping.naming.NamingSystemRegistry;
import se.inera.ehds.mapping.rivta.GetDiagnosisResponse;
import se.inera.ehds.mapping.tk.MapperContext;
import se.inera.ehds.mapping.tk.getdiagnosis.GetDiagnosisMapper;
import se.inera.ehds.proxy.service.ProxyTakService;
import se.inera.ehds.soap.client.GetDiagnosisClient;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/fhir/{vgHsaId}")
public class ConditionProxyController {

    private static final Logger log = LoggerFactory.getLogger(ConditionProxyController.class);
    private static final String NAMESPACE =
            "urn:riv:clinicalprocess:activity:conditions:GetDiagnosisResponder:2";

    private final ProxyTakService tak;
    private final GetDiagnosisClient soapClient;
    private final GetDiagnosisMapper mapper;
    private final NamingSystemRegistry namingRegistry;
    private final IParser fhirParser;

    public ConditionProxyController(ProxyTakService tak,
                                     GetDiagnosisClient soapClient,
                                     GetDiagnosisMapper mapper,
                                     NamingSystemRegistry namingRegistry,
                                     FhirContext fhirContext) {
        this.tak = tak;
        this.soapClient = soapClient;
        this.mapper = mapper;
        this.namingRegistry = namingRegistry;
        this.fhirParser = fhirContext.newJsonParser().setPrettyPrint(true);
    }

    @GetMapping(value = "/Condition", produces = "application/fhir+json")
    public ResponseEntity<String> searchConditions(
            @PathVariable String vgHsaId,
            @RequestParam("patient.identifier") String patientIdentifier) {

        String[] parts = patientIdentifier.split("\\|", 2);
        String patientSystem = parts.length == 2 ? parts[0] : "http://electronichealth.se/identifier/personnummer";
        String patientValue  = parts.length == 2 ? parts[1] : parts[0];

        String physUrl = tak.getPhysicalAddress(NAMESPACE, vgHsaId);
        if (physUrl == null) {
            log.warn("Ingen fysisk adress i TAK för {} / {}", NAMESPACE, vgHsaId);
            return ResponseEntity.ok(emptyBundle());
        }

        String root = namingRegistry.uriToOid(patientSystem);
        List<Condition> conditions;
        try {
            GetDiagnosisResponse response = soapClient.call(physUrl, vgHsaId, root, patientValue);
            conditions = mapper.map(response, new MapperContext(patientSystem, patientValue));
            log.debug("GetDiagnosis för {} returnerade {} Condition(s)", vgHsaId, conditions.size());
        } catch (Exception e) {
            log.error("SOAP-fel mot {}: {}", vgHsaId, e.getMessage());
            return ResponseEntity.ok(emptyBundle());
        }

        Bundle bundle = buildBundle(conditions);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/fhir+json"))
                .body(fhirParser.encodeResourceToString(bundle));
    }

    private Bundle buildBundle(List<Condition> conditions) {
        Bundle b = new Bundle();
        b.setId(UUID.randomUUID().toString());
        b.getMeta().setLastUpdated(new Date());
        b.setType(Bundle.BundleType.SEARCHSET);
        b.setTotal(conditions.size());
        for (Condition c : conditions) {
            b.addEntry().setFullUrl("urn:uuid:" + c.getId()).setResource(c)
             .getSearch().setMode(Bundle.SearchEntryMode.MATCH);
        }
        return b;
    }

    private String emptyBundle() {
        return fhirParser.encodeResourceToString(buildBundle(List.of()));
    }
}
