package se.inera.ehds.fhir;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.r4.model.*;
import se.inera.ehds.config.VgConfig;
import se.inera.ehds.config.VgConfigLoader;

import java.util.List;
import java.util.Set;

/**
 * Genererar ett VG-specifikt CapabilityStatement för /{vgHsaId}/fhir/metadata.
 *
 * Flöde:
 *  1. Läser vgHsaId ur request-attributet (satt av {@link TenantInterceptor}).
 *  2. Söker upp VG:ns konfiguration i vg-config.yaml.
 *     – Okänt vgHsaId → 404 ResourceNotFoundException.
 *     – Saknar vgHsaId i URL → 400 InvalidRequestException.
 *  3. Filtrerar bort resurstyper som VG:n inte stöder (HAPI auto-genererar alla
 *     registrerade providers; vi tar bara med de som finns i resources-blocket).
 *  4. Berikar kvarvarande resurser med supportedProfile och searchParam.
 *  5. Lägger till SMART-on-FHIR säkerhetsinformation och EU HDA-konformansdeklaration.
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
    private final List<VgConfig> vgConfigs;
    private final VgConfigLoader vgConfigLoader;

    public CapabilityStatementEnricher(String authBaseUrl,
                                        List<VgConfig> vgConfigs,
                                        VgConfigLoader vgConfigLoader) {
        this.authBaseUrl = authBaseUrl;
        this.vgConfigs = vgConfigs;
        this.vgConfigLoader = vgConfigLoader;
    }

    @Hook(Pointcut.SERVER_CAPABILITY_STATEMENT_GENERATED)
    public void enrich(IBaseConformance rawCs, RequestDetails requestDetails) {
        if (!(rawCs instanceof CapabilityStatement cs)) return;

        String vgHsaId = requestDetails != null
                ? (String) requestDetails.getAttribute("vgHsaId")
                : null;

        if (vgHsaId == null) {
            throw new InvalidRequestException(
                    "VG HSA-id saknas i URL:en. Anropa /{vgHsaId}/fhir/metadata.");
        }

        VgConfig vgConfig = vgConfigLoader.findByHsaId(vgConfigs, vgHsaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Okänd vårdgivare: " + vgHsaId));

        cs.addInstantiates(EU_HDA_RAP_URL);
        cs.addImplementationGuide(EU_EPS_IG_URL);
        cs.setTitle("EHDS-brygga – " + (vgConfig.getDescription() != null
                ? vgConfig.getDescription() : vgHsaId));

        if (cs.getRest().isEmpty()) return;
        CapabilityStatement.CapabilityStatementRestComponent rest = cs.getRestFirstRep();

        addSmartSecurity(rest);

        Set<String> supported = vgConfig.getResources() != null
                ? vgConfig.getResources().keySet()
                : Set.of();

        // Ta bort resurser som VG:n inte stöder (HAPI inkluderar alla registrerade providers)
        rest.getResource().removeIf(r -> !supported.contains(r.getType()));

        if (supported.contains("Condition")) {
            enrichResource(rest, "Condition", SE_EHDS_CONDITION, EU_EPS_CONDITION);
        }
        if (supported.contains("DocumentReference")) {
            enrichResource(rest, "DocumentReference", SE_EHDS_DOC_REF, null);
        }
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
        oauth.addExtension(new Extension("introspect", new UriType(authBaseUrl + "/auth/introspect")));
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
