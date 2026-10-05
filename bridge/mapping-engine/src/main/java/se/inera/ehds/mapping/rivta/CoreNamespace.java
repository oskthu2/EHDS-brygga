package se.inera.ehds.mapping.rivta;

/**
 * GetDiagnosisResponder:2 is expected to wrap a separate "core components" schema —
 * the same pattern confirmed against the real, published XSD for
 * GetCareDocumentationResponder:3 (urn:riv:clinicalprocess:healthcond:description:3):
 * the Responder schema only declares the GetDiagnosis/GetDiagnosisResponse wrapper
 * elements; everything nested below (diagnosisHeader, diagnosisBody, result, ...) is
 * declared in the domain's own core-components schema and therefore qualified with
 * that schema's namespace, not the Responder namespace.
 *
 * Unlike GetCareDocumentationResponder:3, the official
 * clinicalprocess:activity:conditions:2 core-components XSD could not be located in
 * this session (network access to Inera's tjänstekontrakt-katalog, mvnrepository.com
 * and GitHub cross-repo code search are all blocked here) — see the PR description.
 * This value is inferred from the domain + major-version naming convention observed
 * on the confirmed GetCareDocumentationResponder:3 schema
 * ("urn:riv:clinicalprocess:<domain>:<version>", dropping ":GetXResponder:N"), not
 * verified against the real XSD. Verify against the official package once available.
 */
final class CoreNamespace {
    static final String VALUE = "urn:riv:clinicalprocess:activity:conditions:2";

    private CoreNamespace() {
    }
}
