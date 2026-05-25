package se.inera.ehds.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.inera.ehds.mapping.concept.ConceptMapRegistry;
import se.inera.ehds.mapping.naming.NamingSystemRegistry;
import se.inera.ehds.mapping.tk.getdiagnosis.GetDiagnosisMapper;
import se.inera.ehds.mapping.tk.getdocumentlist.GetDocumentListMapper;
import se.inera.ehds.soap.client.GetDiagnosisClient;
import se.inera.ehds.soap.client.GetDocumentListClient;

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

    @Bean
    public GetDocumentListMapper getDocumentListMapper(NamingSystemRegistry naming,
                                                        ConceptMapRegistry concepts) {
        return new GetDocumentListMapper(naming, concepts);
    }

    @Bean
    public GetDocumentListClient getDocumentListClient(AppProperties props) {
        return new GetDocumentListClient(props.getBridgeHsaId());
    }
}
