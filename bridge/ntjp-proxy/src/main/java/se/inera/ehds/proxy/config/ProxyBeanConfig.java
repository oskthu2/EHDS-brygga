package se.inera.ehds.proxy.config;

import ca.uhn.fhir.context.FhirContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

import se.inera.ehds.mapping.concept.ConceptMapRegistry;
import se.inera.ehds.mapping.naming.NamingSystemRegistry;
import se.inera.ehds.mapping.tk.getdiagnosis.GetDiagnosisMapper;
import se.inera.ehds.mapping.tk.getcaredocumentation.GetCareDocumentationMapper;
import se.inera.ehds.soap.client.GetDiagnosisClient;
import se.inera.ehds.soap.client.GetCareDocumentationClient;

@Configuration
@EnableAsync
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
    public GetCareDocumentationMapper getCareDocumentationMapper(NamingSystemRegistry n) {
        return new GetCareDocumentationMapper(n);
    }

    @Bean
    public GetDiagnosisClient getDiagnosisClient(ProxyProperties props) {
        return new GetDiagnosisClient(props.getBridgeHsaId());
    }

    @Bean
    public GetCareDocumentationClient getCareDocumentationClient(ProxyProperties props) {
        return new GetCareDocumentationClient(props.getBridgeHsaId());
    }
}
