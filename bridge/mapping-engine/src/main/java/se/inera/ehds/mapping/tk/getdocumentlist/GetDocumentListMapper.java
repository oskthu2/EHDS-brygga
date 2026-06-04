package se.inera.ehds.mapping.tk.getdocumentlist;

import org.hl7.fhir.r4.model.*;
import se.inera.ehds.mapping.concept.ConceptMapRegistry;
import se.inera.ehds.mapping.naming.NamingSystemRegistry;
import se.inera.ehds.mapping.rivta.doclist.CVType;
import se.inera.ehds.mapping.rivta.doclist.DocumentEntry;
import se.inera.ehds.mapping.rivta.doclist.PersonIdType;
import se.inera.ehds.mapping.rivta.doclist.GetDocumentListResponse;
import se.inera.ehds.mapping.rivta.doclist.ResultType;
import se.inera.ehds.mapping.tk.MapperContext;
import se.inera.ehds.mapping.tk.MappedDocumentEntry;
import se.inera.ehds.mapping.tk.ProvenanceBuilder;
import se.inera.ehds.mapping.tk.RivDateParser;
import se.inera.ehds.mapping.tk.TkMapper;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class GetDocumentListMapper implements TkMapper<GetDocumentListResponse, MappedDocumentEntry> {

    private static final String CANONICAL_BASE = "https://ehds-brygga.inera.se/fhir";
    private static final String HSA_OID = "1.2.752.129.2.1.4.1";
    private static final String PROFILE_URL = CANONICAL_BASE + "/StructureDefinition/se-ehds-document-reference";

    private final NamingSystemRegistry namingSystem;
    private final ConceptMapRegistry conceptMaps;

    public GetDocumentListMapper(NamingSystemRegistry namingSystem, ConceptMapRegistry conceptMaps) {
        this.namingSystem = namingSystem;
        this.conceptMaps = conceptMaps;
    }

    @Override
    public List<MappedDocumentEntry> map(GetDocumentListResponse response, MapperContext ctx) {
        if (response == null || response.getDocumentEntry() == null) return List.of();
        ResultType result = response.getResult();
        if (result != null && !"OK".equals(result.getResultCode())) return List.of();

        return response.getDocumentEntry().stream()
                .map(d -> mapDocumentEntry(d, ctx))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private MappedDocumentEntry mapDocumentEntry(DocumentEntry entry, MapperContext ctx) {
        if (entry == null) return null;

        DocumentReference dr = new DocumentReference();
        dr.setId(UUID.randomUUID().toString());
        dr.getMeta().addProfile(PROFILE_URL);

        // status: active → CURRENT, otherwise SUPERSEDED
        String statusCode = entry.getStatusCode();
        dr.setStatus("active".equalsIgnoreCase(statusCode)
                ? Enumerations.DocumentReferenceStatus.CURRENT
                : Enumerations.DocumentReferenceStatus.SUPERSEDED);

        // type: CodeableConcept from CVType via NamingSystemRegistry OID lookup
        CVType typeCode = entry.getTypeCode();
        if (typeCode != null) {
            String codeSystem = namingSystem.oidToUri(typeCode.getCodeSystem());
            String typeText = typeCode.getOriginalText() != null ? typeCode.getOriginalText() : typeCode.getDisplayName();
            dr.setType(new CodeableConcept()
                    .addCoding(new Coding()
                            .setSystem(codeSystem)
                            .setCode(typeCode.getCode())
                            .setDisplay(typeCode.getDisplayName()))
                    .setText(typeText));
        }

        // subject: patient identifier
        PersonIdType pid = entry.getPatientId();
        if (pid != null) {
            dr.setSubject(new Reference().setIdentifier(
                    new Identifier()
                            .setSystem(namingSystem.oidToUri(pid.getRoot()))
                            .setValue(pid.getExtension())));
        }

        // date from documentTime — InstantType requires full timestamp (no day-precision)
        if (entry.getDocumentTime() != null) {
            String iso = RivDateParser.parse(entry.getDocumentTime());
            if (iso != null) {
                dr.setDateElement(new InstantType(iso.length() == 10 ? iso + "T00:00:00Z" : iso + "Z"));
            }
        }

        // description (title)
        if (entry.getTitle() != null) {
            dr.setDescription(entry.getTitle());
        }

        // author: careUnitHSAId – informationsägare vårdenhet (standard FHIR author field)
        String careUnitHsaId = entry.getCareUnitHSAId();
        String hsaSystem = namingSystem.oidToUri(HSA_OID);
        if (careUnitHsaId != null) {
            dr.addAuthor(new Reference().setIdentifier(
                    new Identifier().setSystem(hsaSystem).setValue(careUnitHsaId)));
        }

        // meta.source: sourceSystemHSAId as URI (urn:oid:{HSA_OID}#{hsaId})
        String sourceHsaId = entry.getSourceSystemHSAId();
        if (sourceHsaId != null) {
            dr.getMeta().setSource(hsaSystem + "#" + sourceHsaId);
        }

        // content: default PDF attachment
        DocumentReference.DocumentReferenceContentComponent content =
                new DocumentReference.DocumentReferenceContentComponent();
        Attachment attachment = new Attachment();
        attachment.setContentType("application/pdf");
        if (entry.getTitle() != null) {
            attachment.setTitle(entry.getTitle());
        }
        content.setAttachment(attachment);
        dr.addContent(content);

        Provenance prov = ProvenanceBuilder.build(
                dr.getId(),
                entry.getCareProviderHSAId(),
                entry.getCareUnitHSAId(),
                entry.getDocumentTime(),
                hsaSystem,
                ctx);

        return new MappedDocumentEntry(dr, prov);
    }
}
