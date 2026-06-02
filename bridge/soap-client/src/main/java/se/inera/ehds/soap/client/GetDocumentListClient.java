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

import java.util.Collections;

/**
 * CXF-based SOAP client for RIVTA GetDocumentList:1.
 *
 * NTjP call pattern:
 *   - LogicalAddress header: target VG's HSA-id (RIVTA routing)
 *   - x-rivta-original-serviceconsumer-hsaid: bridge's own HSA-id
 *   - mTLS: placeholder — configure CXF HTTPConduit with SITHS keystore in production
 *   - SAML: placeholder — add WS-Security interceptor with bridge's SITHS cert in production
 */
public class GetDocumentListClient {

    private static final Logger log = LoggerFactory.getLogger(GetDocumentListClient.class);

    private static final String CONSUMER_HSA_HEADER = "x-rivta-original-serviceconsumer-hsaid";

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
     * Calls GetDocumentList at the given physical endpoint for the given logical address.
     *
     * @param physicalUrl    Physical URL from TAK (e.g. http://mock-backend:4005/soap)
     * @param logicalAddress Logical HSA-id of target VG (for RIVTA LogicalAddress header)
     * @param patientRoot    OID for patient ID type (e.g. 1.2.752.129.2.1.3.1)
     * @param patientValue   Patient ID value (personnummer/samordningsnummer)
     */
    public GetDocumentListResponse call(String physicalUrl, String logicalAddress,
                                        String patientRoot, String patientValue) {
        GetDocumentListResponderInterface port =
                (GetDocumentListResponderInterface) factory.create();

        // Override endpoint URL per call (different VG → different physical URL from TAK)
        BindingProvider bp = (BindingProvider) port;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, physicalUrl);
        bp.getRequestContext().put("jakarta.xml.ws.client.receiveTimeout", 10000L);
        bp.getRequestContext().put("jakarta.xml.ws.client.connectionTimeout", 5000L);

        // Bridge identifies itself as the consumer towards NTjP.
        bp.getRequestContext().put(MessageContext.HTTP_REQUEST_HEADERS,
                java.util.Map.of(CONSUMER_HSA_HEADER, Collections.singletonList(bridgeHsaId)));

        // Build request
        PersonIdType patientId = new PersonIdType();
        patientId.setRoot(patientRoot.startsWith("urn:oid:") ? patientRoot.substring(8) : patientRoot);
        patientId.setExtension(patientValue);

        GetDocumentList request = new GetDocumentList();
        request.setPatientId(patientId);

        log.debug("GetDocumentList → {} (logicalAddress={})", physicalUrl, logicalAddress);
        return port.getDocumentList(logicalAddress, request);
    }
}
