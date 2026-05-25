package se.inera.ehds.fhir;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import jakarta.servlet.http.HttpServletRequest;

@Interceptor
public class TenantInterceptor {

    @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
    public void extractTenant(RequestDetails requestDetails, HttpServletRequest httpRequest) {
        String vgHsaId = httpRequest.getHeader("X-VG-HSA-ID");
        if (vgHsaId != null && !vgHsaId.isBlank()) {
            requestDetails.setAttribute("vgHsaId", vgHsaId);
        }
    }
}
