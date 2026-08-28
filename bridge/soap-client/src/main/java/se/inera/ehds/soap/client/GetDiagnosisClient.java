package se.inera.ehds.soap.client;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.handler.MessageContext;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.inera.ehds.mapping.rivta.GetDiagnosis;
import se.inera.ehds.mapping.rivta.GetDiagnosisResponse;
import se.inera.ehds.mapping.rivta.PersonIdType;
import se.inera.ehds.soap.sei.GetDiagnosisResponderInterface;

import java.util.HashMap;
import java.util.Map;

/**
 * CXF-based SOAP client for RIVTA GetDiagnosis:2.
 *
 * Call pattern (post T1/F1-uppslag, se CatalogDiscoveryService):
 *   - endpointAddress: den fysiska adress som slagits upp via tjänstekatalogen (T1)
 *   - LogicalAddress header: target VG's HSA-id (RIVTA routing)
 *   - x-rivta-original-serviceconsumer-hsaid: bridge's own HSA-id
 *   - Authorization: Bearer-åtkomstintyg hämtat från åtkomstintygsutfärdaren
 *   - mTLS: placeholder — configure CXF HTTPConduit with SITHS keystore in production
 *   - SAML: placeholder — add WS-Security interceptor with bridge's SITHS cert in production
 */
public class GetDiagnosisClient {

    private static final Logger log = LoggerFactory.getLogger(GetDiagnosisClient.class);

    private static final String CONSUMER_HSA_HEADER = "x-rivta-original-serviceconsumer-hsaid";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String NS_RIV = "urn:riv:clinicalprocess:activity:conditions:GetDiagnosisResponder:2";

    private final JaxWsProxyFactoryBean factory;
    private final String bridgeHsaId;

    public GetDiagnosisClient(String bridgeHsaId) {
        this.bridgeHsaId = bridgeHsaId;
        factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(GetDiagnosisResponderInterface.class);
        factory.setAddress("http://placeholder"); // overridden per call
        // TODO production: add WS-Security interceptor for SAML assertion
        // factory.getOutInterceptors().add(new SamlOutInterceptor(siths));
        // TODO production: configure mTLS on CXF HTTPConduit
    }

    /**
     * Calls GetDiagnosis at the endpoint address resolved via T1 (tjänstekatalogen).
     *
     * @param endpointAddress Fysisk adress uppslagen via CatalogDiscoveryService (T1)
     * @param logicalAddress  HSA-id of target VG — kvarstår som RIVTA-routingheader
     * @param patientRoot     OID for patient ID type (e.g. 1.2.752.129.2.1.3.1)
     * @param patientValue    Patient ID value (personnummer/samordningsnummer)
     * @param accessToken     Åtkomstintyg (OAuth2 access token) från åtkomstintygsutfärdaren
     */
    public GetDiagnosisResponse call(String endpointAddress, String logicalAddress,
                                      String patientRoot, String patientValue, String accessToken) {
        GetDiagnosisResponderInterface port = (GetDiagnosisResponderInterface) factory.create();

        BindingProvider bp = (BindingProvider) port;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointAddress);
        bp.getRequestContext().put("jakarta.xml.ws.client.receiveTimeout", 10000L);
        bp.getRequestContext().put("jakarta.xml.ws.client.connectionTimeout", 5000L);

        // Bridge identifies itself as the consumer, and presents its åtkomstintyg.
        // CXF requires HTTP_REQUEST_HEADERS (Map<String, List<String>>) for custom HTTP headers.
        Map<String, java.util.List<String>> httpHeaders = new HashMap<>();
        httpHeaders.put(CONSUMER_HSA_HEADER, java.util.List.of(bridgeHsaId));
        httpHeaders.put(AUTHORIZATION_HEADER, java.util.List.of("Bearer " + accessToken));
        bp.getRequestContext().put(MessageContext.HTTP_REQUEST_HEADERS, httpHeaders);

        // Build request
        PersonIdType patientId = new PersonIdType();
        // Strip urn:oid: prefix if present — RIVTA root is the raw OID
        patientId.setRoot(patientRoot.startsWith("urn:oid:") ? patientRoot.substring(8) : patientRoot);
        patientId.setExtension(patientValue);

        GetDiagnosis request = new GetDiagnosis();
        request.setPatientId(patientId);

        log.debug("GetDiagnosis → {} (logicalAddress={})", endpointAddress, logicalAddress);
        return port.getDiagnosis(logicalAddress, request);
    }
}
