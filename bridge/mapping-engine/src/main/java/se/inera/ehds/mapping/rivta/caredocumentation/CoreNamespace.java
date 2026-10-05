package se.inera.ehds.mapping.rivta.caredocumentation;

/**
 * GetCareDocumentationResponder:3 wraps a separate "core components" schema
 * (clinicalprocess_healthcond_description_3.0.xsd) that holds every nested type
 * (header, body, result, IIType, CVType, ...). Elements declared inside those
 * core-schema complex types are qualified with this namespace, not the
 * Responder namespace from package-info.java. Only the Responder-declared
 * wrapper types (GetCareDocumentation, GetCareDocumentationResponse, HasMore)
 * stay in the package-default namespace.
 */
final class CoreNamespace {
    static final String VALUE = "urn:riv:clinicalprocess:healthcond:description:3";

    private CoreNamespace() {
    }
}
