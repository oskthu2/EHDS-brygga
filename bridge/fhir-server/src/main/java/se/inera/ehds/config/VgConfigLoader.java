package se.inera.ehds.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Configuration
public class VgConfigLoader {

    @Bean
    public List<VgConfig> vgConfigs() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();
        ClassPathResource resource = new ClassPathResource("config/vg-config.yaml");
        Map<String, List<VgConfig>> raw = mapper.readValue(
                resource.getInputStream(),
                new TypeReference<>() {});
        return raw.getOrDefault("vgConfigs", List.of());
    }

    public Optional<VgConfig> findByHsaId(List<VgConfig> configs, String hsaId) {
        return configs.stream()
                .filter(v -> hsaId.equals(v.getVgHsaId()))
                .findFirst();
    }
}
