package se.inera.ehds.soap.sei;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import se.inera.ehds.mapping.rivta.doclist.GetDocumentList;
import se.inera.ehds.mapping.rivta.doclist.GetDocumentListResponse;

/**
 * JAX-WS SEI for RIVTA GetDocumentList:1.
 *
 * The LogicalAddress header is mandatory in all NTjP/RIVTA calls.
 */
@WebService(
        name = "GetDocumentListResponderInterface",
        targetNamespace = "urn:riv:clinicalprocess:healthrecord:GetDocumentListResponder:1"
)
public interface GetDocumentListResponderInterface {

    @WebMethod(operationName = "GetDocumentList")
    @WebResult(
            name = "GetDocumentListResponse",
            targetNamespace = "urn:riv:clinicalprocess:healthrecord:GetDocumentListResponder:1"
    )
    GetDocumentListResponse getDocumentList(

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
                    name = "GetDocumentList",
                    targetNamespace = "urn:riv:clinicalprocess:healthrecord:GetDocumentListResponder:1"
            )
            GetDocumentList parameters
    );
}
