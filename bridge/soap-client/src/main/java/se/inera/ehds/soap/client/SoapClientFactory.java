package se.inera.ehds.soap.client;

/**
 * Factory that creates pre-configured SOAP clients.
 * Extend here to add mTLS conduit configuration for NTjP production use.
 */
public class SoapClientFactory {

    public static GetDiagnosisClient createGetDiagnosisClient(String bridgeHsaId) {
        return new GetDiagnosisClient(bridgeHsaId);
    }
}
