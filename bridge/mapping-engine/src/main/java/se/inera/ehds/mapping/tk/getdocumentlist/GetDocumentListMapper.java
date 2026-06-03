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
import se.inera.ehds.mapping.tk.TkMapper;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class GetDocumentListMapper implements TkMapper<GetDocumentListResponse, DocumentReference> {

    private static final String CANONICAL_BASE = "https://ehds-brygga.inera.se/fhir";
    private static final String HSA_OID = "1.2.752.129.2.1.4.1";
    private static final String EXT_SOURCE_SYSTEM = CANONICAL_BASE + "/StructureDefinition/ext-source-system";
    private static final String EXT_CARE_PROVIDER  = CANONICAL_BASE + "/StructureDefinition/ext-care-provider";
    private static final String PROFILE_URL = CANONICAL_BASE + "/StructureDefinition/se-ehds-document-reference";

    private final NamingSystemRegistry namingSystem;
    private final ConceptMapRegistry conceptMaps;

    public GetDocumentListMapper(NamingSystemRegistry namingSystem, ConceptMapRegistry conceptMaps) {
        this.namingSystem = namingSystem;
        this.conceptMaps = conceptMaps;
    }

    @Override
    public List<DocumentReference> map(GetDocumentListResponse response, MapperContext ctx) {
        if (response == null || response.getDocumentEntry() == null) return List.of();
        ResultType result = response.getResult();
        if (result != null && !"OK".equals(result.getResultCode())) return List.of();

        return response.getDocumentEntry().stream()
                .map(d -> mapDocumentEntry(d, ctx))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private DocumentReference mapDocumentEntry(DocumentEntry entry, MapperContext ctx) {
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

        // date from documentTime
        if (entry.getDocumentTime() != null) {
            dr.setDateElement(new InstantType(parseRivDate(entry.getDocumentTime())));
        }

        // description (title)
        if (entry.getTitle() != null) {
            dr.setDescription(entry.getTitle());
        }

        // author: careUnit HSA-id
        String careUnitHsaId = entry.getCareUnitHSAId();
        if (careUnitHsaId != null) {
            String hsaSystem = namingSystem.oidToUri(HSA_OID);
            dr.addAuthor(new Reference().setIdentifier(
                    new Identifier().setSystem(hsaSystem).setValue(careUnitHsaId)));
        }

        // custodian: careProvider HSA-id
        String careProviderHsaId = entry.getCareProviderHSAId();
        if (careProviderHsaId != null) {
            String hsaSystem = namingSystem.oidToUri(HSA_OID);
            dr.setCustodian(new Reference().setIdentifier(
                    new Identifier().setSystem(hsaSystem).setValue(careProviderHsaId)));
            // ext-care-provider: organisationsnivå-identifierare för Sparr (samma mönster som Condition)
            Extension extCp = new Extension(EXT_CARE_PROVIDER);
            extCp.setValue(new Identifier().setSystem(hsaSystem).setValue(careProviderHsaId));
            dr.addExtension(extCp);
        }

        // extension: ext-source-system (used by SparrFilterService)
        String sourceHsaId = entry.getSourceSystemHSAId();
        if (sourceHsaId != null) {
            String hsaSystem = namingSystem.oidToUri(HSA_OID);
            Extension ext = new Extension(EXT_SOURCE_SYSTEM);
            ext.setValue(new Identifier().setSystem(hsaSystem).setValue(sourceHsaId));
            dr.addExtension(ext);
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

        return dr;
    }

    /**
     * Convert RIVTA date string to ISO 8601.
     * YYYYMMDD → YYYY-MM-DD
     * YYYYMMDDHHmmss → YYYY-MM-DDTHH:mm:ss
     */
    private String parseRivDate(String d) {
        if (d == null || d.isBlank()) return null;
        String s = d.trim();
        if (s.length() == 8) {
            return s.substring(0, 4) + "-" + s.substring(4, 6) + "-" + s.substring(6, 8);
        }
        if (s.length() >= 14) {
            return s.substring(0, 4) + "-" + s.substring(4, 6) + "-" + s.substring(6, 8)
                    + "T" + s.substring(8, 10) + ":" + s.substring(10, 12) + ":" + s.substring(12, 14);
        }
        return s;
    }
}
