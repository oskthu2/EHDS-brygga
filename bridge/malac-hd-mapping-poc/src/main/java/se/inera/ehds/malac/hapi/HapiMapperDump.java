package se.inera.ehds.malac.hapi;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Provenance;
import se.inera.ehds.mapping.naming.NamingSystemRegistry;
import se.inera.ehds.mapping.rivta.Diagnosis;
import se.inera.ehds.mapping.rivta.DiagnosisHeader;
import se.inera.ehds.mapping.rivta.GetDiagnosisResponse;
import se.inera.ehds.mapping.rivta.HealthcareProfessionalType;
import se.inera.ehds.mapping.tk.MappedDiagnosisEntry;
import se.inera.ehds.mapping.tk.RivDateParser;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Comparison-harness driver: runs the SAME {@code .map} files MaLaC-HD compiles (see
 * {@link HapiFmlEngine}) over the SAME two fixtures {@link JavaMapperDump} already builds
 * (huvuddiagnosFixture/ogiltigtPersonnummerFixture - sharing them guarantees all three tracks
 * compare literally identical input), producing the SAME
 * {@code {"condition": ..., "provenance": ...}} shape {@code run_pipeline.py}/{@link
 * JavaMapperDump#mapToJson} do, so {@code compare/run_comparison.py} can diff all three
 * pairwise. Mirrors {@code run_pipeline.py}'s orchestration (DIAG-001 guard before either
 * transform, Provenance.target/.recorded filled in afterwards - a real StructureMap
 * transform() only ever has one target, see get-diagnosis-to-provenance.map's header
 * comment - plus Condition.onset[x]/ext-asserted-date and the three "uri" fields moved out of
 * FML for the same reasons, see that file's header comment and {@link #setUriFields}).
 */
public final class HapiMapperDump {

    private static final Pattern PERSONNUMMER = Pattern.compile("^\\d{12}$");

    private HapiMapperDump() {}

    public static List<MappedDiagnosisEntry> map(GetDiagnosisResponse response, HapiFmlEngine engine) throws Exception {
        List<MappedDiagnosisEntry> entries = new ArrayList<>();
        if (response.getResult() == null
                || !"OK".equals(response.getResult().getResultCode())) {
            return entries;
        }
        NamingSystemRegistry namingSystem = new NamingSystemRegistry();
        LmDiagnosisJsonSourceBuilder sourceBuilder = new LmDiagnosisJsonSourceBuilder(namingSystem);

        for (Diagnosis diag : response.getDiagnosis()) {
            if (!passesGuard(diag)) continue;

            // Condition pass: authorTime -> offset dateTime (recordedDate).
            String condSourceJson = sourceBuilder.toJson(diag, false);
            Base condSource = engine.parseSource(condSourceJson);
            if (!engine.passesDiag001Guard(condSource)) continue;
            Base conditionBase = engine.transformToCondition(condSource);
            Condition condition = (Condition) conditionBase;
            String conditionId = UUID.randomUUID().toString();
            condition.setId(conditionId);
            setOnsetAndAssertedDate(condition, diag);
            setUriFields(condition, condSourceJson);

            // Provenance pass: authorTime -> TRUE UTC instant (Provenance.recorded).
            Base provSource = engine.parseSource(sourceBuilder.toJson(diag, true));
            Base provenanceBase = engine.transformToProvenance(provSource);
            Provenance provenance = (Provenance) provenanceBase;
            provenance.setId(UUID.randomUUID().toString());
            provenance.addTarget(new org.hl7.fhir.r4.model.Reference("urn:uuid:" + conditionId));
            String rawAuthorTime = authorTimeOf(diag);
            if (rawAuthorTime != null) {
                String instant = RivDateParser.parseInstant(rawAuthorTime);
                if (instant != null) {
                    provenance.setRecordedElement(new org.hl7.fhir.r4.model.InstantType(instant));
                }
            }

            entries.add(new MappedDiagnosisEntry(condition, provenance));
        }
        return entries;
    }

    /** Mirrors guard.py's DIAG-001 check: diagnosisHeader/diagnosisBody present, patientId a
     * 12-digit personnummer/samordningsnummer. Done in Java first (same as the Python driver
     * does before ever building a source JSON) - HapiFmlEngine.passesDiag001Guard() is a
     * SECOND, FHIRPath-based check against the parsed source, kept for parity with how
     * claude/fml-mapping-9ovqyh's FmlEngine enforces this same policy. */
    private static boolean passesGuard(Diagnosis diag) {
        if (diag.getDiagnosisHeader() == null || diag.getDiagnosisBody() == null) return false;
        DiagnosisHeader header = diag.getDiagnosisHeader();
        if (header.getPatientId() == null || header.getPatientId().getId() == null) return false;
        return PERSONNUMMER.matcher(header.getPatientId().getId()).matches();
    }

    private static String authorTimeOf(Diagnosis diag) {
        HealthcareProfessionalType ahp = diag.getDiagnosisHeader() != null
                ? diag.getDiagnosisHeader().getAccountableHealthcareProfessional() : null;
        return ahp != null ? ahp.getAuthorTime() : null;
    }

    private static final String ASSERTED_DATE_EXTENSION_URL =
            "https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-asserted-date";

    /** Sets Condition.onsetDateTime and the ext-asserted-date extension's valueDateTime
     * directly on the transform() RESULT, exactly like Provenance.target/.recorded are set
     * outside FML - see get-diagnosis-to-condition.map's header comment for why NEITHER
     * engine can be asked to set a dateTime-typed CHOICE element sourced from a custom model:
     * a confirmed, irreconcilable conflict between MaLaC-HD and HAPI, found by actually
     * testing every combination against both. Mirrors run_pipeline.py's
     * _set_onset_and_asserted_date(). */
    private static void setOnsetAndAssertedDate(Condition condition, Diagnosis diag) {
        String rawDiagnosisTime = diag.getDiagnosisBody() != null ? diag.getDiagnosisBody().getDiagnosisTime() : null;
        if (rawDiagnosisTime != null) {
            String iso = RivDateParser.parse(rawDiagnosisTime);
            if (iso != null) condition.setOnset(new org.hl7.fhir.r4.model.DateTimeType(iso));
        }
        String rawSignatureTime = diag.getDiagnosisHeader() != null && diag.getDiagnosisHeader().getLegalAuthenticator() != null
                ? diag.getDiagnosisHeader().getLegalAuthenticator().getSignatureTime() : null;
        if (rawSignatureTime != null) {
            String iso = RivDateParser.parse(rawSignatureTime);
            if (iso != null) {
                for (org.hl7.fhir.r4.model.Extension ext : condition.getExtension()) {
                    if (ASSERTED_DATE_EXTENSION_URL.equals(ext.getUrl())) {
                        ext.setValue(new org.hl7.fhir.r4.model.DateTimeType(iso));
                    }
                }
            }
        }
    }

    /** Sets Condition.meta.source, Condition.subject.identifier.system and
     * Condition.code.coding[0].system directly on the transform() RESULT, reading the
     * already-resolved values straight back out of condSourceJson (the SAME JSON string the
     * transform just consumed - no re-derivation needed). Mirrors run_pipeline.py's
     * _set_uri_fields(): see get-diagnosis-to-condition.map's header comment for why - a
     * confirmed HAPI-only bug corrupts these three "uri" fields when FML sets them via
     * create('uri') from a navigated (non-literal) source Element, so they moved out of FML
     * on both sides, exactly like onset/assertedDate above. */
    private static void setUriFields(Condition condition, String condSourceJson) {
        JsonObject diag = JsonParser.parseString(condSourceJson)
                .getAsJsonObject().getAsJsonArray("diagnosis").get(0).getAsJsonObject();
        JsonObject header = diag.has("diagnosisHeader") ? diag.getAsJsonObject("diagnosisHeader") : new JsonObject();
        JsonObject body = diag.has("diagnosisBody") ? diag.getAsJsonObject("diagnosisBody") : new JsonObject();

        if (header.has("sourceSystemHSAId") && condition.hasMeta()) {
            JsonObject sourceHsa = header.getAsJsonObject("sourceSystemHSAId");
            if (sourceHsa.has("value")) {
                condition.getMeta().setSource(sourceHsa.get("value").getAsString());
            }
        }
        if (header.has("patientId") && condition.hasSubject() && condition.getSubject().hasIdentifier()) {
            JsonObject patientId = header.getAsJsonObject("patientId");
            if (patientId.has("system")) {
                condition.getSubject().getIdentifier().setSystem(patientId.get("system").getAsString());
            }
        }
        if (body.has("diagnosisCode") && condition.hasCode() && !condition.getCode().getCoding().isEmpty()) {
            JsonObject diagnosisCode = body.getAsJsonObject("diagnosisCode");
            if (diagnosisCode.has("coding") && diagnosisCode.getAsJsonArray("coding").size() > 0) {
                JsonObject coding = diagnosisCode.getAsJsonArray("coding").get(0).getAsJsonObject();
                if (coding.has("system")) {
                    condition.getCode().getCodingFirstRep().setSystem(coding.get("system").getAsString());
                }
            }
        }
    }

    /** Same shape as JavaMapperDump.mapToJson / run_pipeline.py's output - a JSON array of
     * {"condition": ..., "provenance": ...} objects, one per mapped entry. */
    public static String mapToJson(GetDiagnosisResponse response, HapiFmlEngine engine) throws Exception {
        FhirContext ctx = FhirContext.forR4();
        IParser parser = ctx.newJsonParser().setPrettyPrint(true);
        List<MappedDiagnosisEntry> entries = map(response, engine);

        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < entries.size(); i++) {
            MappedDiagnosisEntry entry = entries.get(i);
            sb.append("  {\n");
            sb.append("    \"condition\": ").append(indent(parser.encodeResourceToString(entry.condition()), 4)).append(",\n");
            sb.append("    \"provenance\": ").append(indent(parser.encodeResourceToString(entry.provenance()), 4)).append("\n");
            sb.append("  }");
            if (i < entries.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]\n");
        return sb.toString();
    }

    private static String indent(String json, int spaces) {
        String pad = " ".repeat(spaces);
        return json.replace("\n", "\n" + pad);
    }
}
