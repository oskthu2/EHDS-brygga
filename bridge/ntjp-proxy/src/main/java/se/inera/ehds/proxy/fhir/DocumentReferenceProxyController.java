package se.inera.ehds.proxy.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.inera.ehds.mapping.naming.NamingSystemRegistry;
import se.inera.ehds.mapping.rivta.doclist.GetDocumentListResponse;
import se.inera.ehds.mapping.tk.MapperContext;
import se.inera.ehds.mapping.tk.MappedDocumentEntry;
import se.inera.ehds.mapping.tk.getdocumentlist.GetDocumentListMapper;
import se.inera.ehds.proxy.service.ProxyTakService;
import se.inera.ehds.soap.client.GetDocumentListClient;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/fhir/{vgHsaId}")
public class DocumentReferenceProxyController {

    private static final Logger log = LoggerFactory.getLogger(DocumentReferenceProxyController.class);
    private static final String NAMESPACE =
            "urn:riv:clinicalprocess:healthrecord:GetDocumentListResponder:1";

    private final ProxyTakService tak;
    private final GetDocumentListClient soapClient;
    private final GetDocumentListMapper mapper;
    private final NamingSystemRegistry namingRegistry;
    private final IParser fhirParser;

    public DocumentReferenceProxyController(ProxyTakService tak,
                                             GetDocumentListClient soapClient,
                                             GetDocumentListMapper mapper,
                                             NamingSystemRegistry namingRegistry,
                                             FhirContext fhirContext) {
        this.tak = tak;
        this.soapClient = soapClient;
        this.mapper = mapper;
        this.namingRegistry = namingRegistry;
        this.fhirParser = fhirContext.newJsonParser().setPrettyPrint(true);
    }

    @GetMapping(value = "/DocumentReference", produces = "application/fhir+json")
    public ResponseEntity<String> searchDocumentReferences(
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
        List<MappedDocumentEntry> entries;
        try {
            GetDocumentListResponse response = soapClient.call(physUrl, vgHsaId, root, patientValue);
            entries = mapper.map(response, new MapperContext(patientSystem, patientValue, null));
            log.debug("GetDocumentList för {} returnerade {} DocumentReference(s)", vgHsaId, entries.size());
        } catch (Exception e) {
            log.error("SOAP-fel mot {}: {}", vgHsaId, e.getMessage());
            return ResponseEntity.ok(emptyBundle());
        }

        Bundle bundle = buildBundle(entries);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/fhir+json"))
                .body(fhirParser.encodeResourceToString(bundle));
    }

    private Bundle buildBundle(List<MappedDocumentEntry> entries) {
        Bundle b = new Bundle();
        b.setId(UUID.randomUUID().toString());
        b.getMeta().setLastUpdated(new Date());
        b.setType(Bundle.BundleType.SEARCHSET);
        b.setTotal(entries.size());
        for (MappedDocumentEntry entry : entries) {
            DocumentReference dr = entry.documentReference();
            b.addEntry().setFullUrl("urn:uuid:" + dr.getId()).setResource(dr)
             .getSearch().setMode(Bundle.SearchEntryMode.MATCH);
            b.addEntry().setFullUrl("urn:uuid:" + entry.provenance().getId()).setResource(entry.provenance())
             .getSearch().setMode(Bundle.SearchEntryMode.INCLUDE);
        }
        return b;
    }

    private String emptyBundle() {
        return fhirParser.encodeResourceToString(buildBundle(List.of()));
    }
}
