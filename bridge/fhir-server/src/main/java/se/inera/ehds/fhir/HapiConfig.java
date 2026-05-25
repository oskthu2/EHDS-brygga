package se.inera.ehds.fhir;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;
import se.inera.ehds.fhir.provider.ConditionResourceProvider;

@Configuration
@EnableAsync
public class HapiConfig {

    @Bean
    public EhdsFhirServer fhirServer(ConditionResourceProvider conditionProvider) {
        return new EhdsFhirServer(conditionProvider);
    }

    @Bean
    public ServletRegistrationBean<EhdsFhirServer> fhirServlet(EhdsFhirServer server) {
        ServletRegistrationBean<EhdsFhirServer> reg =
                new ServletRegistrationBean<>(server, "/*");
        reg.setName("fhirServlet");
        reg.setLoadOnStartup(1);
        return reg;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
