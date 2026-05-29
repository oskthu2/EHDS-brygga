package se.inera.ehds.proxy.config;

import ca.uhn.fhir.context.FhirContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import se.inera.ehds.mapping.concept.ConceptMapRegistry;
import se.inera.ehds.mapping.naming.NamingSystemRegistry;
import se.inera.ehds.mapping.tk.getdiagnosis.GetDiagnosisMapper;
import se.inera.ehds.mapping.tk.getdocumentlist.GetDocumentListMapper;
import se.inera.ehds.soap.client.GetDiagnosisClient;
import se.inera.ehds.soap.client.GetDocumentListClient;

@Configuration
public class ProxyBeanConfig {

    @Bean public FhirContext fhirContext() { return FhirContext.forR4Cached(); }
    @Bean public RestTemplate restTemplate() { return new RestTemplate(); }
    @Bean public NamingSystemRegistry namingSystemRegistry() { return new NamingSystemRegistry(); }
    @Bean public ConceptMapRegistry conceptMapRegistry() { return new ConceptMapRegistry(); }

    @Bean
    public GetDiagnosisMapper getDiagnosisMapper(NamingSystemRegistry n, ConceptMapRegistry c) {
        return new GetDiagnosisMapper(n, c);
    }

    @Bean
    public GetDocumentListMapper getDocumentListMapper(NamingSystemRegistry n, ConceptMapRegistry c) {
        return new GetDocumentListMapper(n, c);
    }

    @Bean
    public GetDiagnosisClient getDiagnosisClient(ProxyProperties props) {
        return new GetDiagnosisClient(props.getBridgeHsaId());
    }

    @Bean
    public GetDocumentListClient getDocumentListClient(ProxyProperties props) {
        return new GetDocumentListClient(props.getBridgeHsaId());
    }
}
