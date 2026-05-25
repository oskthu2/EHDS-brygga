package se.inera.ehds.mapping.rivta;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlRootElement(name = "GetDiagnosis")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GetDiagnosisRequestType", propOrder = {"patientId"})
public class GetDiagnosis {
    private PersonIdType patientId;

    public PersonIdType getPatientId() { return patientId; }
    public void setPatientId(PersonIdType patientId) { this.patientId = patientId; }
}
