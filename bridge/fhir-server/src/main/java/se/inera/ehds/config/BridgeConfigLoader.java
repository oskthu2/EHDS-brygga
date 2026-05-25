package se.inera.ehds.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Configuration
public class BridgeConfigLoader {

    @Bean
    public List<ServiceContractConfig> serviceContracts() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();
        ClassPathResource resource = new ClassPathResource("config/services.yaml");
        Map<String, List<ServiceContractConfig>> raw = mapper.readValue(
                resource.getInputStream(),
                mapper.getTypeFactory().constructMapType(Map.class, String.class,
                        mapper.getTypeFactory().constructCollectionType(List.class, ServiceContractConfig.class)));
        return raw.getOrDefault("serviceContracts", List.of());
    }
}
