package se.inera.ehds.mapping.rivta;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DiagnosisType", propOrder = {"diagnosisHeader", "diagnosisBody"})
public class Diagnosis {
    private DiagnosisHeader diagnosisHeader;
    private DiagnosisBody diagnosisBody;

    public DiagnosisHeader getDiagnosisHeader() { return diagnosisHeader; }
    public void setDiagnosisHeader(DiagnosisHeader diagnosisHeader) { this.diagnosisHeader = diagnosisHeader; }
    public DiagnosisBody getDiagnosisBody() { return diagnosisBody; }
    public void setDiagnosisBody(DiagnosisBody diagnosisBody) { this.diagnosisBody = diagnosisBody; }
}
