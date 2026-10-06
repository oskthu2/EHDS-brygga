package se.inera.ehds.mapping.rivta;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DiagnosisType", propOrder = {"diagnosisHeader", "diagnosisBody"})
public class Diagnosis {
    @XmlElement(namespace = CoreNamespace.VALUE)
    private DiagnosisHeader diagnosisHeader;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private DiagnosisBody diagnosisBody;

    public DiagnosisHeader getDiagnosisHeader() { return diagnosisHeader; }
    public void setDiagnosisHeader(DiagnosisHeader diagnosisHeader) { this.diagnosisHeader = diagnosisHeader; }
    public DiagnosisBody getDiagnosisBody() { return diagnosisBody; }
    public void setDiagnosisBody(DiagnosisBody diagnosisBody) { this.diagnosisBody = diagnosisBody; }
}
