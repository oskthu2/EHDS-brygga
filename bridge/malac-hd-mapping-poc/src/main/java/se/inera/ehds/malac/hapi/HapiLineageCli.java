package se.inera.ehds.malac.hapi;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.Provenance;
import org.hl7.fhir.r4.model.Reference;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Thin CLI entry point for the viewer (bridge/malac-hd-mapping-poc/viewer/) added in this same
 * PR - NOT part of the comparison-harness test suite ({@link HapiMapperDumpTest} already
 * covers that). Unlike {@link HapiMapperDump}, which starts from a {@code GetDiagnosisResponse}
 * POJO (built by hand in {@link se.inera.ehds.malac.JavaMapperDump} for exactly two fixtures),
 * this CLI starts from the SAME lm-diagnosis-shaped JSON the Python/MaLaC-HD track's
 * run_pipeline.py builds and feeds to its compiled transforms (see
 * ../../python/lm_diagnosis_json_builder.py) - the viewer's Python backend builds that JSON
 * once (for an arbitrary RIVTA fixture, not just the two hand-built ones) and hands it to BOTH
 * engines, so this class only needs to run {@link HapiFmlEngine} over it, with no RIVTA XML
 * parsing of its own (this module's RIVTA model has none - see JavaMapperDump's class
 * Javadoc).
 *
 * <p>Takes two already-built lm-diagnosis JSON documents - one per
 * {@code accountableHealthcareProfessional.authorTime} conversion the two transforms each need
 * (see get-diagnosis-to-provenance.map's header comment) - and prints a single
 * {@code {"condition": ..., "provenance": ...}} object, reading every driver-set field (onset,
 * ext-asserted-date's value, the three "uri" fields, Provenance.recorded) straight back out of
 * the SAME already-resolved JSON it was given, exactly like {@link HapiMapperDump#setUriFields}
 * does - no raw-RIVTA-string re-parsing needed here, since the viewer backend already produced
 * fully-resolved values.
 *
 * <p>Usage: {@code HapiLineageCli <condition-source.json> <provenance-source.json>}
 */
public final class HapiLineageCli {

    private static final String ASSERTED_DATE_EXTENSION_URL =
            "https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-asserted-date";

    private HapiLineageCli() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("usage: HapiLineageCli <condition-source.json> <provenance-source.json>");
            System.exit(1);
        }
        String condJsonText = Files.readString(Path.of(args[0]), StandardCharsets.UTF_8);
        String provJsonText = Files.readString(Path.of(args[1]), StandardCharsets.UTF_8);

        // HAPI's StructureMapUtilities writes a verbose rule-by-rule debug trace straight to
        // System.out (no logger involved, confirmed by inspecting the output - not something
        // this class controls), both while parsing the .map files and while transform()
        // executes. The viewer backend needs CLEAN JSON on stdout, so stdout is silenced for
        // every HapiFmlEngine call and restored only for this class's own final print.
        PrintStream realOut = System.out;
        System.setOut(new PrintStream(OutputStream.nullOutputStream(), false, StandardCharsets.UTF_8));
        String result;
        try {
            Path moduleRoot = Path.of(".").toAbsolutePath().normalize();
            HapiFmlEngine engine = new HapiFmlEngine(moduleRoot);

            Base condSource = engine.parseSource(condJsonText);
            if (!engine.passesDiag001Guard(condSource)) {
                // Mirrors HapiMapperDump.map()/run_pipeline.py: a record that fails DIAG-001
                // produces no output at all - the viewer backend already checks this with
                // guard.py before calling us, this is a second, defence-in-depth check only.
                result = "{}\n";
            } else {
                Condition condition = (Condition) engine.transformToCondition(condSource);
                String conditionId = UUID.randomUUID().toString();
                condition.setId(conditionId);
                setOnsetAndAssertedDate(condition, condJsonText);
                setUriFields(condition, condJsonText);

                Base provSource = engine.parseSource(provJsonText);
                Provenance provenance = (Provenance) engine.transformToProvenance(provSource);
                provenance.setId(UUID.randomUUID().toString());
                provenance.addTarget(new Reference("urn:uuid:" + conditionId));
                String recordedInstant = stringAt(
                        provJsonText, "diagnosisHeader", "accountableHealthcareProfessional", "authorTime");
                if (recordedInstant != null) {
                    provenance.setRecordedElement(new InstantType(recordedInstant));
                }

                FhirContext ctx = FhirContext.forR4();
                IParser parser = ctx.newJsonParser().setPrettyPrint(true);
                StringBuilder sb = new StringBuilder("{\n");
                sb.append("  \"condition\": ").append(indent(parser.encodeResourceToString(condition), 2)).append(",\n");
                sb.append("  \"provenance\": ").append(indent(parser.encodeResourceToString(provenance), 2)).append("\n");
                sb.append("}\n");
                result = sb.toString();
            }
        } finally {
            System.setOut(realOut);
        }
        System.out.println(result);
    }

    /** Sets Condition.onsetDateTime and the ext-asserted-date extension's valueDateTime
     * directly from values ALREADY ISO-8601-converted by the viewer backend's JSON builder -
     * see get-diagnosis-to-condition.map's header comment for why FML cannot set these two
     * dateTime-typed choice elements on either engine. Mirrors
     * HapiMapperDump.setOnsetAndAssertedDate / run_pipeline.py's
     * _set_onset_and_asserted_date, but reads the already-converted ISO string straight out
     * of the source JSON instead of re-parsing a raw RIVTA date, since the input here is
     * already the resolved lm-diagnosis document. */
    private static void setOnsetAndAssertedDate(Condition condition, String condJsonText) {
        String diagnosisTime = stringAt(condJsonText, "diagnosisBody", "diagnosisTime");
        if (diagnosisTime != null) {
            condition.setOnset(new DateTimeType(diagnosisTime));
        }
        String signatureTime = stringAt(condJsonText, "diagnosisHeader", "legalAuthenticator", "signatureTime");
        if (signatureTime != null) {
            for (Extension ext : condition.getExtension()) {
                if (ASSERTED_DATE_EXTENSION_URL.equals(ext.getUrl())) {
                    ext.setValue(new DateTimeType(signatureTime));
                }
            }
        }
    }

    /** Sets Condition.meta.source, Condition.subject.identifier.system and
     * Condition.code.coding[0].system directly from the already-resolved source JSON - see
     * HapiMapperDump.setUriFields's Javadoc for why these three "uri" fields cannot be set via
     * FML on HAPI (a confirmed engine bug when create('uri') consumes a navigated Element). */
    private static void setUriFields(Condition condition, String condJsonText) {
        JsonObject root = JsonParser.parseString(condJsonText).getAsJsonObject();
        JsonObject diag = root.getAsJsonArray("diagnosis").get(0).getAsJsonObject();
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

    /** Reads diagnosis[0].&lt;path...&gt; as a string, or null if any segment is absent. */
    private static String stringAt(String jsonText, String... path) {
        JsonObject root = JsonParser.parseString(jsonText).getAsJsonObject();
        if (!root.has("diagnosis") || root.getAsJsonArray("diagnosis").size() == 0) return null;
        JsonObject cur = root.getAsJsonArray("diagnosis").get(0).getAsJsonObject();
        for (int i = 0; i < path.length - 1; i++) {
            if (!cur.has(path[i])) return null;
            cur = cur.getAsJsonObject(path[i]);
        }
        String last = path[path.length - 1];
        if (!cur.has(last)) return null;
        return cur.get(last).getAsString();
    }

    private static String indent(String json, int spaces) {
        String pad = " ".repeat(spaces);
        return json.replace("\n", "\n" + pad);
    }
}
