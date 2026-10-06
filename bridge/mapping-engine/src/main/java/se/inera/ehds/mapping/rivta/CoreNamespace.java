package se.inera.ehds.mapping.rivta;

/**
 * GetDiagnosisResponder:2 wraps a separate "core components" schema — the same pattern
 * confirmed for GetCareDocumentationResponder:3: the Responder schema only declares the
 * GetDiagnosis/GetDiagnosisResponse wrapper elements; everything nested below
 * (diagnosisHeader, diagnosisBody, result, ...) is declared in the domain's own
 * core-components schema and therefore qualified with that schema's namespace, not the
 * Responder namespace.
 *
 * Verified against the real, published XSDs from the Bitbucket repository
 * rivta-domains/riv.clinicalprocess.healthcond.description (schemas/core_components/
 * clinicalprocess_healthcond_description_2.1.xsd + schemas/interactions/
 * GetDiagnosisInteraction/GetDiagnosisResponder_2.0.xsd). GetDiagnosis:2 lives in the
 * same RIVTA domain repo as GetCareDocumentation (healthcond.description), not in
 * clinicalprocess:activity:conditions as an earlier, unverified version of this file
 * guessed by analogy.
 */
final class CoreNamespace {
    static final String VALUE = "urn:riv:clinicalprocess:healthcond:description:2";

    private CoreNamespace() {
    }
}
