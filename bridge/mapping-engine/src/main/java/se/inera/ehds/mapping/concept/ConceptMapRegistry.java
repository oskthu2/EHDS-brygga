package se.inera.ehds.mapping.concept;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ConceptMapRegistry {

    private final Map<String, ConceptMapEntry> diagnosisTypeMap = new HashMap<>();

    public ConceptMapRegistry() {
        try (InputStream is = getClass().getResourceAsStream("/concept-maps/diagnosis-type.yaml")) {
            if (is == null) throw new IllegalStateException("concept-maps/diagnosis-type.yaml not found");
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.findAndRegisterModules();
            ConceptMapConfig config = mapper.readValue(is, ConceptMapConfig.class);
            for (ConceptMapEntry entry : config.getEntries()) {
                diagnosisTypeMap.put(entry.getSourceCode(), entry);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load diagnosis-type.yaml", e);
        }
    }

    public Optional<ConceptMapEntry> translateDiagnosisType(String sourceCode) {
        return Optional.ofNullable(diagnosisTypeMap.get(sourceCode));
    }
}
