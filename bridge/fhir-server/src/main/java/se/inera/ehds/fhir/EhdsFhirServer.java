package se.inera.ehds.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.CorsInterceptor;
import jakarta.servlet.ServletException;
import org.springframework.web.cors.CorsConfiguration;
import se.inera.ehds.fhir.provider.ConditionResourceProvider;
import se.inera.ehds.fhir.provider.DocumentReferenceResourceProvider;

/**
 * HAPI FHIR RestfulServer registered at /* in Spring Boot.
 * nginx strips /fhir/ prefix before forwarding, so HAPI receives /Condition, /metadata etc.
 */
public class EhdsFhirServer extends RestfulServer {

    private final ConditionResourceProvider conditionProvider;
    private final DocumentReferenceResourceProvider documentReferenceProvider;
    private final TenantInterceptor tenantInterceptor;
    private final CapabilityStatementEnricher csEnricher;

    public EhdsFhirServer(ConditionResourceProvider conditionProvider,
                          DocumentReferenceResourceProvider documentReferenceProvider,
                          TenantInterceptor tenantInterceptor,
                          CapabilityStatementEnricher csEnricher) {
        super(FhirContext.forR4Cached());
        this.conditionProvider = conditionProvider;
        this.documentReferenceProvider = documentReferenceProvider;
        this.tenantInterceptor = tenantInterceptor;
        this.csEnricher = csEnricher;
    }

    @Override
    protected void initialize() throws ServletException {
        registerProvider(conditionProvider);
        registerProvider(documentReferenceProvider);
        setDefaultPrettyPrint(true);

        // Tenant interceptor: extracts X-VG-HSA-ID header into RequestDetails
        registerInterceptor(tenantInterceptor);
        // CapabilityStatement enricher: adds EU HDA/EPS conformance, SMART security, profiles
        registerInterceptor(csEnricher);

        CorsConfiguration cors = new CorsConfiguration();
        cors.addAllowedHeader("*");
        cors.addAllowedOrigin("*");
        cors.addAllowedMethod("*");
        cors.addExposedHeader("Location");
        cors.addExposedHeader("Content-Location");
        registerInterceptor(new CorsInterceptor(cors));
    }
}
