package se.inera.ehds.soap.sei;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import se.inera.ehds.mapping.rivta.caredocumentation.GetCareDocumentation;
import se.inera.ehds.mapping.rivta.caredocumentation.GetCareDocumentationResponse;

/**
 * JAX-WS SEI for RIVTA GetCareDocumentation:3 (clinicalprocess:healthcond:description).
 *
 * The LogicalAddress header is mandatory in all NTjP/RIVTA calls.
 */
@WebService(
        name = "GetCareDocumentationResponderInterface",
        targetNamespace = "urn:riv:clinicalprocess:healthcond:description:GetCareDocumentationResponder:3"
)
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
public interface GetCareDocumentationResponderInterface {

    @WebMethod(operationName = "GetCareDocumentation")
    @WebResult(
            name = "GetCareDocumentationResponse",
            targetNamespace = "urn:riv:clinicalprocess:healthcond:description:GetCareDocumentationResponder:3"
    )
    GetCareDocumentationResponse getCareDocumentation(

            // RIVTA routing header: target VG's logical address
            @WebParam(
                    name = "LogicalAddress",
                    targetNamespace = "urn:riv:itintegration:registry:1",
                    header = true,
                    mode = WebParam.Mode.IN
            )
            String logicalAddress,

            // Request body element
            @WebParam(
                    name = "GetCareDocumentation",
                    targetNamespace = "urn:riv:clinicalprocess:healthcond:description:GetCareDocumentationResponder:3"
            )
            GetCareDocumentation parameters
    );
}
