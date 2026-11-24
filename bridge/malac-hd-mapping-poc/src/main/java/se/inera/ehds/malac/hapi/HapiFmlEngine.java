package se.inera.ehds.malac.hapi;

import org.hl7.fhir.r4.context.SimpleWorkerContext;
import org.hl7.fhir.r4.elementmodel.JsonParser;
import org.hl7.fhir.r4.fhirpath.FHIRPathEngine;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.StructureMap;
import org.hl7.fhir.r4.utils.StructureMapUtilities;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Runs the exact SAME {@code .map} files MaLaC-HD compiles (see {@code ../../python/fml/},
 * loaded from disk here, never copied into this module's own resources) via HAPI FHIR's
 * {@code org.hl7.fhir.r4.utils.StructureMapUtilities} - a second engine on the shared/portable
 * FML artifact, so Oskar can compare MaLaC-HD-generated Python output against HAPI-executed
 * output for the SAME {@code .map} source. Design pattern borrowed from
 * {@code claude/fml-mapping-9ovqyh}'s {@code FmlEngine} (prior art, read for reference - this
 * class is a from-scratch reimplementation scoped to GetDiagnosis only, not a dependency on
 * that unmerged branch).
 *
 * <p>Unlike that branch's registry-driven, multi-contract design, this engine is deliberately
 * narrow (GetDiagnosis -&gt; Condition/Provenance only) - the SAME scope as the MaLaC-HD track
 * it compares against. See malac-hd-evaluation.md for the full account.
 *
 * <p><b>Why this class has no real execution tracing, unlike the Python/MaLaC-HD track's
 * {@code traced_python_engine.py}:</b> the exact pinned {@code org.hl7.fhir.r4:6.3.11} jar was
 * decompiled and inspected directly for a usable hook (not just documentation/web search).
 * {@code StructureMapUtilities}'s constructor DOES accept an {@code ITransformerServices}
 * callback, and its private {@code log(String)} method routes every debug line through
 * {@code services.log(...)} instead of {@code System.out} once one is supplied - including one
 * call, in {@code executeRule()}, of exactly {@code log(rule.getName() + " / " +
 * vars.summary())} per rule EXECUTION (confirmed by running it - see the {@code rule : ... ;
 * vars = ...} lines a plain {@code mvn test} prints for this module, currently going to
 * {@code System.out} because no {@code ITransformerServices} is supplied here). That is real,
 * and more than nothing, but it fires at rule ENTRY with a free-text {@code
 * Variables.summary()} snapshot, not at the point a property is actually SET on the target
 * (that logic - {@code processTarget}/{@code runTransform} - is private, with no hook of any
 * kind). Turning it into a real, concrete-index trace would mean parsing yet another piece of
 * free text, which is not a clear improvement in rigor over what it would replace; going
 * further still (wrapping the private target-processing methods via reflection, or
 * instrumenting {@code Base.setProperty()} itself) would need something close to a Java
 * agent/bytecode-weaving approach, judged disproportionate for this dev tool. See
 * {@code viewer/README.md}'s "How the source -&gt; target lineage is computed" for the full
 * account and what this means for {@code relatedDiagnosis} index resolution specifically.
 */
public final class HapiFmlEngine {

    /** Mirrors GetDiagnosisMapper.TJANSTEKATALOG_BASE / riv_date.TJANSTEKATALOG_BASE. */
    public static final String TJANSTEKATALOG_BASE = "https://tjanstekatalogen.inera.se";

    /** DIAG-001 "stop the resource" guard, evaluated against the parsed source BEFORE
     * transform() runs - mirrors GetDiagnosisMapper.mapDiagnosis()'s personnummer check and
     * guard.py's PERSONNUMMER_PATTERN (both engines apply it outside FML; a real StructureMap
     * transform() has no "abort the whole call" primitive of its own, see
     * claude/fml-mapping-9ovqyh's FmlEngine.stopIfFalseFhirPath, reimplemented here). */
    private static final String DIAG_001_GUARD =
            "diagnosis.diagnosisHeader.patientId.value.matches('^[0-9]{12}$')";

    private static final String[] CORE_DEFINITION_RESOURCES = {
            "/org/hl7/fhir/r4/model/profile/profiles-types.xml",
            "/org/hl7/fhir/r4/model/profile/profiles-resources.xml",
            "/org/hl7/fhir/r4/model/extension/extension-definitions.xml",
    };

    private final SimpleWorkerContext ctx;
    private final StructureMapUtilities smu;
    private final FHIRPathEngine fhirPathEngine;
    private final StructureDefinition lmDiagnosis;
    private final StructureMap conditionMap;
    private final StructureMap provenanceMap;

    /**
     * @param moduleRoot this Maven module's base directory (the one holding {@code pom.xml}),
     *                   so the SAME {@code python/fml/*.map} and {@code fhir/lm-diagnosis.json}
     *                   files the Python track uses can be read straight off disk.
     */
    public HapiFmlEngine(Path moduleRoot) throws IOException {
        this.ctx = SimpleWorkerContext.fromDefinitions(loadCoreDefinitions());
        ctx.setCanRunWithoutTerminology(true);
        ctx.setExpansionProfile(new Parameters());
        ctx.cacheResource(readConceptMap(moduleRoot.resolve("python/conceptmaps/diagnosis-type.json")));

        this.lmDiagnosis = readStructureDefinition(moduleRoot.resolve("fhir/lm-diagnosis.json"));
        ctx.cacheResource(lmDiagnosis);

        this.smu = new StructureMapUtilities(ctx);
        this.fhirPathEngine = new FHIRPathEngine(ctx);
        this.conditionMap = smu.parse(
                readFile(moduleRoot.resolve("python/fml/get-diagnosis-to-condition.map")),
                "GetDiagnosisToCondition");
        this.provenanceMap = smu.parse(
                readFile(moduleRoot.resolve("python/fml/get-diagnosis-to-provenance.map")),
                "GetDiagnosisToProvenance");
    }

    /** Parses a JSON instance of inera-ehds-lm-diagnosis (see {@link LmDiagnosisJsonSourceBuilder})
     * into a HAPI {@code Element} source, usable directly as an FML source. */
    public Base parseSource(String json) throws Exception {
        return new JsonParser(ctx).parse(json, lmDiagnosis.getType());
    }

    /** True when the source passes DIAG-001 (a trustworthy 12-digit patient identifier) -
     * false means the caller should skip BOTH transform calls for this record entirely, same
     * as GetDiagnosisMapper.mapDiagnosis()'s "return null" / guard.py's drop. */
    public boolean passesDiag001Guard(Base source) {
        java.util.List<Base> result = fhirPathEngine.evaluate(source, DIAG_001_GUARD);
        return !result.isEmpty() && result.get(0) instanceof BooleanType bt && bt.booleanValue();
    }

    public Base transformToCondition(Base source) {
        Base target = new org.hl7.fhir.r4.model.Condition();
        smu.transform(null, source, conditionMap, target);
        return target;
    }

    public Base transformToProvenance(Base source) {
        Base target = new org.hl7.fhir.r4.model.Provenance();
        smu.transform(null, source, provenanceMap, target);
        return target;
    }

    /** Pretty-prints a transform() result as FHIR JSON. */
    public String toJson(Base resource) throws IOException {
        ca.uhn.fhir.parser.IParser parser = ca.uhn.fhir.context.FhirContext.forR4()
                .newJsonParser().setPrettyPrint(true);
        return parser.encodeResourceToString((Resource) resource);
    }

    private static ConceptMap readConceptMap(Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path)) {
            return (ConceptMap) new org.hl7.fhir.r4.formats.JsonParser().parse(is);
        }
    }

    private static StructureDefinition readStructureDefinition(Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path)) {
            return (StructureDefinition) new org.hl7.fhir.r4.formats.JsonParser().parse(is);
        }
    }

    private static String readFile(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static java.util.Map<String, byte[]> loadCoreDefinitions() throws IOException {
        java.util.Map<String, byte[]> defs = new java.util.LinkedHashMap<>();
        for (String path : CORE_DEFINITION_RESOURCES) {
            try (InputStream is = HapiFmlEngine.class.getResourceAsStream(path)) {
                if (is == null) throw new IOException("Core definition resource not found: " + path);
                String name = path.substring(path.lastIndexOf('/') + 1);
                defs.put(name, is.readAllBytes());
            }
        }
        return defs;
    }
}
