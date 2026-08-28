package se.inera.ehds.soap.client;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.handler.MessageContext;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.inera.ehds.mapping.rivta.doclist.GetDocumentList;
import se.inera.ehds.mapping.rivta.doclist.PersonIdType;
import se.inera.ehds.mapping.rivta.doclist.GetDocumentListResponse;
import se.inera.ehds.soap.sei.GetDocumentListResponderInterface;

import java.util.HashMap;
import java.util.Map;

/**
 * CXF-based SOAP client for RIVTA GetDocumentList:1.
 *
 * Call pattern (post T1/F1-uppslag, se CatalogDiscoveryService):
 *   - endpointAddress: den fysiska adress som slagits upp via tjänstekatalogen (T1)
 *   - LogicalAddress header: target VG's HSA-id (RIVTA routing)
 *   - x-rivta-original-serviceconsumer-hsaid: bridge's own HSA-id
 *   - Authorization: Bearer-åtkomstintyg hämtat från åtkomstintygsutfärdaren
 *   - mTLS: placeholder — configure CXF HTTPConduit with SITHS keystore in production
 *   - SAML: placeholder — add WS-Security interceptor with bridge's SITHS cert in production
 */
public class GetDocumentListClient {

    private static final Logger log = LoggerFactory.getLogger(GetDocumentListClient.class);

    private static final String CONSUMER_HSA_HEADER = "x-rivta-original-serviceconsumer-hsaid";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final JaxWsProxyFactoryBean factory;
    private final String bridgeHsaId;

    public GetDocumentListClient(String bridgeHsaId) {
        this.bridgeHsaId = bridgeHsaId;
        factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(GetDocumentListResponderInterface.class);
        factory.setAddress("http://placeholder"); // overridden per call
        // TODO production: add WS-Security interceptor for SAML assertion
        // TODO production: configure mTLS on CXF HTTPConduit
    }

    /**
     * Calls GetDocumentList at the endpoint address resolved via T1 (tjänstekatalogen).
     *
     * @param endpointAddress Fysisk adress uppslagen via CatalogDiscoveryService (T1)
     * @param logicalAddress  HSA-id of target VG — kvarstår som RIVTA-routingheader
     * @param patientRoot     OID for patient ID type (e.g. 1.2.752.129.2.1.3.1)
     * @param patientValue    Patient ID value (personnummer/samordningsnummer)
     * @param accessToken     Åtkomstintyg (OAuth2 access token) från åtkomstintygsutfärdaren
     */
    public GetDocumentListResponse call(String endpointAddress, String logicalAddress,
                                        String patientRoot, String patientValue, String accessToken) {
        GetDocumentListResponderInterface port =
                (GetDocumentListResponderInterface) factory.create();

        BindingProvider bp = (BindingProvider) port;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointAddress);
        bp.getRequestContext().put("jakarta.xml.ws.client.receiveTimeout", 10000L);
        bp.getRequestContext().put("jakarta.xml.ws.client.connectionTimeout", 5000L);

        // Bridge identifies itself as the consumer, and presents its åtkomstintyg.
        Map<String, java.util.List<String>> httpHeaders = new HashMap<>();
        httpHeaders.put(CONSUMER_HSA_HEADER, java.util.List.of(bridgeHsaId));
        httpHeaders.put(AUTHORIZATION_HEADER, java.util.List.of("Bearer " + accessToken));
        bp.getRequestContext().put(MessageContext.HTTP_REQUEST_HEADERS, httpHeaders);

        // Build request
        PersonIdType patientId = new PersonIdType();
        patientId.setRoot(patientRoot.startsWith("urn:oid:") ? patientRoot.substring(8) : patientRoot);
        patientId.setExtension(patientValue);

        GetDocumentList request = new GetDocumentList();
        request.setPatientId(patientId);

        log.debug("GetDocumentList → {} (logicalAddress={})", endpointAddress, logicalAddress);
        return port.getDocumentList(logicalAddress, request);
    }
}
