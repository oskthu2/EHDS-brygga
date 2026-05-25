package se.inera.ehds.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.inera.ehds.mapping.concept.ConceptMapRegistry;
import se.inera.ehds.mapping.naming.NamingSystemRegistry;
import se.inera.ehds.mapping.tk.getdiagnosis.GetDiagnosisMapper;
import se.inera.ehds.soap.client.GetDiagnosisClient;

@Configuration
public class MappingEngineConfig {

    @Bean
    public NamingSystemRegistry namingSystemRegistry() {
        return new NamingSystemRegistry();
    }

    @Bean
    public ConceptMapRegistry conceptMapRegistry() {
        return new ConceptMapRegistry();
    }

    @Bean
    public GetDiagnosisMapper getDiagnosisMapper(NamingSystemRegistry naming,
                                                   ConceptMapRegistry concepts) {
        return new GetDiagnosisMapper(naming, concepts);
    }

    @Bean
    public GetDiagnosisClient getDiagnosisClient(AppProperties props) {
        return new GetDiagnosisClient(props.getBridgeHsaId());
    }
}
