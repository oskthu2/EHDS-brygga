package se.inera.ehds.fhir;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;
import se.inera.ehds.config.AppProperties;
import se.inera.ehds.config.VgConfig;
import se.inera.ehds.config.VgConfigLoader;
import se.inera.ehds.fhir.provider.ConditionResourceProvider;
import se.inera.ehds.fhir.provider.DocumentReferenceResourceProvider;

import java.util.List;

@Configuration
@EnableAsync
public class HapiConfig {

    @Bean
    public TenantInterceptor tenantInterceptor() {
        return new TenantInterceptor();
    }

    @Bean
    public CapabilityStatementEnricher capabilityStatementEnricher(AppProperties props,
                                                                    List<VgConfig> vgConfigs,
                                                                    VgConfigLoader vgConfigLoader) {
        return new CapabilityStatementEnricher(props.getAuthBaseUrl(), vgConfigs, vgConfigLoader);
    }

    @Bean
    public EhdsFhirServer fhirServer(ConditionResourceProvider conditionProvider,
                                     DocumentReferenceResourceProvider documentReferenceProvider,
                                     TenantInterceptor tenantInterceptor,
                                     CapabilityStatementEnricher csEnricher) {
        return new EhdsFhirServer(conditionProvider, documentReferenceProvider,
                tenantInterceptor, csEnricher);
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
