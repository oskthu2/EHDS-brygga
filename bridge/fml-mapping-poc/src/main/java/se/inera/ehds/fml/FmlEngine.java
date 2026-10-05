package se.inera.ehds.fml;

import org.hl7.fhir.r4.context.SimpleWorkerContext;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Provenance;
import org.hl7.fhir.r4.model.StructureMap;
import org.hl7.fhir.r4.utils.StructureMapUtilities;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal runner for the GetDiagnosis FML evaluation PoC: an offline
 * (classpath-only) worker context, the two evaluation ConceptMaps, and the
 * GetDiagnosisToCondition StructureMap. Not used by the production bridge.
 *
 * {@code SimpleWorkerContext.fromClassPath()} / {@code fromPackage(...)} expects either a
 * bundled "validation.json.zip" (no longer shipped in hapi-fhir-validation-resources-r4
 * 7.4.x) or the "hl7.fhir.r4.core" npm package fetched from packages.fhir.org — unreachable
 * from this sandbox (403, see team memory). Falling back to {@code fromDefinitions(...)} fed
 * directly from the core profiles-{types,resources}.xml and extension-definitions.xml that
 * ARE still on the classpath (same jar, just not in the one bundle fromClassPath() looks for)
 * is itself a finding worth keeping: an offline FML engine is possible here, but only once you
 * know to reach past the documented entry point.
 */
public final class FmlEngine {

    private static final String[] CORE_DEFINITION_RESOURCES = {
            "/org/hl7/fhir/r4/model/profile/profiles-types.xml",
            "/org/hl7/fhir/r4/model/profile/profiles-resources.xml",
            "/org/hl7/fhir/r4/model/extension/extension-definitions.xml"
    };

    private final SimpleWorkerContext ctx;
    private final StructureMapUtilities smu;
    private final StructureMap map;
    private final StructureMap provenanceMap;
    private final StructureMap careDocMap;

    public FmlEngine() throws IOException {
        this.ctx = SimpleWorkerContext.fromDefinitions(loadCoreDefinitions());
        ctx.setCanRunWithoutTerminology(true);
        ctx.setExpansionProfile(new Parameters());
        ctx.cacheResource(readConceptMap("/fhir/conceptmap-diagnosis-type.json"));
        ctx.cacheResource(readConceptMap("/fhir/conceptmap-codesystem-oid.json"));
        this.smu = new StructureMapUtilities(ctx);
        this.map = smu.parse(readResource("/fml/get-diagnosis-to-condition.map"),
                "GetDiagnosisToCondition");
        // Separat StructureMap för Provenance - transform() stödjer bara en target per anrop,
        // se kommentaren i get-diagnosis-to-provenance.map.
        this.provenanceMap = smu.parse(readResource("/fml/get-diagnosis-to-provenance.map"),
                "GetDiagnosisToProvenance");
        this.careDocMap = smu.parse(readResource("/fml/get-caredocumentation-to-documentreference.map"),
                "GetCareDocumentationToDocumentReference");
    }

    public String render() {
        return StructureMapUtilities.render(map);
    }

    public Condition transformDiagnosis(Parameters source) {
        Condition target = new Condition();
        smu.transform(null, source, map, target);
        return target;
    }

    public Provenance transformDiagnosisProvenance(Parameters source) {
        Provenance target = new Provenance();
        smu.transform(null, source, provenanceMap, target);
        return target;
    }

    public DocumentReference transformCareDocumentation(Parameters source) {
        DocumentReference target = new DocumentReference();
        smu.transform(null, source, careDocMap, target);
        return target;
    }

    private ConceptMap readConceptMap(String path) throws IOException {
        try (InputStream is = FmlEngine.class.getResourceAsStream(path)) {
            if (is == null) throw new IOException("Resource not found: " + path);
            return (ConceptMap) ctx.newJsonParser().parse(is);
        }
    }

    private static Map<String, byte[]> loadCoreDefinitions() throws IOException {
        Map<String, byte[]> defs = new LinkedHashMap<>();
        for (String path : CORE_DEFINITION_RESOURCES) {
            try (InputStream is = FmlEngine.class.getResourceAsStream(path)) {
                if (is == null) throw new IOException("Core definition resource not found: " + path);
                String name = path.substring(path.lastIndexOf('/') + 1);
                defs.put(name, is.readAllBytes());
            }
        }
        return defs;
    }

    private String readResource(String path) throws IOException {
        try (InputStream is = FmlEngine.class.getResourceAsStream(path)) {
            if (is == null) throw new IOException("Resource not found: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
