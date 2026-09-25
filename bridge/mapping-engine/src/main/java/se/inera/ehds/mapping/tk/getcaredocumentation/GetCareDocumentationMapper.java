package se.inera.ehds.mapping.tk.getcaredocumentation;

import org.hl7.fhir.r4.model.*;
import se.inera.ehds.mapping.naming.NamingSystemRegistry;
import se.inera.ehds.mapping.rivta.caredocumentation.*;
import se.inera.ehds.mapping.rivta.caredocumentation.Signature;
import se.inera.ehds.mapping.tk.MapperContext;
import se.inera.ehds.mapping.tk.MappedDocumentEntry;
import se.inera.ehds.mapping.tk.ProvenanceBuilder;
import se.inera.ehds.mapping.tk.RivDateParser;
import se.inera.ehds.mapping.tk.TkMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Maps RIVTA GetCareDocumentation:3 (JoL-header v2.2) to DocumentReference + Provenance.
 *
 * Field mapping follows mapping-getcaredocumentation.md. Notable departures from the simpler
 * PatientSummaryHeader-based TKs (GetDiagnosis):
 *   - Sparr keys come directly from accessControlHeader (DES-005), not a nested
 *     accountableHealthcareProfessional block.
 *   - status is always "current" — the RIVTA body carries no status field; makulerade
 *     anteckningar are assumed not to be returned by the source system.
 *   - content is XOR: clinicalDocumentNoteText (fritext) or multimediaEntry (binary/URL).
 *   - approvedForPatient (PDL-001) and hasMore/paging (DOC-001) are intentionally left
 *     unmapped — see "Öppna frågor" in the mapping doc.
 */
public class GetCareDocumentationMapper implements TkMapper<GetCareDocumentationResponse, MappedDocumentEntry> {

    private static final String CANONICAL_BASE = "https://ehds-brygga.inera.se/fhir";
    private static final String HSA_OID = "1.2.752.129.2.1.4.1";
    private static final String PROFILE_URL = CANONICAL_BASE + "/StructureDefinition/se-ehds-document-reference";
    private static final String EXT_BLOCK_COMPARISON_TIME = CANONICAL_BASE + "/StructureDefinition/ext-block-comparison-time";
    private static final String EXT_SIGNATURE_TIME = CANONICAL_BASE + "/StructureDefinition/ext-signature-time";
    // Shared/central Inera extension — not owned by this IG, so it is not redefined here.
    // See "Öppna frågor" / DOC-004 in mapping-getcaredocumentation.md.
    private static final String EXT_DISSENTING_OPINION = "https://fhir.inera.se/StructureDefinition/dissenting-opinion";
    private static final String PRACTITIONER_ROLE_TYPE = "PractitionerRole";

    private final NamingSystemRegistry namingSystem;

    public GetCareDocumentationMapper(NamingSystemRegistry namingSystem) {
        this.namingSystem = namingSystem;
    }

    @Override
    public List<MappedDocumentEntry> map(GetCareDocumentationResponse response, MapperContext ctx) {
        if (response == null || response.getCareDocumentation() == null) return List.of();
        ResultType result = response.getResult();
        if (result != null && !"OK".equals(result.getResultCode())) return List.of();

        return response.getCareDocumentation().stream()
                .map(d -> mapCareDocumentation(d, ctx))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private MappedDocumentEntry mapCareDocumentation(CareDocumentation entry, MapperContext ctx) {
        if (entry == null) return null;
        Header header = entry.getHeader();
        Body body = entry.getBody();
        if (header == null || body == null) return null;
        AccessControlHeader ach = header.getAccessControlHeader();
        RecordType record = header.getRecord();

        DocumentReference dr = new DocumentReference();
        dr.setId(java.util.UUID.randomUUID().toString());
        dr.getMeta().addProfile(PROFILE_URL);

        // status: RIVTA body carries no status field — source system is assumed to return
        // only active documentation.
        dr.setStatus(Enumerations.DocumentReferenceStatus.CURRENT);

        String hsaSystem = namingSystem.oidToUri(HSA_OID);

        // masterIdentifier: record.recordId — källsystemets primärnyckel
        if (record != null && record.getRecordId() != null) {
            dr.setMasterIdentifier(new Identifier().setValue(record.getRecordId()));
        }

        // date: record.timestamp
        if (record != null && record.getTimestamp() != null) {
            String iso = RivDateParser.parse(record.getTimestamp());
            if (iso != null) {
                dr.setDateElement(new InstantType(iso.length() == 10 ? iso + "T00:00:00Z" : iso + "Z"));
            }
        }

        // meta.source: sourceSystemId as URI (urn:oid:{HSA_OID}#{hsaId})
        if (header.getSourceSystemId() != null) {
            dr.getMeta().setSource(hsaSystem + "#" + header.getSourceSystemId());
        }

        if (ach != null) {
            // subject: patientId
            PersonIdType pid = ach.getPatientId();
            if (pid != null) {
                dr.setSubject(new Reference().setIdentifier(
                        new Identifier()
                                .setSystem(namingSystem.oidToUri(pid.getRoot()))
                                .setValue(pid.getExtension())));
            }

            // context.related: careProcessId — no system given in the spec, value only
            if (ach.getCareProcessId() != null) {
                dr.getContext().addRelated(new Reference().setIdentifier(
                        new Identifier().setValue(ach.getCareProcessId())));
            }

            // extension[blockComparisonTime]: tidpunkt för Sparr-jämförelse
            if (ach.getBlockComparisonTime() != null) {
                String iso = RivDateParser.parse(ach.getBlockComparisonTime());
                if (iso != null) {
                    dr.addExtension(new Extension(EXT_BLOCK_COMPARISON_TIME, new DateTimeType(iso)));
                }
            }

            // approvedForPatient (PDL-001): inget beslutat FHIR-kodsystem ännu — inte mappad.
        }

        // type: clinicalDocumentNoteCode (ClinicalDocumentNoteCodeCS, OID 1.2.752.129.2.2.2.11)
        CVType noteCode = body.getClinicalDocumentNoteCode();
        if (noteCode != null) {
            String codeSystem = namingSystem.oidToUri(noteCode.getCodeSystem());
            String codeText = noteCode.getOriginalText() != null ? noteCode.getOriginalText() : noteCode.getDisplayName();
            dr.setType(new CodeableConcept()
                    .addCoding(new Coding()
                            .setSystem(codeSystem)
                            .setCode(noteCode.getCode())
                            .setDisplay(noteCode.getDisplayName()))
                    .setText(codeText));
        }

        // description: clinicalDocumentNoteTitle
        if (body.getClinicalDocumentNoteTitle() != null) {
            dr.setDescription(body.getClinicalDocumentNoteTitle());
        }

        // content: XOR clinicalDocumentNoteText / multimediaEntry
        DocumentReference.DocumentReferenceContentComponent content = buildContent(body);
        if (content != null) {
            dr.addContent(content);
        }

        // dissentingOpinion[] → extension[dissentingOpinion][]
        for (DissentingOpinion dissent : body.getDissentingOpinion()) {
            dr.addExtension(buildDissentingOpinionExtension(dissent));
        }

        // author: header.author.authorId → PractitionerRole (logical reference)
        Author author = header.getAuthor();
        if (author != null && author.getAuthorId() != null) {
            dr.addAuthor(hsaRoleRef(author.getAuthorId(), author.getName(), hsaSystem));
        }

        // authenticator: header.signature.signatureId → PractitionerRole (logical reference)
        Signature signature = header.getSignature();
        if (signature != null && signature.getSignatureId() != null) {
            dr.setAuthenticator(hsaRoleRef(signature.getSignatureId(), signature.getName(), hsaSystem));
        }
        if (signature != null && signature.getTimestamp() != null) {
            String iso = RivDateParser.parse(signature.getTimestamp());
            if (iso != null) {
                dr.addExtension(new Extension(EXT_SIGNATURE_TIME, new DateTimeType(iso)));
            }
        }

        // Provenance.recorded: author.timestamp, med record.timestamp som fallback (DOC-002 —
        // author är valfri men author.timestamp obligatorisk om author finns; utan author
        // finns ingen annan källa än record.timestamp).
        String recordedTime = (author != null && author.getTimestamp() != null)
                ? author.getTimestamp()
                : (record != null ? record.getTimestamp() : null);

        Provenance prov = ProvenanceBuilder.build(
                dr.getId(),
                ach != null ? ach.getAccountableHealthcareProvider() : null,
                ach != null ? ach.getAccountableCareUnit() : null,
                recordedTime,
                hsaSystem,
                ctx);

        return new MappedDocumentEntry(dr, prov);
    }

    private DocumentReference.DocumentReferenceContentComponent buildContent(Body body) {
        Attachment attachment = new Attachment();

        if (body.getClinicalDocumentNoteText() != null) {
            attachment.setContentType("text/plain; charset=utf-8");
            attachment.setData(body.getClinicalDocumentNoteText().getBytes(StandardCharsets.UTF_8));
        } else if (body.getMultimediaEntry() != null) {
            MultimediaEntry media = body.getMultimediaEntry();
            attachment.setContentType(media.getMediaType());
            if (media.getValue() != null) {
                attachment.setData(Base64.getDecoder().decode(media.getValue()));
            } else if (media.getReference() != null) {
                attachment.setUrl(media.getReference());
            }
        } else {
            return null;
        }

        if (body.getClinicalDocumentNoteTitle() != null) {
            attachment.setTitle(body.getClinicalDocumentNoteTitle());
        }

        DocumentReference.DocumentReferenceContentComponent content =
                new DocumentReference.DocumentReferenceContentComponent();
        content.setAttachment(attachment);
        return content;
    }

    private Extension buildDissentingOpinionExtension(DissentingOpinion dissent) {
        Extension ext = new Extension(EXT_DISSENTING_OPINION);
        if (dissent.getOpinionId() != null) {
            ext.addExtension("opinionId", new StringType(dissent.getOpinionId()));
        }
        if (dissent.getAuthorTime() != null) {
            String iso = RivDateParser.parse(dissent.getAuthorTime());
            if (iso != null) {
                ext.addExtension("authorTime", new DateTimeType(iso));
            }
        }
        if (dissent.getOpinion() != null) {
            ext.addExtension("opinion", new StringType(dissent.getOpinion()));
        }
        PersonIdType personId = dissent.getPersonId();
        if (personId != null) {
            ext.addExtension("personId", new Identifier()
                    .setSystem(namingSystem.oidToUri(personId.getRoot()))
                    .setValue(personId.getExtension()));
        }
        if (dissent.getPersonName() != null) {
            ext.addExtension("personName", new StringType(dissent.getPersonName()));
        }
        return ext;
    }

    private Reference hsaRoleRef(String hsaId, String displayName, String hsaSystem) {
        Reference ref = new Reference()
                .setType(PRACTITIONER_ROLE_TYPE)
                .setIdentifier(new Identifier().setSystem(hsaSystem).setValue(hsaId));
        if (displayName != null) {
            ref.setDisplay(displayName);
        }
        return ref;
    }
}
