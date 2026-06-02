package se.inera.ehds.fhir;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.r4.model.*;

/**
 * Post-processes HAPI's auto-generated CapabilityStatement to declare:
 * - EU HDA Resource Access Provider conformance (instantiates)
 * - Supported EHDS content IGs (implementationGuide)
 * - SMART-on-FHIR security endpoints
 * - supportedProfile on Condition and DocumentReference
 * - patient.identifier as mandatory search parameter
 */
@Interceptor
public class CapabilityStatementEnricher {

    private static final String EU_HDA_RAP_URL =
            "http://hl7.eu/fhir/health-data-api/CapabilityStatement/EEHRxF-ResourceAccessProvider";
    private static final String EU_EPS_IG_URL = "http://hl7.eu/fhir/eps";
    private static final String SMART_OAUTH_EXT =
            "http://fhir-registry.smarthealthit.org/StructureDefinition/oauth-uris";
    private static final String SE_EHDS_CONDITION =
            "https://ehds-brygga.inera.se/fhir/StructureDefinition/se-ehds-condition";
    private static final String EU_EPS_CONDITION =
            "http://hl7.eu/fhir/eps/StructureDefinition/condition-obl-eu-eps";
    private static final String SE_EHDS_DOC_REF =
            "https://ehds-brygga.inera.se/fhir/StructureDefinition/se-ehds-document-reference";

    private final String authBaseUrl;

    public CapabilityStatementEnricher(String authBaseUrl) {
        this.authBaseUrl = authBaseUrl;
    }

    @Hook(Pointcut.SERVER_CAPABILITY_STATEMENT_GENERATED)
    public void enrich(IBaseConformance rawCs) {
        if (!(rawCs instanceof CapabilityStatement cs)) return;

        cs.addInstantiates(EU_HDA_RAP_URL);
        cs.addImplementationGuide(EU_EPS_IG_URL);

        if (cs.getRest().isEmpty()) return;
        CapabilityStatement.CapabilityStatementRestComponent rest = cs.getRestFirstRep();

        addSmartSecurity(rest);
        enrichResource(rest, "Condition", SE_EHDS_CONDITION, EU_EPS_CONDITION);
        enrichResource(rest, "DocumentReference", SE_EHDS_DOC_REF, null);
    }

    private void addSmartSecurity(CapabilityStatement.CapabilityStatementRestComponent rest) {
        CapabilityStatement.CapabilityStatementRestSecurityComponent sec = rest.getSecurity();
        sec.setCors(true);
        sec.addService(new CodeableConcept().addCoding(new Coding()
                .setSystem("http://terminology.hl7.org/CodeSystem/restful-security-service")
                .setCode("SMART-on-FHIR")
                .setDisplay("SMART-on-FHIR")));
        Extension oauth = new Extension(SMART_OAUTH_EXT);
        oauth.addExtension(new Extension("authorize", new UriType(authBaseUrl + "/auth/authorize")));
        oauth.addExtension(new Extension("token",     new UriType(authBaseUrl + "/auth/token")));
        oauth.addExtension(new Extension("introspect",new UriType(authBaseUrl + "/auth/introspect")));
        sec.addExtension(oauth);
    }

    private void enrichResource(CapabilityStatement.CapabilityStatementRestComponent rest,
                                 String type, String... profiles) {
        CapabilityStatement.CapabilityStatementRestResourceComponent res =
                rest.getResource().stream()
                        .filter(r -> type.equals(r.getType()))
                        .findFirst()
                        .orElseGet(() -> {
                            CapabilityStatement.CapabilityStatementRestResourceComponent r =
                                    rest.addResource().setType(type);
                            r.addInteraction().setCode(
                                    CapabilityStatement.TypeRestfulInteraction.SEARCHTYPE);
                            return r;
                        });

        for (String profile : profiles) {
            if (profile != null) res.addSupportedProfile(profile);
        }

        boolean hasPatientIdentifier = res.getSearchParam().stream()
                .anyMatch(sp -> "patient.identifier".equals(sp.getName()));
        if (!hasPatientIdentifier) {
            res.addSearchParam()
                    .setName("patient.identifier")
                    .setType(Enumerations.SearchParamType.TOKEN)
                    .setDocumentation("Personnummer eller samordningsnummer (system|value) – obligatorisk");
        }
    }
}
