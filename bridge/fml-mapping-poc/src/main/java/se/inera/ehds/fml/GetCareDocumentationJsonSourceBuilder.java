package se.inera.ehds.fml;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import se.inera.ehds.mapping.naming.NamingSystemRegistry;
import se.inera.ehds.mapping.rivta.caredocumentation.AccessControlHeader;
import se.inera.ehds.mapping.rivta.caredocumentation.Author;
import se.inera.ehds.mapping.rivta.caredocumentation.Body;
import se.inera.ehds.mapping.rivta.caredocumentation.CVType;
import se.inera.ehds.mapping.rivta.caredocumentation.CareDocumentation;
import se.inera.ehds.mapping.rivta.caredocumentation.DissentingOpinion;
import se.inera.ehds.mapping.rivta.caredocumentation.Header;
import se.inera.ehds.mapping.rivta.caredocumentation.MultimediaEntry;
import se.inera.ehds.mapping.rivta.caredocumentation.PersonIdType;
import se.inera.ehds.mapping.rivta.caredocumentation.RecordType;
import se.inera.ehds.mapping.rivta.caredocumentation.Signature;
import se.inera.ehds.mapping.tk.RivDateParser;

/**
 * Builds one JSON instance of the EHDS-TK logical model
 * {@code inera-ehds-lm-care-documentation} (one {@code careDocumentation[]} entry) per RIVTA
 * {@link CareDocumentation} record — the GetCareDocumentation counterpart of
 * {@link GetDiagnosisJsonSourceBuilder}; see its javadoc for the general approach.
 *
 * DocBook-formatted {@code clinicalDocumentNoteText} is deliberately NOT carried into the
 * JSON (same as the old Parameters adapter) — DocBookToNarrativeTransformer and the Strategy B
 * Composition both require real Java code and have no FML equivalent; see fml-evaluation.md.
 * {@code dissentingOpinion} only carries its first entry, per the logical model's own
 * single-cardinality design choice for this PoC's coverage (documented limitation, unchanged
 * from the Parameters-based version).
 */
public final class GetCareDocumentationJsonSourceBuilder {

    private final NamingSystemRegistry namingSystem;

    public GetCareDocumentationJsonSourceBuilder(NamingSystemRegistry namingSystem) {
        this.namingSystem = namingSystem;
    }

    public String toJson(CareDocumentation entry) {
        JsonObject root = new JsonObject();
        JsonArray entries = new JsonArray();
        entries.add(buildEntry(entry));
        root.add("careDocumentation", entries);
        return root.toString();
    }

    private JsonObject buildEntry(CareDocumentation entry) {
        JsonObject e = new JsonObject();
        if (entry == null) return e;
        Header header = entry.getHeader();
        Body body = entry.getBody();
        if (header != null) e.add("header", buildHeader(header));
        if (body != null) e.add("body", buildBody(body));
        return e;
    }

    private JsonObject buildHeader(Header header) {
        JsonObject h = new JsonObject();
        AccessControlHeader ach = header.getAccessControlHeader();
        if (ach != null) h.add("accessControlHeader", buildAccessControlHeader(ach));
        if (header.getSourceSystemId() != null && header.getSourceSystemId().getExtension() != null) {
            h.add("sourceSystemId", identifier(null, header.getSourceSystemId().getExtension()));
        }
        RecordType record = header.getRecord();
        if (record != null) h.add("record", buildRecord(record));
        Author author = header.getAuthor();
        if (author != null) h.add("author", buildAuthor(author));
        Signature signature = header.getSignature();
        if (signature != null) h.add("signature", buildSignature(signature));
        return h;
    }

    private JsonObject buildAccessControlHeader(AccessControlHeader ach) {
        JsonObject a = new JsonObject();
        addPersonId(a, "accountableHealthcareProvider", ach.getAccountableHealthcareProvider());
        addPersonId(a, "accountableCareUnit", ach.getAccountableCareUnit());
        addPersonId(a, "patientId", ach.getPatientId());
        addPersonId(a, "careProcessId", ach.getCareProcessId());
        putDateTime(a, "blockComparisonTime", ach.getBlockComparisonTime());
        return a;
    }

    private JsonObject buildRecord(RecordType record) {
        JsonObject r = new JsonObject();
        addPersonId(r, "recordId", record.getId());
        putInstant(r, "timestamp", record.getTimestamp());
        return r;
    }

    private JsonObject buildAuthor(Author author) {
        JsonObject a = new JsonObject();
        if (author.getId() != null && author.getId().getExtension() != null) {
            a.add("authorId", identifier(null, author.getId().getExtension()));
        }
        putString(a, "name", author.getName());
        putInstant(a, "timestamp", author.getTimestamp());
        return a;
    }

    private JsonObject buildSignature(Signature signature) {
        JsonObject s = new JsonObject();
        if (signature.getId() != null && signature.getId().getExtension() != null) {
            s.add("signatureId", identifier(null, signature.getId().getExtension()));
        }
        putString(s, "name", signature.getName());
        putDateTime(s, "timestamp", signature.getTimestamp());
        return s;
    }

    private JsonObject buildBody(Body body) {
        JsonObject b = new JsonObject();
        CVType noteCode = body.getClinicalDocumentNoteCode();
        if (noteCode != null) {
            String text = noteCode.getOriginalText();
            String system = noteCode.getCodeSystem() != null ? namingSystem.oidToUri(noteCode.getCodeSystem()) : null;
            b.add("clinicalDocumentNoteCode",
                    codeableConcept(system, noteCode.getCode(), noteCode.getDisplayName(), text));
        }
        putString(b, "clinicalDocumentNoteTitle", body.getClinicalDocumentNoteTitle());

        String noteText = body.getClinicalDocumentNoteText();
        if (noteText != null && !looksLikeDocBook(noteText)) {
            // Base64-kodat redan här: FML:s base64Binary-primitiv förväntar sig att källvärdet
            // RÅKAR vara en base64-sträng (motorn kan inte base64-koda en godtycklig sträng
            // själv) - samma adapterlagerkompromiss som den tidigare Parameters-baserade
            // versionen gjorde (noteTextPlainBase64). Modellens fält är fortfarande "vanlig
            // text" semantiskt; det är bara den här JSON-representationen som förkodar den för
            // att kunna tilldelas Attachment.data direkt.
            b.addProperty("clinicalDocumentNoteText",
                    java.util.Base64.getEncoder().encodeToString(noteText.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        }

        MultimediaEntry media = body.getMultimediaEntry();
        if (media != null) {
            JsonObject m = new JsonObject();
            putString(m, "mediaType", media.getMediaType());
            putString(m, "value", media.getValue());
            putString(m, "reference", media.getReference());
            b.add("multimediaEntry", m);
        }

        if (!body.getDissentingOpinion().isEmpty()) {
            DissentingOpinion first = body.getDissentingOpinion().get(0);
            if (first != null) {
                JsonArray dissenting = new JsonArray();
                dissenting.add(buildDissentingOpinion(first));
                b.add("dissentingOpinion", dissenting);
            }
        }
        return b;
    }

    private JsonObject buildDissentingOpinion(DissentingOpinion dissent) {
        JsonObject d = new JsonObject();
        if (dissent.getOpinionId() != null && dissent.getOpinionId().getExtension() != null) {
            d.add("opinionId", identifier(null, dissent.getOpinionId().getExtension()));
        }
        putDateTime(d, "authorTime", dissent.getAuthorTime());
        putString(d, "opinion", dissent.getOpinion());
        addPersonId(d, "personId", dissent.getPersonId());
        putString(d, "personName", dissent.getPersonName());
        return d;
    }

    // DocBook-detektion identisk med GetCareDocumentationMapper.looksLikeDocBook().
    private boolean looksLikeDocBook(String text) {
        return text.stripLeading().startsWith("<");
    }

    private void addPersonId(JsonObject obj, String key, PersonIdType pid) {
        if (pid == null) return;
        String system = pid.getRoot() != null ? namingSystem.oidToUri(pid.getRoot()) : null;
        obj.add(key, identifier(system, pid.getExtension()));
    }

    private JsonObject codeableConcept(String codeSystemOid, String code, String display, String text) {
        JsonObject cc = new JsonObject();
        JsonArray codings = new JsonArray();
        JsonObject coding = new JsonObject();
        if (codeSystemOid != null) coding.addProperty("system", codeSystemOid);
        if (code != null) coding.addProperty("code", code);
        if (display != null) coding.addProperty("display", display);
        codings.add(coding);
        cc.add("coding", codings);
        if (text != null) cc.addProperty("text", text);
        return cc;
    }

    private JsonObject identifier(String system, String value) {
        JsonObject id = new JsonObject();
        if (system != null) id.addProperty("system", system);
        if (value != null) id.addProperty("value", value);
        return id;
    }

    private void putString(JsonObject obj, String key, String value) {
        if (value != null) obj.addProperty(key, value);
    }

    private void putDateTime(JsonObject obj, String key, String rivtaDate) {
        if (rivtaDate == null) return;
        String iso = RivDateParser.parse(rivtaDate);
        if (iso != null) obj.addProperty(key, iso);
    }

    private void putInstant(JsonObject obj, String key, String rivtaDate) {
        if (rivtaDate == null) return;
        String iso = RivDateParser.parseInstant(rivtaDate);
        if (iso != null) obj.addProperty(key, iso);
    }
}
