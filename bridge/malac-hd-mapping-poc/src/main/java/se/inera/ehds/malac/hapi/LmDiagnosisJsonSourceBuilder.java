package se.inera.ehds.malac.hapi;

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
 * (one {@code diagnosis[]} entry) per RIVTA {@link Diagnosis} record - the Java mirror of
 * {@code lm_diagnosis_json_builder.py} (this module's {@code python/} tree), which builds the
 * SAME shape for MaLaC-HD. {@link HapiFmlEngine} parses the result into a real HAPI
 * {@code Element} source via {@code org.hl7.fhir.r4.elementmodel.JsonParser}, exactly like
 * {@code claude/fml-mapping-9ovqyh}'s {@code GetDiagnosisJsonSourceBuilder} (prior art - this
 * class is a from-scratch reimplementation in this module, not a dependency on that branch,
 * see malac-hd-evaluation.md).
 *
 * <p>Unlike that branch's version, this builder ALSO pre-resolves {@code diagnosisCode}'s OID
 * (not only {@code patientId}'s) and pre-builds the full tjanstekatalog Endpoint URL for
 * {@code sourceSystemHSAId.value} - see {@code get-diagnosis-to-condition.map}'s header
 * comment for why: the two engines must compile the IDENTICAL {@code .map} file, and a
 * confirmed MaLaC-HD 1.10.0 bug makes FML's {@code &} operator unusable for a model-sourced
 * fallback on that engine, so both JSON builders do the full OID->URI resolution (mapped +
 * {@code urn:oid:} fallback) up front now, not just this one.
 */
public final class LmDiagnosisJsonSourceBuilder {

    private final NamingSystemRegistry namingSystem;

    public LmDiagnosisJsonSourceBuilder(NamingSystemRegistry namingSystem) {
        this.namingSystem = namingSystem;
    }

    public String toJson(Diagnosis diag) {
        return toJson(diag, false);
    }

    /**
     * {@code recordedAsInstant}: Provenance.recorded needs the true UTC instant
     * (RivDateParser.parseInstant), while Condition.recordedDate needs the local-offset
     * dateTime (RivDateParser.parse) from the SAME raw accountableHealthcareProfessional.
     * authorTime field - see ProvenanceBuilder.build() vs. GetDiagnosisMapper.java.
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
            h.add("sourceSystemHSAId", identifier(
                    HapiFmlEngine.TJANSTEKATALOG_BASE + "/Endpoint/" + header.getSourceSystemHSAId(), null));
        }
        PersonIdType patientId = header.getPatientId();
        if (patientId != null) {
            h.add("patientId", identifier(patientId.getId(), namingSystem.oidToUri(patientId.getType())));
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
            a.add("healthcareProfessionalHSAId", identifier(ahp.getHealthcareProfessionalHSAId(), null));
        }
        putString(a, "healthcareProfessionalName", ahp.getHealthcareProfessionalName());
        if (ahp.getHealthcareProfessionalCareUnitHSAId() != null) {
            a.add("healthcareProfessionalCareUnitHSAId", identifier(ahp.getHealthcareProfessionalCareUnitHSAId(), null));
        }
        if (ahp.getHealthcareProfessionalCareGiverHSAId() != null) {
            a.add("healthcareProfessionalCareGiverHSAId", identifier(ahp.getHealthcareProfessionalCareGiverHSAId(), null));
        }
        return a;
    }

    private JsonObject buildLegalAuthenticator(LegalAuthenticatorType la) {
        JsonObject l = new JsonObject();
        putDateTime(l, "signatureTime", la.getSignatureTime());
        if (la.getLegalAuthenticatorHSAId() != null) {
            l.add("legalAuthenticatorHSAId", identifier(la.getLegalAuthenticatorHSAId(), null));
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
            String system = dc.getCodeSystem() != null ? namingSystem.oidToUri(dc.getCodeSystem()) : null;
            b.add("diagnosisCode", codeableConcept(system, dc.getCode(), dc.getDisplayName(), dc.getOriginalText()));
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

    /** CodeableConcept-shaped ({@code coding: [{system, code, display}], text}). {@code
     * system} is already a resolved URI (or {@code urn:oid:} fallback) by the time it reaches
     * here - see the class javadoc. */
    private JsonObject codeableConcept(String system, String code, String display, String text) {
        JsonObject cc = new JsonObject();
        JsonArray codings = new JsonArray();
        JsonObject coding = new JsonObject();
        if (system != null) coding.addProperty("system", system);
        if (code != null) coding.addProperty("code", code);
        if (display != null) coding.addProperty("display", display);
        codings.add(coding);
        cc.add("coding", codings);
        if (text != null) cc.addProperty("text", text);
        return cc;
    }

    /** Identifier-shaped ({@code {value, system}}) - matches
     * python/lm_diagnosis_json_builder.py's {@code _identifier(value, system)} parameter
     * order. */
    private JsonObject identifier(String value, String system) {
        JsonObject id = new JsonObject();
        if (value != null) id.addProperty("value", value);
        if (system != null) id.addProperty("system", system);
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
