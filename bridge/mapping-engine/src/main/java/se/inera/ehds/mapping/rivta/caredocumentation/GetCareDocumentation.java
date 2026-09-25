package se.inera.ehds.mapping.rivta.caredocumentation;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Request body. The mapping spec supplied for GetCareDocumentation:3 documents only the
 * response shape (careDocumentation.header/body) — no request-side fields (e.g. a date range
 * or paging cursor matching hasMore) were specified. Modelled minimally as patientId only,
 * matching the request shape of every other TK this bridge calls; revisit against the real
 * WSDL/XSD if GetCareDocumentation's request carries additional parameters.
 */
@XmlRootElement(name = "GetCareDocumentation")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GetCareDocumentationRequestType", propOrder = {"patientId"})
public class GetCareDocumentation {

    private PersonIdType patientId;

    public PersonIdType getPatientId() { return patientId; }
    public void setPatientId(PersonIdType patientId) { this.patientId = patientId; }
}
