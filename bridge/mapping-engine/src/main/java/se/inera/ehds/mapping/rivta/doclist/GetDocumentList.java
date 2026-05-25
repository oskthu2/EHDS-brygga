package se.inera.ehds.mapping.rivta.doclist;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import se.inera.ehds.mapping.rivta.PersonIdType;

@XmlRootElement(name = "GetDocumentList")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GetDocumentListRequestType", propOrder = {"patientId"})
public class GetDocumentList {

    private PersonIdType patientId;

    public PersonIdType getPatientId() { return patientId; }
    public void setPatientId(PersonIdType patientId) { this.patientId = patientId; }
}
