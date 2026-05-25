package se.inera.ehds.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.CorsInterceptor;
import jakarta.servlet.ServletException;
import org.springframework.web.cors.CorsConfiguration;
import se.inera.ehds.fhir.provider.ConditionResourceProvider;

/**
 * HAPI FHIR RestfulServer registered at /* in Spring Boot.
 * nginx strips /fhir/ prefix before forwarding, so HAPI receives /Condition, /metadata etc.
 */
public class EhdsFhirServer extends RestfulServer {

    private final ConditionResourceProvider conditionProvider;

    public EhdsFhirServer(ConditionResourceProvider conditionProvider) {
        super(FhirContext.forR4Cached());
        this.conditionProvider = conditionProvider;
    }

    @Override
    protected void initialize() throws ServletException {
        registerProvider(conditionProvider);
        setDefaultPrettyPrint(true);

        CorsConfiguration cors = new CorsConfiguration();
        cors.addAllowedHeader("*");
        cors.addAllowedOrigin("*");
        cors.addAllowedMethod("*");
        cors.addExposedHeader("Location");
        cors.addExposedHeader("Content-Location");
        registerInterceptor(new CorsInterceptor(cors));
    }
}
