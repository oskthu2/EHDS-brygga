package se.inera.ehds.fml;

import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import se.inera.ehds.mapping.rivta.caredocumentation.AccessControlHeader;
import se.inera.ehds.mapping.rivta.caredocumentation.Author;
import se.inera.ehds.mapping.rivta.caredocumentation.Body;
import se.inera.ehds.mapping.rivta.caredocumentation.CareDocumentation;
import se.inera.ehds.mapping.rivta.caredocumentation.CVType;
import se.inera.ehds.mapping.rivta.caredocumentation.DissentingOpinion;
import se.inera.ehds.mapping.rivta.caredocumentation.Header;
import se.inera.ehds.mapping.rivta.caredocumentation.MultimediaEntry;
import se.inera.ehds.mapping.rivta.caredocumentation.PersonIdType;
import se.inera.ehds.mapping.rivta.caredocumentation.RecordType;
import se.inera.ehds.mapping.rivta.caredocumentation.Signature;
import se.inera.ehds.mapping.tk.RivDateParser;

/**
 * Flattens the subset of GetCareDocumentation fields covered by the FML evaluation PoC into a
 * FHIR Parameters resource - same adapter-layer approach as RivtaDiagnosisParametersAdapter, and
 * the same caveat applies: this is NOT how the production Java mapper reads the RIVTA tree.
 *
 * Explicitly NOT flattened here because the corresponding FML rules were judged to be
 * architecturally impossible to express in FML - see fml-evaluation.md:
 *   - clinicalDocumentNoteText when it is DocBook-XML (the heuristic + DocBookToNarrativeTransformer
 *     call that produces the XHTML narrative) - requires custom Java logic, not expressible in FML.
 *   - the DocBook-to-Composition "Strategy B" section tree (same reason).
 *   - approvedForPatient (PDL-001 is an open question in the Java mapper too - nothing to compare).
 *
 * dissentingOpinion[] is a repeating structure; this Parameters-flattening only carries the
 * FIRST entry (named dissentX below, no index) since a generic repeat-N-times convention was
 * judged out of scope for this PoC - a real translation would need either an indexed naming
 * convention (dissent0OpinionId, dissent1OpinionId, ...) or a proper logical model with real
 * cardinality, not single-valued Parameters entries. This is a scope limitation of the
 * Parameters adapter, not of FML's extension-building rules themselves (which are demonstrated
 * correctly for the single-entry case).
 */
public final class RivtaCareDocumentationParametersAdapter {

    private RivtaCareDocumentationParametersAdapter() {}

    public static Parameters toParameters(CareDocumentation entry) {
        Parameters p = new Parameters();
        if (entry == null) return p;
        Header header = entry.getHeader();
        Body body = entry.getBody();

        if (header != null) {
            if (header.getSourceSystemId() != null) {
                p.addParameter().setName("sourceSystemId").setValue(new StringType(header.getSourceSystemId()));
            }
            RecordType record = header.getRecord();
            if (record != null) {
                if (record.getRecordId() != null) {
                    p.addParameter().setName("recordId").setValue(new StringType(record.getRecordId()));
                }
                if (record.getTimestamp() != null) {
                    String instant = RivDateParser.parseInstant(record.getTimestamp());
                    if (instant != null) {
                        p.addParameter().setName("recordTimestampInstant").setValue(new StringType(instant));
                    }
                }
            }
            AccessControlHeader ach = header.getAccessControlHeader();
            if (ach != null) {
                PersonIdType pid = ach.getPatientId();
                if (pid != null && pid.getExtension() != null) {
                    p.addParameter().setName("patientId").setValue(new StringType(pid.getExtension()));
                }
                if (ach.getCareProcessId() != null) {
                    p.addParameter().setName("careProcessId").setValue(new StringType(ach.getCareProcessId()));
                }
                if (ach.getBlockComparisonTime() != null) {
                    String iso = RivDateParser.parse(ach.getBlockComparisonTime());
                    if (iso != null) {
                        p.addParameter().setName("blockComparisonTime").setValue(new StringType(iso));
                    }
                }
            }
            Author author = header.getAuthor();
            if (author != null && author.getAuthorId() != null) {
                p.addParameter().setName("authorId").setValue(new StringType(author.getAuthorId()));
                if (author.getName() != null) {
                    p.addParameter().setName("authorName").setValue(new StringType(author.getName()));
                }
            }
            Signature signature = header.getSignature();
            if (signature != null && signature.getSignatureId() != null) {
                p.addParameter().setName("signatureId").setValue(new StringType(signature.getSignatureId()));
                if (signature.getName() != null) {
                    p.addParameter().setName("signatureName").setValue(new StringType(signature.getName()));
                }
                if (signature.getTimestamp() != null) {
                    String iso = RivDateParser.parse(signature.getTimestamp());
                    if (iso != null) {
                        p.addParameter().setName("signatureTimestamp").setValue(new StringType(iso));
                    }
                }
            }
        }

        if (body != null) {
            CVType noteCode = body.getClinicalDocumentNoteCode();
            if (noteCode != null) {
                if (noteCode.getCode() != null) {
                    p.addParameter().setName("noteCodeCode").setValue(new StringType(noteCode.getCode()));
                }
                if (noteCode.getCodeSystem() != null) {
                    p.addParameter().setName("noteCodeSystemOid").setValue(new StringType(noteCode.getCodeSystem()));
                }
                if (noteCode.getDisplayName() != null) {
                    p.addParameter().setName("noteCodeDisplay").setValue(new StringType(noteCode.getDisplayName()));
                }
                String codeText = noteCode.getOriginalText() != null ? noteCode.getOriginalText() : noteCode.getDisplayName();
                if (codeText != null) {
                    p.addParameter().setName("noteCodeText").setValue(new StringType(codeText));
                }
            }
            if (body.getClinicalDocumentNoteTitle() != null) {
                p.addParameter().setName("noteTitle").setValue(new StringType(body.getClinicalDocumentNoteTitle()));
            }
            // Fritext utan DocBook-struktur ("ser inte ut som XML") - DocBook-grenen (looksLikeDocBook
            // + DocBookToNarrativeTransformer) är explicit INTE med här, se klasskommentaren.
            if (body.getClinicalDocumentNoteText() != null
                    && !body.getClinicalDocumentNoteText().stripLeading().startsWith("<")) {
                // Base64-kodad redan här - se klasskommentaren om attachment.data i
                // get-caredocumentation-to-documentreference.map.
                String base64 = Base64.getEncoder().encodeToString(
                        body.getClinicalDocumentNoteText().getBytes(StandardCharsets.UTF_8));
                p.addParameter().setName("noteTextPlainBase64").setValue(new StringType(base64));
            }

            MultimediaEntry media = body.getMultimediaEntry();
            if (media != null) {
                if (media.getMediaType() != null) {
                    p.addParameter().setName("multimediaType").setValue(new StringType(media.getMediaType()));
                }
                // value/reference är en XOR i källan (samma invariant som Java-mappern litar på) -
                // media.getValue() är redan base64-kodad RIVTA-text, precis som
                // Attachment.setData(Base64.getDecoder().decode(media.getValue())) i Java förutsätter.
                if (media.getValue() != null) {
                    p.addParameter().setName("multimediaValueBase64").setValue(new StringType(media.getValue()));
                } else if (media.getReference() != null) {
                    p.addParameter().setName("multimediaReference").setValue(new StringType(media.getReference()));
                }
            }

            if (!body.getDissentingOpinion().isEmpty()) {
                DissentingOpinion dissent = body.getDissentingOpinion().get(0);
                if (dissent.getOpinionId() != null) {
                    p.addParameter().setName("dissentOpinionId").setValue(new StringType(dissent.getOpinionId()));
                }
                if (dissent.getAuthorTime() != null) {
                    String iso = RivDateParser.parse(dissent.getAuthorTime());
                    if (iso != null) {
                        p.addParameter().setName("dissentAuthorTime").setValue(new StringType(iso));
                    }
                }
                if (dissent.getOpinion() != null) {
                    p.addParameter().setName("dissentOpinion").setValue(new StringType(dissent.getOpinion()));
                }
                if (dissent.getPersonId() != null && dissent.getPersonId().getExtension() != null) {
                    p.addParameter().setName("dissentPersonId")
                            .setValue(new StringType(dissent.getPersonId().getExtension()));
                }
                if (dissent.getPersonName() != null) {
                    p.addParameter().setName("dissentPersonName").setValue(new StringType(dissent.getPersonName()));
                }
            }
        }
        return p;
    }
}
