package se.inera.ehds.mapping.rivta;

import jakarta.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "GetDiagnosisResponse")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GetDiagnosisResponseType", propOrder = {"diagnosis", "result"})
public class GetDiagnosisResponse {

    @XmlElement(name = "diagnosis")
    private List<Diagnosis> diagnosis = new ArrayList<>();

    private ResultType result;

    public ResultType getResult() { return result; }
    public void setResult(ResultType result) { this.result = result; }
    public List<Diagnosis> getDiagnosis() { return diagnosis; }
    public void setDiagnosis(List<Diagnosis> diagnosis) { this.diagnosis = diagnosis; }
}
