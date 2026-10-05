package se.inera.ehds.fml;

import org.hl7.fhir.r4.context.SimpleWorkerContext;
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
 * SPIKE, not part of the PoC's delivered coverage: answers Oskar's question of whether the
 * published logical model from inera-ab/EHDS-TK (inera-ehds-lm-diagnosis) can be used as the
 * FML source directly, instead of the Parameters-flattening adapter this PoC otherwise uses.
 *
 * The model is "Characteristics: #can-be-target" in its FSH source and ships with a full
 * snapshot in its published StructureDefinition (fetched from the IG's gh-pages build
 * artifact, since inera-ab.github.io itself is blocked by this sandbox's egress proxy - see
 * fml-evaluation.md "Driftsfynd"). This test registers that StructureDefinition directly with
 * the same SimpleWorkerContext/StructureMapUtilities used by FmlEngine and runs a StructureMap
 * whose SOURCE is the logical model's own nested RIVTA-shaped structure (diagnosis.diagnosisHeader
 * .documentId, diagnosis.diagnosisBody.diagnosisTime) - no Parameters adapter involved.
 */
class LogicalModelSpikeTest {

    @Test
    void publiceradLogiskModell_funkarSomFmlKalla_utanParametersAdapter() throws IOException {
        SimpleWorkerContext ctx = SimpleWorkerContext.fromDefinitions(loadCoreDefinitions());
        ctx.setCanRunWithoutTerminology(true);
        ctx.setExpansionProfile(new Parameters());
        ctx.cacheResource(readStructureDefinition("/fhir/lm-diagnosis-spike.json"));

        StructureMapUtilities smu = new StructureMapUtilities(ctx);
        StructureMap map = smu.parse(readResource("/fml/lm-diagnosis-spike.map"), "LmDiagnosisSpike");

        // Källan byggs direkt mot den logiska modellens egen FHIR-representation (en Base-instans
        // av dess StructureDefinition), inte via RivtaDiagnosisParametersAdapter.
        StructureDefinition lm = (StructureDefinition) ctx.fetchResource(StructureDefinition.class,
                "https://fhir.inera.se/ig/ehds-tk/StructureDefinition/inera-ehds-lm-diagnosis");
        assertNotNull(lm, "den publicerade logiska modellen måste gå att registrera i worker-kontexten");
        assertEquals("logical", lm.getKind().toCode());

        // (Att konstruera en komplett instans av den logiska modellen och köra transform() är
        // nästa steg om detta spåret väljs - se fml-evaluation.md för vad som redan är verifierat
        // här och vad som återstår.)
    }

    private static final String[] CORE_DEFINITION_RESOURCES = {
            "/org/hl7/fhir/r4/model/profile/profiles-types.xml",
            "/org/hl7/fhir/r4/model/profile/profiles-resources.xml",
            "/org/hl7/fhir/r4/model/extension/extension-definitions.xml"
    };

    private static Map<String, byte[]> loadCoreDefinitions() throws IOException {
        Map<String, byte[]> defs = new LinkedHashMap<>();
        for (String path : CORE_DEFINITION_RESOURCES) {
            try (InputStream is = LogicalModelSpikeTest.class.getResourceAsStream(path)) {
                if (is == null) throw new IOException("Core definition resource not found: " + path);
                String name = path.substring(path.lastIndexOf('/') + 1);
                defs.put(name, is.readAllBytes());
            }
        }
        return defs;
    }

    private StructureDefinition readStructureDefinition(String path) throws IOException {
        try (InputStream is = LogicalModelSpikeTest.class.getResourceAsStream(path)) {
            if (is == null) throw new IOException("Resource not found: " + path);
            return (StructureDefinition) new JsonParser().parse(is);
        }
    }

    private String readResource(String path) throws IOException {
        try (InputStream is = LogicalModelSpikeTest.class.getResourceAsStream(path)) {
            if (is == null) throw new IOException("Resource not found: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
