package se.inera.ehds.soap.sei;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import se.inera.ehds.mapping.rivta.GetDiagnosis;
import se.inera.ehds.mapping.rivta.GetDiagnosisResponse;

/**
 * JAX-WS SEI for RIVTA GetDiagnosis:2.
 *
 * The LogicalAddress header is mandatory in all NTjP/RIVTA calls.
 * The bridgeHsaId header (x-rivta-original-serviceconsumer-hsaid) identifies
 * the bridge as the consumer towards NTjP — NOT the end user.
 */
@WebService(
        name = "GetDiagnosisResponderInterface",
        targetNamespace = "urn:riv:clinicalprocess:activity:conditions:GetDiagnosisResponder:2"
)
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
public interface GetDiagnosisResponderInterface {

    @WebMethod(operationName = "GetDiagnosis")
    @WebResult(
            name = "GetDiagnosisResponse",
            targetNamespace = "urn:riv:clinicalprocess:activity:conditions:GetDiagnosisResponder:2"
    )
    GetDiagnosisResponse getDiagnosis(

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
                    name = "GetDiagnosis",
                    targetNamespace = "urn:riv:clinicalprocess:activity:conditions:GetDiagnosisResponder:2"
            )
            GetDiagnosis parameters
    );
}
