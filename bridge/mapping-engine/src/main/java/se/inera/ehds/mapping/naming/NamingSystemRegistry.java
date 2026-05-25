package se.inera.ehds.mapping.naming;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class NamingSystemRegistry {

    private final Map<String, String> oidToUri = new HashMap<>();

    public NamingSystemRegistry() {
        try (InputStream is = getClass().getResourceAsStream("/naming-systems.yaml")) {
            if (is == null) throw new IllegalStateException("naming-systems.yaml not found on classpath");
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.findAndRegisterModules();
            NamingSystemConfig config = mapper.readValue(is, NamingSystemConfig.class);
            for (NamingSystemEntry entry : config.getEntries()) {
                oidToUri.put(entry.getOid(), entry.getUri());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load naming-systems.yaml", e);
        }
    }

    /** Resolves an OID to a FHIR URI. Falls back to urn:oid:<oid> if not found. */
    public String oidToUri(String oid) {
        if (oid == null) return null;
        return oidToUri.getOrDefault(oid, "urn:oid:" + oid);
    }
}
