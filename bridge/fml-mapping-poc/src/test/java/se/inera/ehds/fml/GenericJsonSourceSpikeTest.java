package se.inera.ehds.fml;

import org.hl7.fhir.r4.context.SimpleWorkerContext;
import org.hl7.fhir.r4.elementmodel.Element;
import org.hl7.fhir.r4.formats.JsonParser;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.StructureMap;
import org.hl7.fhir.r4.utils.StructureMapUtilities;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SPIKE: proves that a hand-built JSON string matching the logical model's own shape can be
 * parsed into an org.hl7.fhir.r4.elementmodel.Element (a Base, usable directly as a
 * StructureMap source) via org.hl7.fhir.r4.elementmodel.JsonParser - no Parameters-flattening
 * adapter, no RIVTA-specific Java class needed on the source side. Not part of delivered
 * coverage; superseded by the real JSON source builders once this is confirmed.
 */
class GenericJsonSourceSpikeTest {

    @Test
    void handbyggdJson_motLogiskModell_funkarSomFmlKalla() throws Exception {
        SimpleWorkerContext ctx = SimpleWorkerContext.fromDefinitions(loadCoreDefinitions());
        ctx.setCanRunWithoutTerminology(true);
        ctx.setExpansionProfile(new Parameters());
        StructureDefinition lm = readStructureDefinition("/fhir/lm-diagnosis.json");
        ctx.cacheResource(lm);

        String json = """
            {
              "diagnosis": [
                {
                  "diagnosisHeader": { "documentId": "doc-1" },
                  "diagnosisBody": { "diagnosisTime": "2024-01-03T12:00:00+01:00", "chronicDiagnosis": true }
                }
              ]
            }
            """;

        org.hl7.fhir.r4.elementmodel.JsonParser emParser = new org.hl7.fhir.r4.elementmodel.JsonParser(ctx);
        Element source = emParser.parse(json, lm.getType());
        assertNotNull(source, "elementmodel-parsern ska kunna bygga ett Element från JSON mot den logiska modellen");

        StructureMapUtilities smu = new StructureMapUtilities(ctx);
        String mapText = """
            map "http://ehds-brygga.inera.se/fhir/StructureMap/GenericSpike" = "GenericSpike"
            uses "%s" alias LmDiagnosis as source
            uses "http://hl7.org/fhir/StructureDefinition/Condition" alias Condition as target
            group GenericSpike(source src : LmDiagnosis, target tgt : Condition) {
              src.diagnosis as d then {
                d.diagnosisBody as b then {
                  b.diagnosisTime as t -> tgt.onset = t "onset-value";
                  b.chronicDiagnosis as cc -> tgt.extension = create('Extension') as ext then {
                    cc -> ext.url = 'https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-chronic-condition' "chronic-url";
                    cc.value as ccv -> ext.value = create('boolean') as el then {
                      ccv -> el.value = ccv "chronic-value";
                    } "chronic-el";
                  } "chronic-ext";
                } "body";
              } "diagnosis";
            }
            """.formatted(lm.getUrl());
        StructureMap map = smu.parse(mapText, "GenericSpike");

        Condition target = new Condition();
        smu.transform(null, source, map, (Base) target);

        assertEquals("2024-01-03T12:00:00+01:00", target.getOnsetDateTimeType().getValueAsString(),
                "dateTime-fält ska castas direkt från Element via castToDateTime()");
        assertTrue(((org.hl7.fhir.r4.model.BooleanType) target.getExtensionByUrl(
                "https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-chronic-condition").getValue()).booleanValue(),
                "boolean-fält ska gå genom create('boolean') precis som för Parameters-källan");
    }

    private static final String[] CORE_DEFINITION_RESOURCES = {
            "/org/hl7/fhir/r4/model/profile/profiles-types.xml",
            "/org/hl7/fhir/r4/model/profile/profiles-resources.xml",
            "/org/hl7/fhir/r4/model/extension/extension-definitions.xml"
    };

    private static Map<String, byte[]> loadCoreDefinitions() throws IOException {
        Map<String, byte[]> defs = new LinkedHashMap<>();
        for (String path : CORE_DEFINITION_RESOURCES) {
            try (InputStream is = GenericJsonSourceSpikeTest.class.getResourceAsStream(path)) {
                if (is == null) throw new IOException("Core definition resource not found: " + path);
                String name = path.substring(path.lastIndexOf('/') + 1);
                defs.put(name, is.readAllBytes());
            }
        }
        return defs;
    }

    private StructureDefinition readStructureDefinition(String path) throws IOException {
        try (InputStream is = GenericJsonSourceSpikeTest.class.getResourceAsStream(path)) {
            if (is == null) throw new IOException("Resource not found: " + path);
            return (StructureDefinition) new JsonParser().parse(is);
        }
    }
}
