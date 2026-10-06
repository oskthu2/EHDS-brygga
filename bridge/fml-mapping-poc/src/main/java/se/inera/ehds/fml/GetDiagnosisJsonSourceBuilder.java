package se.inera.ehds.fml;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import se.inera.ehds.mapping.naming.NamingSystemRegistry;
import se.inera.ehds.mapping.rivta.CVType;
import se.inera.ehds.mapping.rivta.Diagnosis;
import se.inera.ehds.mapping.rivta.DiagnosisBody;
import se.inera.ehds.mapping.rivta.DiagnosisHeader;
import se.inera.ehds.mapping.rivta.HealthcareProfessionalType;
import se.inera.ehds.mapping.rivta.LegalAuthenticatorType;
import se.inera.ehds.mapping.rivta.PersonIdType;
import se.inera.ehds.mapping.rivta.RelatedDiagnosis;
import se.inera.ehds.mapping.tk.RivDateParser;

/**
 * Builds one JSON instance of the EHDS-TK logical model {@code inera-ehds-lm-diagnosis}
 * (one {@code diagnosis[]} entry) per RIVTA {@link Diagnosis} record, which {@link FmlEngine}
 * then parses into a real {@code Element} source via
 * {@code org.hl7.fhir.r4.elementmodel.JsonParser} — no flat Parameters stand-in, see
 * fml-evaluation.md.
 *
 * Field names mirror the logical model's own nested shape 1:1 (it was generated from the same
 * TKB as the production RIVTA classes), so this is a thin tree-to-tree copy. The only real
 * work is what FML itself has no hook for: OID -> URI resolution (NamingSystemRegistry) and
 * RIVTA date-string -> ISO-8601 conversion (RivDateParser) — the same two conversions the
 * production Java mapper and the old Parameters adapter both needed. Everything else (ConceptMap
 * translation, boolean/date target casting, the stop-the-resource policy) happens in the .map
 * file or in FmlEngine's registry, not here.
 */
public final class GetDiagnosisJsonSourceBuilder {

    private static final String HSA_OID_INERA = "1.2.752.129.2.1.4.1";

    private final NamingSystemRegistry namingSystem;

    public GetDiagnosisJsonSourceBuilder(NamingSystemRegistry namingSystem) {
        this.namingSystem = namingSystem;
    }

    public String toJson(Diagnosis diag) {
        return toJson(diag, false);
    }

    /**
     * {@code recordedAsInstant}: Provenance.recorded needs the true UTC instant
     * (RivDateParser.parseInstant), while Condition.recordedDate needs the local-offset
     * dateTime (RivDateParser.parse) from the SAME raw accountableHealthcareProfessional.
     * authorTime field - see ProvenanceBuilder.build() vs. GetDiagnosisMapper.java. FML has no
     * date-reformatting function of its own, so the two target maps each get their own JSON
     * built with the conversion they need, rather than trying to carry both forms in one
     * logical-model instance.
     */
    public String toJson(Diagnosis diag, boolean recordedAsInstant) {
        JsonObject root = new JsonObject();
        JsonArray diagnosisArray = new JsonArray();
        diagnosisArray.add(buildDiagnosis(diag, recordedAsInstant));
        root.add("diagnosis", diagnosisArray);
        return root.toString();
    }

    private JsonObject buildDiagnosis(Diagnosis diag, boolean recordedAsInstant) {
        JsonObject d = new JsonObject();
        if (diag == null) return d;
        DiagnosisHeader header = diag.getDiagnosisHeader();
        DiagnosisBody body = diag.getDiagnosisBody();
        if (header != null) d.add("diagnosisHeader", buildHeader(header, recordedAsInstant));
        if (body != null) d.add("diagnosisBody", buildBody(body));
        return d;
    }

    private JsonObject buildHeader(DiagnosisHeader header, boolean recordedAsInstant) {
        JsonObject h = new JsonObject();
        putString(h, "documentId", header.getDocumentId());
        if (header.getSourceSystemHSAId() != null) {
            h.add("sourceSystemHSAId", identifier(null, header.getSourceSystemHSAId()));
        }
        PersonIdType patientId = header.getPatientId();
        if (patientId != null) {
            h.add("patientId", identifier(namingSystem.oidToUri(patientId.getType()), patientId.getId()));
        }
        HealthcareProfessionalType ahp = header.getAccountableHealthcareProfessional();
        if (ahp != null) h.add("accountableHealthcareProfessional", buildAhp(ahp, recordedAsInstant));
        LegalAuthenticatorType la = header.getLegalAuthenticator();
        if (la != null) h.add("legalAuthenticator", buildLegalAuthenticator(la));
        return h;
    }

    private JsonObject buildAhp(HealthcareProfessionalType ahp, boolean recordedAsInstant) {
        JsonObject a = new JsonObject();
        if (recordedAsInstant) {
            putInstant(a, "authorTime", ahp.getAuthorTime());
        } else {
            putDateTime(a, "authorTime", ahp.getAuthorTime());
        }
        if (ahp.getHealthcareProfessionalHSAId() != null) {
            a.add("healthcareProfessionalHSAId", identifier(null, ahp.getHealthcareProfessionalHSAId()));
        }
        putString(a, "healthcareProfessionalName", ahp.getHealthcareProfessionalName());
        if (ahp.getHealthcareProfessionalCareUnitHSAId() != null) {
            a.add("healthcareProfessionalCareUnitHSAId", identifier(null, ahp.getHealthcareProfessionalCareUnitHSAId()));
        }
        if (ahp.getHealthcareProfessionalCareGiverHSAId() != null) {
            a.add("healthcareProfessionalCareGiverHSAId", identifier(null, ahp.getHealthcareProfessionalCareGiverHSAId()));
        }
        return a;
    }

    private JsonObject buildLegalAuthenticator(LegalAuthenticatorType la) {
        JsonObject l = new JsonObject();
        putDateTime(l, "signatureTime", la.getSignatureTime());
        if (la.getLegalAuthenticatorHSAId() != null) {
            l.add("legalAuthenticatorHSAId", identifier(null, la.getLegalAuthenticatorHSAId()));
        }
        putString(l, "legalAuthenticatorName", la.getLegalAuthenticatorName());
        return l;
    }

    private JsonObject buildBody(DiagnosisBody body) {
        JsonObject b = new JsonObject();
        if (body.getTypeOfDiagnosis() != null) {
            b.add("typeOfDiagnosis", codeableConcept(null, body.getTypeOfDiagnosis(), null, null));
        }
        if (body.getChronicDiagnosis() != null) {
            b.addProperty("chronicDiagnosis", body.getChronicDiagnosis());
        }
        putDateTime(b, "diagnosisTime", body.getDiagnosisTime());
        CVType dc = body.getDiagnosisCode();
        if (dc != null) {
            String text = dc.getOriginalText() != null ? dc.getOriginalText() : null;
            b.add("diagnosisCode", codeableConcept(dc.getCodeSystem(), dc.getCode(), dc.getDisplayName(), text));
        }
        if (!body.getRelatedDiagnosis().isEmpty()) {
            JsonArray related = new JsonArray();
            for (RelatedDiagnosis rd : body.getRelatedDiagnosis()) {
                if (rd != null && rd.getDocumentId() != null) {
                    JsonObject r = new JsonObject();
                    r.addProperty("documentId", rd.getDocumentId());
                    related.add(r);
                }
            }
            if (related.size() > 0) b.add("relatedDiagnosis", related);
        }
        return b;
    }

    /** {@code codeSystemOid} is the RIVTA OID when there is one (e.g. diagnosisCode); null for
     * the bare-enum typeOfDiagnosis field, which carries no OID of its own. It is carried
     * un-resolved in Coding.system (a real OID string, not yet a URI) deliberately — the .map
     * resolves it via the same codesystem-oid ConceptMap/translate() pattern used before, so
     * that part of the evaluation stays demonstrated. */
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

    public static String hsaOidInera() {
        return HSA_OID_INERA;
    }
}
