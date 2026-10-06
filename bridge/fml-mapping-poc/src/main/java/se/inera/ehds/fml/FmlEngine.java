package se.inera.ehds.fml;

import org.hl7.fhir.r4.context.SimpleWorkerContext;
import org.hl7.fhir.r4.elementmodel.JsonParser;
import org.hl7.fhir.r4.fhirpath.FHIRPathEngine;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.StructureMap;
import org.hl7.fhir.r4.utils.StructureMapUtilities;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic, registry-driven FML runner — a matchbox-$transform-style engine: adding a new
 * target resource for an existing (or new) RIVTA service contract means adding one
 * {@link MappingDefinition} entry and one {@code .map} file, never a new Java field/method.
 * This replaces the earlier version of this class, which had one hand-written field + method
 * per target resource (see git history / fml-evaluation.md "Rekommendation").
 *
 * The source side is always a real logical-model instance (an
 * {@code org.hl7.fhir.r4.elementmodel.Element}, parsed from JSON built by a
 * *JsonSourceBuilder against the EHDS-TK-published logical model for that service contract),
 * never a flattened Parameters stand-in — see fml-evaluation.md "Rättelse: den logiska modellen
 * behöver inte författas - den finns redan".
 *
 * Each call produces exactly one resource (one {@link #transform} call = one target), but
 * several {@link MappingDefinition}s can share the same {@code sourceLogicalModelUrl} — e.g.
 * GetDiagnosis's logical model feeds both a Condition map and a Provenance map, matching
 * Oskar's "olika resurser ska kunna ha samma tjänstekontrakt i botten" requirement.
 *
 * {@code stopIfFalseFhirPath}, when set, is evaluated against the source before transform()
 * runs: a false/empty result means the whole resource is skipped (the "stoppa resursen"
 * error-handling policy from the original evaluation ask), configured declaratively per
 * registry entry instead of hand-coded per mapper in Java.
 */
public final class FmlEngine {

    public record MappingDefinition(
            String key,
            String sourceLogicalModelResource,
            String mapResourcePath,
            String groupName,
            String targetResourceType,
            String stopIfFalseFhirPath) {
    }

    /** The registry: add an entry here (+ a .map file) to support a new resource/contract. */
    private static final List<MappingDefinition> REGISTRY = List.of(
            new MappingDefinition(
                    "GetDiagnosisToCondition", "/fhir/lm-diagnosis.json",
                    "/fml/get-diagnosis-to-condition.map", "GetDiagnosisToCondition", "Condition",
                    "diagnosis.diagnosisHeader.patientId.value.matches('^[0-9]{12}$')"),
            new MappingDefinition(
                    "GetDiagnosisToProvenance", "/fhir/lm-diagnosis.json",
                    "/fml/get-diagnosis-to-provenance.map", "GetDiagnosisToProvenance", "Provenance",
                    null),
            new MappingDefinition(
                    "GetCareDocumentationToDocumentReference", "/fhir/lm-caredocumentation.json",
                    "/fml/get-caredocumentation-to-documentreference.map",
                    "GetCareDocumentationToDocumentReference", "DocumentReference", null),
            new MappingDefinition(
                    "GetCareDocumentationToProvenance", "/fhir/lm-caredocumentation.json",
                    "/fml/get-caredocumentation-to-provenance.map",
                    "GetCareDocumentationToProvenance", "Provenance", null)
    );

    private static final String[] CORE_DEFINITION_RESOURCES = {
            "/org/hl7/fhir/r4/model/profile/profiles-types.xml",
            "/org/hl7/fhir/r4/model/profile/profiles-resources.xml",
            "/org/hl7/fhir/r4/model/extension/extension-definitions.xml"
    };

    private final SimpleWorkerContext ctx;
    private final StructureMapUtilities smu;
    private final FHIRPathEngine fhirPathEngine;
    private final Map<String, MappingDefinition> registry = new LinkedHashMap<>();
    private final Map<String, StructureMap> maps = new LinkedHashMap<>();
    private final Map<String, StructureDefinition> logicalModels = new LinkedHashMap<>();

    public FmlEngine() throws IOException {
        this.ctx = SimpleWorkerContext.fromDefinitions(loadCoreDefinitions());
        ctx.setCanRunWithoutTerminology(true);
        ctx.setExpansionProfile(new Parameters());
        ctx.cacheResource(readConceptMap("/fhir/conceptmap-diagnosis-type.json"));
        ctx.cacheResource(readConceptMap("/fhir/conceptmap-codesystem-oid.json"));
        this.smu = new StructureMapUtilities(ctx);
        this.fhirPathEngine = new FHIRPathEngine(ctx);

        for (MappingDefinition def : REGISTRY) {
            StructureDefinition lm = logicalModels.computeIfAbsent(def.sourceLogicalModelResource(), path -> {
                try {
                    StructureDefinition sd = readStructureDefinition(path);
                    ctx.cacheResource(sd);
                    return sd;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            registry.put(def.key(), def);
            maps.put(def.key(), smu.parse(readResource(def.mapResourcePath()), def.groupName()));
            // force a reference so unused-variable tools don't flag it; kept for clarity.
            assert lm != null;
        }
    }

    /**
     * Parses a JSON instance of the named logical model into an Element usable directly as an
     * FML source — see *JsonSourceBuilder for how the JSON itself is built from the RIVTA
     * objects.
     */
    public Base parseSource(String logicalModelResource, String json) throws Exception {
        StructureDefinition sd = logicalModels.get(logicalModelResource);
        if (sd == null) throw new IllegalArgumentException("Unregistered logical model: " + logicalModelResource);
        return new JsonParser(ctx).parse(json, sd.getType());
    }

    /**
     * Runs the named mapping. Returns null if the registry entry's stop-the-resource guard
     * evaluates false against the source (see class javadoc) — mirroring the production
     * Java mappers' "invalid record -> no resource at all" behaviour, but driven by a FHIRPath
     * expression in the registry instead of a hand-written null-return in a mapper class.
     */
    public Base transform(String key, Base source) {
        MappingDefinition def = registry.get(key);
        if (def == null) throw new IllegalArgumentException("Unknown mapping: " + key);
        if (def.stopIfFalseFhirPath() != null && !evaluatesTrue(source, def.stopIfFalseFhirPath())) {
            return null;
        }
        Base target = newTargetInstance(def.targetResourceType());
        smu.transform(null, source, maps.get(key), target);
        return target;
    }

    private boolean evaluatesTrue(Base source, String fhirPath) {
        List<Base> result = fhirPathEngine.evaluate(source, fhirPath);
        return !result.isEmpty() && result.get(0) instanceof BooleanType bt && bt.booleanValue();
    }

    private static Base newTargetInstance(String resourceType) {
        try {
            return (Base) Class.forName("org.hl7.fhir.r4.model." + resourceType)
                    .getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Could not instantiate target resource type " + resourceType, e);
        }
    }

    private ConceptMap readConceptMap(String path) throws IOException {
        try (InputStream is = FmlEngine.class.getResourceAsStream(path)) {
            if (is == null) throw new IOException("Resource not found: " + path);
            return (ConceptMap) ctx.newJsonParser().parse(is);
        }
    }

    private StructureDefinition readStructureDefinition(String path) throws IOException {
        try (InputStream is = FmlEngine.class.getResourceAsStream(path)) {
            if (is == null) throw new IOException("Resource not found: " + path);
            return (StructureDefinition) new org.hl7.fhir.r4.formats.JsonParser().parse(is);
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
