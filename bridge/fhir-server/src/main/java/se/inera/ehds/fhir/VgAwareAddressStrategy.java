package se.inera.ehds.fhir;

import ca.uhn.fhir.rest.server.IServerAddressStrategy;
import ca.uhn.fhir.rest.server.IncomingRequestAddressStrategy;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Härleder FHIR-serverns bas-URL ur inkommande sökväg.
 *
 * Stödjer URL-mönstret /{vgHsaId}/fhir/** — returnerar
 * {scheme}://{host}[:{port}]/{vgHsaId}/fhir som bas-URL, vilket gör att
 * HAPI FHIR kan strippa prefixet och routa /Condition, /metadata etc. korrekt.
 *
 * Utan denna strategi skulle HAPI försöka tolka "/{vgHsaId}/fhir/Condition"
 * som en okänd resurstyp och returnera 404.
 */
public class VgAwareAddressStrategy implements IServerAddressStrategy {

    private static final Pattern VG_FHIR_BASE = Pattern.compile("^(/[^/]+/fhir)(?:/|$|\\?)");
    private final IncomingRequestAddressStrategy fallback = new IncomingRequestAddressStrategy();

    @Override
    public String determineServerBase(ServletContext servletContext, HttpServletRequest request) {
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty()) {
            uri = uri.substring(ctx.length());
        }
        Matcher m = VG_FHIR_BASE.matcher(uri);
        if (m.find()) {
            int port = request.getServerPort();
            String portPart = (port == 80 || port == 443) ? "" : ":" + port;
            return request.getScheme() + "://" + request.getServerName() + portPart + m.group(1);
        }
        return fallback.determineServerBase(servletContext, request);
    }
}
