package se.inera.ehds.fhir;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extraherar vgHsaId ur URL-sökvägen och lagrar det som request-attribut.
 *
 * Förväntar sig URL-mönstret /{vgHsaId}/fhir/** — samma mönster som
 * {@link VgAwareAddressStrategy} använder för bas-URL-beräkning.
 */
@Interceptor
public class TenantInterceptor {

    private static final Pattern VG_FHIR_PATTERN = Pattern.compile("^/([^/]+)/fhir(?:/|$)");

    @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
    public void extractTenant(RequestDetails requestDetails) {
        if (!(requestDetails instanceof ServletRequestDetails srd)) return;
        String uri = srd.getServletRequest().getRequestURI();
        String ctx = srd.getServletRequest().getContextPath();
        if (ctx != null && !ctx.isEmpty()) {
            uri = uri.substring(ctx.length());
        }
        Matcher m = VG_FHIR_PATTERN.matcher(uri);
        if (m.find()) {
            requestDetails.setAttribute("vgHsaId", m.group(1));
        }
    }
}
