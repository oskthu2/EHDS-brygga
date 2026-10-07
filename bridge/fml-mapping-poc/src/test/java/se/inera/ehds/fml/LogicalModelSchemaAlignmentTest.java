package se.inera.ehds.fml;

import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against the logical models ({@code lm-diagnosis.json} / {@code lm-caredocumentation.json},
 * the FML sources {@link FmlEngine} parses) silently drifting from the RIV-TA schema they are
 * supposed to mirror on element names and cardinality (single vs. repeating) — Oskar's rule for
 * the FML track: the logical model follows the schema, not the TKB prose, so a new or changed
 * field has to land in both places or this test goes red (see thread "FML-spår för
 * konfigurerbar mappning", 2026-10-07).
 *
 * The schema side is read via reflection over the JAXB classes under mapping-engine's
 * {@code se.inera.ehds.mapping.rivta[.caredocumentation]} packages (verified against the real
 * XSD in PR #41) rather than the XSD itself, since those classes ARE the verified schema shape
 * this codebase already relies on. {@link #KNOWN_GAPS} lists drift that already existed when
 * this test was written — real schema fields the logical model does not carry (yet). Whether to
 * add them is a modelling decision for Oskar/EHDS-TK, not something to silently paper over here;
 * anything NOT listed is a regression and fails the test.
 */
class LogicalModelSchemaAlignmentTest {

    /**
     * Pre-existing drift (2026-10-07), schema-path -> reason. Kept in the test itself (not a
     * separate file) so a new gap has to be a deliberate, reviewed addition to this list.
     */
    private static final Map<String, String> KNOWN_GAPS = Map.ofEntries(
            Map.entry("diagnosis.diagnosisHeader.nullified",
                    "GetDiagnosis: 'dra tillbaka posten'-flagga, inget FHIR-mål beslutat än"),
            Map.entry("diagnosis.diagnosisHeader.nullifiedReason",
                    "GetDiagnosis: fritext till nullified, samma status"),
            Map.entry("diagnosis.diagnosisHeader.accountableHealthcareProfessional.healthcareProfessionalOrgUnit.orgUnitTelecom",
                    "GetDiagnosis: OrgUnitType-fält som aldrig togs med i den publicerade logiska modellen"),
            Map.entry("diagnosis.diagnosisHeader.accountableHealthcareProfessional.healthcareProfessionalOrgUnit.orgUnitEmail",
                    "GetDiagnosis: se orgUnitTelecom ovan"),
            Map.entry("diagnosis.diagnosisHeader.accountableHealthcareProfessional.healthcareProfessionalOrgUnit.orgUnitAddress",
                    "GetDiagnosis: se orgUnitTelecom ovan"),
            Map.entry("diagnosis.diagnosisHeader.accountableHealthcareProfessional.healthcareProfessionalOrgUnit.orgUnitLocation",
                    "GetDiagnosis: se orgUnitTelecom ovan"),
            Map.entry("result.subCode",
                    "GetDiagnosis: ResultType.subCode finns i schemat, inte i den publicerade logiska modellen")
    );

    /**
     * Medvetna namnbyten: JAXB-fältet {@code id} i RecordType/Author/Signature (så namngivet i
     * schemat) döptes om i den logiska modellen för att inte krocka med FHIR:s egna
     * BackboneElement.id-metaelement. Namnet skiljer sig alltså avsiktligt från schemat här -
     * motsatsen till okänd drift - så den jämförs mot sin omdöpta modellsökväg i stället för att
     * flaggas som saknad.
     */
    private static final Map<String, String> KNOWN_RENAMES = Map.of(
            "careDocumentation.header.record.id", "careDocumentation.header.record.recordId",
            "careDocumentation.header.author.id", "careDocumentation.header.author.authorId",
            "careDocumentation.header.signature.id", "careDocumentation.header.signature.signatureId"
    );

    private static final Set<String> LEAF_COMPLEX_TYPES = Set.of("PersonIdType", "CVType");

    @Test
    void getDiagnosis_logiskModellMatcharSchema() throws IOException {
        assertSchemaMatchesLogicalModel(
                se.inera.ehds.mapping.rivta.GetDiagnosisResponse.class,
                "/fhir/lm-diagnosis.json",
                "inera-ehds-lm-diagnosis");
    }

    @Test
    void getCareDocumentation_logiskModellMatcharSchema() throws IOException {
        assertSchemaMatchesLogicalModel(
                se.inera.ehds.mapping.rivta.caredocumentation.GetCareDocumentationResponse.class,
                "/fhir/lm-caredocumentation.json",
                "inera-ehds-lm-care-documentation");
    }

    private void assertSchemaMatchesLogicalModel(Class<?> responseClass, String logicalModelResource,
            String resourceTypeId) throws IOException {
        Map<String, Boolean> schemaPaths = new LinkedHashMap<>();
        collectJaxbPaths(responseClass, "", schemaPaths);
        Map<String, Boolean> modelPaths = collectLogicalModelPaths(logicalModelResource, resourceTypeId);

        List<String> missingFromModel = new ArrayList<>();
        List<String> cardinalityMismatch = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : schemaPaths.entrySet()) {
            String path = entry.getKey();
            if (KNOWN_GAPS.containsKey(path)) continue;
            String lookupPath = KNOWN_RENAMES.getOrDefault(path, path);
            Boolean modelRepeating = modelPaths.get(lookupPath);
            if (modelRepeating == null) {
                missingFromModel.add(path);
            } else if (!modelRepeating.equals(entry.getValue())) {
                cardinalityMismatch.add(path + " (schema repeating=" + entry.getValue()
                        + ", modell repeating=" + modelRepeating + ")");
            }
        }

        List<String> onlyInModel = new ArrayList<>();
        Set<String> renamedModelPaths = Set.copyOf(KNOWN_RENAMES.values());
        for (String path : modelPaths.keySet()) {
            if (!schemaPaths.containsKey(path) && !KNOWN_GAPS.containsKey(path)
                    && !renamedModelPaths.contains(path)) {
                onlyInModel.add(path);
            }
        }

        assertTrue(missingFromModel.isEmpty() && cardinalityMismatch.isEmpty() && onlyInModel.isEmpty(),
                "Logisk modell " + logicalModelResource + " har glidit från schemat ("
                        + responseClass.getSimpleName() + ").\n"
                        + "Saknas i modellen: " + missingFromModel + "\n"
                        + "Kardinalitet skiljer: " + cardinalityMismatch + "\n"
                        + "Finns bara i modellen, inte i schemat: " + onlyInModel);
    }

    // --- Schemasidan: fält-för-fält-vandring över JAXB-klassernas egna fält. ---

    private void collectJaxbPaths(Class<?> clazz, String prefix, Map<String, Boolean> out) {
        for (Field f : clazz.getDeclaredFields()) {
            if (f.isSynthetic() || Modifier.isStatic(f.getModifiers())) continue;
            String path = prefix.isEmpty() ? f.getName() : prefix + "." + f.getName();
            Class<?> fieldType = f.getType();
            if (List.class.isAssignableFrom(fieldType)) {
                Class<?> elementType = (Class<?>) ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0];
                out.put(path, true);
                if (!isLeaf(elementType)) collectJaxbPaths(elementType, path, out);
            } else {
                out.put(path, false);
                if (!isLeaf(fieldType)) collectJaxbPaths(fieldType, path, out);
            }
        }
    }

    private boolean isLeaf(Class<?> type) {
        return type == String.class || type == Boolean.class || type == boolean.class
                || LEAF_COMPLEX_TYPES.contains(type.getSimpleName());
    }

    // --- Modellsidan: StructureDefinition.snapshot.element, path minus resurs-id-prefixet,
    //     med FHIR:s egna standardelement (id/extension/modifierExtension) bortfiltrerade. ---

    private Map<String, Boolean> collectLogicalModelPaths(String resource, String resourceTypeId) throws IOException {
        StructureDefinition sd;
        try (InputStream is = getClass().getResourceAsStream(resource)) {
            if (is == null) throw new IOException("Resource not found: " + resource);
            sd = (StructureDefinition) new org.hl7.fhir.r4.formats.JsonParser().parse(is);
        }
        Map<String, Boolean> out = new LinkedHashMap<>();
        String prefix = resourceTypeId + ".";
        for (ElementDefinition el : sd.getSnapshot().getElement()) {
            String path = el.getPath();
            if (!path.startsWith(prefix)) continue;
            String rel = path.substring(prefix.length());
            String last = rel.contains(".") ? rel.substring(rel.lastIndexOf('.') + 1) : rel;
            if (last.equals("id") || last.equals("extension") || last.equals("modifierExtension")) continue;
            out.put(rel, "*".equals(el.getMax()));
        }
        return out;
    }
}
