package se.inera.ehds.malac.hapi;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.Provenance;
import org.hl7.fhir.r4.model.Reference;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
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
 * <p>WHICH fields those are, and where each comes from/goes, is no longer hardcoded here - it
 * is read from {@code ../../driver-fields.json} (this module's base directory, i.e. the SAME
 * file run_pipeline.py/driver_fields.py read on the Python side and lineage.py reads for the
 * viewer's trace - see driver_fields.py's module docstring for the full account of why this
 * used to be three independent copies). Only the "how to place a value of this shape into the
 * typed HAPI model" mechanics - inherently per-field, since HAPI's model is statically typed -
 * still live in Java, as the {@link #CONDITION_SETTERS}/{@link #PROVENANCE_SETTERS} dispatch
 * tables below; {@code DriverFieldsConfigTest} fails if that dispatch ever drifts from the
 * config's own field-id list (an id in the config with no setter wired, or vice versa).
 *
 * <p>Usage: {@code HapiLineageCli <condition-source.json> <provenance-source.json>}
 */
public final class HapiLineageCli {

    private HapiLineageCli() {}

    @FunctionalInterface
    interface ConditionFieldSetter {
        void apply(Condition condition, String value, JsonObject field);
    }

    @FunctionalInterface
    interface ProvenanceFieldSetter {
        void apply(Provenance provenance, String value, JsonObject field);
    }

    /** One entry per "condition" field driver-fields.json declares - see that file and this
     * class's own Javadoc. Adding a config entry without adding a setter here (or vice versa)
     * is caught by DriverFieldsConfigTest, not silently ignored. */
    static final Map<String, ConditionFieldSetter> CONDITION_SETTERS = buildConditionSetters();

    /** Same as {@link #CONDITION_SETTERS}, for the "provenance" field list. */
    static final Map<String, ProvenanceFieldSetter> PROVENANCE_SETTERS = buildProvenanceSetters();

    private static Map<String, ConditionFieldSetter> buildConditionSetters() {
        Map<String, ConditionFieldSetter> setters = new LinkedHashMap<>();
        setters.put("driver-onset", (condition, value, field) -> condition.setOnset(new DateTimeType(value)));
        setters.put("assertedDate-value", (condition, value, field) -> {
            String extensionUrl = field.get("extension_url").getAsString();
            for (Extension ext : condition.getExtension()) {
                if (extensionUrl.equals(ext.getUrl())) {
                    ext.setValue(new DateTimeType(value));
                }
            }
        });
        setters.put("driver-meta-source", (condition, value, field) -> {
            if (condition.hasMeta()) {
                condition.getMeta().setSource(value);
            }
        });
        setters.put("driver-subject-identifier-system", (condition, value, field) -> {
            if (condition.hasSubject() && condition.getSubject().hasIdentifier()) {
                condition.getSubject().getIdentifier().setSystem(value);
            }
        });
        setters.put("driver-code-coding-system", (condition, value, field) -> {
            if (condition.hasCode() && !condition.getCode().getCoding().isEmpty()) {
                condition.getCode().getCodingFirstRep().setSystem(value);
            }
        });
        return setters;
    }

    private static Map<String, ProvenanceFieldSetter> buildProvenanceSetters() {
        Map<String, ProvenanceFieldSetter> setters = new LinkedHashMap<>();
        setters.put("driver-recorded", (provenance, value, field) -> provenance.setRecordedElement(new InstantType(value)));
        return setters;
    }

    /** Reads ../../driver-fields.json (moduleRoot/driver-fields.json) - the ONE declarative
     * list of driver-set fields, see this class's own Javadoc. */
    static JsonObject loadDriverFieldsConfig(Path moduleRoot) throws IOException {
        String text = Files.readString(moduleRoot.resolve("driver-fields.json"), StandardCharsets.UTF_8);
        return JsonParser.parseString(text).getAsJsonObject();
    }

    /** Applies every "condition" entry in `fields` whose source value is present in
     * `condJsonText` (the SAME per-call lm-diagnosis JSON the transform just consumed) -
     * mirrors driver_fields.apply_condition_fields() on the Python side. Returns the set of
     * field ids actually applied (used by DriverFieldsConfigTest). */
    static Set<String> applyConditionFields(Condition condition, String condJsonText, JsonArray fields) {
        Set<String> applied = new LinkedHashSet<>();
        for (JsonElement el : fields) {
            JsonObject field = el.getAsJsonObject();
            String id = field.get("id").getAsString();
            String value = stringAtPath(condJsonText, field.get("source_path").getAsString());
            if (value == null) continue;
            ConditionFieldSetter setter = CONDITION_SETTERS.get(id);
            if (setter == null) {
                throw new IllegalStateException(
                        "driver-fields.json has a 'condition' entry '" + id
                                + "' with no Java setter wired in HapiLineageCli.CONDITION_SETTERS - add one");
            }
            setter.apply(condition, value, field);
            applied.add(id);
        }
        return applied;
    }

    /** Same as {@link #applyConditionFields}, for the "provenance" field list. */
    static Set<String> applyProvenanceFields(Provenance provenance, String provJsonText, JsonArray fields) {
        Set<String> applied = new LinkedHashSet<>();
        for (JsonElement el : fields) {
            JsonObject field = el.getAsJsonObject();
            String id = field.get("id").getAsString();
            String value = stringAtPath(provJsonText, field.get("source_path").getAsString());
            if (value == null) continue;
            ProvenanceFieldSetter setter = PROVENANCE_SETTERS.get(id);
            if (setter == null) {
                throw new IllegalStateException(
                        "driver-fields.json has a 'provenance' entry '" + id
                                + "' with no Java setter wired in HapiLineageCli.PROVENANCE_SETTERS - add one");
            }
            setter.apply(provenance, value, field);
            applied.add(id);
        }
        return applied;
    }

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
            JsonObject driverFields = loadDriverFieldsConfig(moduleRoot);

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
                applyConditionFields(condition, condJsonText, driverFields.getAsJsonArray("condition"));

                Base provSource = engine.parseSource(provJsonText);
                Provenance provenance = (Provenance) engine.transformToProvenance(provSource);
                provenance.setId(UUID.randomUUID().toString());
                provenance.addTarget(new Reference("urn:uuid:" + conditionId));
                applyProvenanceFields(provenance, provJsonText, driverFields.getAsJsonArray("provenance"));

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

    /** Reads `path` (e.g. "diagnosis[].diagnosisBody.diagnosisTime") out of `jsonText` -
     * "[]" always resolves to index 0, since every driver-field source_path's only repeating
     * segment is "diagnosis[]" and this CLI is only ever given a single-diagnosis document
     * (see driver_fields.py's _get_source_value(), the identical Python-side resolver). */
    private static String stringAtPath(String jsonText, String path) {
        JsonElement cur = JsonParser.parseString(jsonText);
        for (String token : path.split("\\.")) {
            boolean isArray = token.endsWith("[]");
            String name = isArray ? token.substring(0, token.length() - 2) : token;
            if (cur == null || !cur.isJsonObject() || !cur.getAsJsonObject().has(name)) return null;
            cur = cur.getAsJsonObject().get(name);
            if (isArray) {
                if (cur == null || !cur.isJsonArray() || cur.getAsJsonArray().size() == 0) return null;
                cur = cur.getAsJsonArray().get(0);
            }
        }
        return (cur != null && cur.isJsonPrimitive()) ? cur.getAsString() : null;
    }

    private static String indent(String json, int spaces) {
        String pad = " ".repeat(spaces);
        return json.replace("\n", "\n" + pad);
    }
}
