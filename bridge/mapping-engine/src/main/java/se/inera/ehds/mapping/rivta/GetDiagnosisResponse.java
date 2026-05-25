package se.inera.ehds.mapping.rivta;

import jakarta.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "GetDiagnosisResponse")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GetDiagnosisResponseType", propOrder = {"result", "diagnosis"})
public class GetDiagnosisResponse {
    private ResultType result;

    @XmlElement(name = "diagnosis")
    private List<Diagnosis> diagnosis = new ArrayList<>();

    public ResultType getResult() { return result; }
    public void setResult(ResultType result) { this.result = result; }
    public List<Diagnosis> getDiagnosis() { return diagnosis; }
    public void setDiagnosis(List<Diagnosis> diagnosis) { this.diagnosis = diagnosis; }
}
