package se.inera.ehds.malac.hapi;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import se.inera.ehds.malac.JavaMapperDump;
import se.inera.ehds.mapping.naming.NamingSystemRegistry;
import se.inera.ehds.mapping.rivta.Diagnosis;
import se.inera.ehds.mapping.rivta.GetDiagnosisResponse;

import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Provenance;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fails if HapiLineageCli's Java-side field-setting code ever drifts from
 * ../../../driver-fields.json - the single declarative list of driver-set fields this module
 * root holds (see driver_fields.py's module docstring and HapiLineageCli's own Javadoc for the
 * full account of why this file exists: there used to be THREE independent copies of this same
 * field list, one of them right here in Java).
 *
 * <p>Two things are checked, deliberately kept separate:
 * <ol>
 *   <li>STATIC: every id the config lists has a Java setter wired in
 *       {@link HapiLineageCli#CONDITION_SETTERS}/{@link HapiLineageCli#PROVENANCE_SETTERS},
 *       and vice versa - so adding a field to the config without wiring a setter (or leaving a
 *       setter wired for a field removed from the config) fails immediately, without needing
 *       any fixture data to trigger it.</li>
 *   <li>DYNAMIC: running HapiLineageCli's actual apply methods against a real fixture's source
 *       JSON applies EXACTLY the ids the config declares (every one of this config's five
 *       fields has a present source value in this fixture) - a regression check that the
 *       dispatch tables keep working end to end, not just existing.</li>
 * </ol>
 */
class DriverFieldsConfigTest {

    private static final Path MODULE_ROOT = Path.of(".").toAbsolutePath().normalize();

    private static Set<String> idsOf(JsonArray fields) {
        Set<String> ids = new LinkedHashSet<>();
        for (JsonElement el : fields) {
            ids.add(el.getAsJsonObject().get("id").getAsString());
        }
        return ids;
    }

    @Test
    void konfigurationens_falt_matchar_de_java_faktiskt_har_satters_for() throws Exception {
        JsonObject config = HapiLineageCli.loadDriverFieldsConfig(MODULE_ROOT);

        assertEquals(idsOf(config.getAsJsonArray("condition")), HapiLineageCli.CONDITION_SETTERS.keySet(),
                "driver-fields.json's 'condition' field ids and HapiLineageCli.CONDITION_SETTERS's keys must match exactly");
        assertEquals(idsOf(config.getAsJsonArray("provenance")), HapiLineageCli.PROVENANCE_SETTERS.keySet(),
                "driver-fields.json's 'provenance' field ids and HapiLineageCli.PROVENANCE_SETTERS's keys must match exactly");
    }

    @Test
    void applicering_pa_huvuddiagnos_fixturen_satter_exakt_konfigurationens_falt() throws Exception {
        JsonObject config = HapiLineageCli.loadDriverFieldsConfig(MODULE_ROOT);
        HapiFmlEngine engine = new HapiFmlEngine(MODULE_ROOT);
        NamingSystemRegistry namingSystem = new NamingSystemRegistry();
        LmDiagnosisJsonSourceBuilder sourceBuilder = new LmDiagnosisJsonSourceBuilder(namingSystem);

        GetDiagnosisResponse response = JavaMapperDump.huvuddiagnosFixture();
        Diagnosis diag = response.getDiagnosis().get(0);

        String condJsonText = sourceBuilder.toJson(diag, false);
        Base condSource = engine.parseSource(condJsonText);
        assertTrue(engine.passesDiag001Guard(condSource), "huvuddiagnos-fixturen ska passera DIAG-001");
        Condition condition = (Condition) engine.transformToCondition(condSource);

        Set<String> appliedCondition = HapiLineageCli.applyConditionFields(
                condition, condJsonText, config.getAsJsonArray("condition"));
        assertEquals(idsOf(config.getAsJsonArray("condition")), appliedCondition,
                "huvuddiagnos-fixturen ska ha ett killvarde for varje driver-satt condition-falt");
        assertTrue(condition.hasOnset(), "driver-onset ska ha satt Condition.onsetDateTime");
        assertEquals("https://tjanstekatalogen.inera.se/Endpoint/SE2321000016-4HK5",
                condition.getMeta().getSource(), "driver-meta-source ska ha satt Condition.meta.source");

        String provJsonText = sourceBuilder.toJson(diag, true);
        Base provSource = engine.parseSource(provJsonText);
        Provenance provenance = (Provenance) engine.transformToProvenance(provSource);
        Set<String> appliedProvenance = HapiLineageCli.applyProvenanceFields(
                provenance, provJsonText, config.getAsJsonArray("provenance"));
        assertEquals(idsOf(config.getAsJsonArray("provenance")), appliedProvenance,
                "huvuddiagnos-fixturen ska ha ett killvarde for driver-recorded");
        assertTrue(provenance.hasRecorded(), "driver-recorded ska ha satt Provenance.recorded");
    }
}
