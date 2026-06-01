package se.inera.ehds.mapping.rivta;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DiagnosisBody", propOrder = {
    "diagnosisCode", "diagnosisType", "diagnosisTimePeriod", "chronicCondition", "assertedDate"
})
public class DiagnosisBody {
    private CVType diagnosisCode;
    private String diagnosisType;
    private DatePeriodType diagnosisTimePeriod;
    private Boolean chronicCondition;
    private String assertedDate;

    public CVType getDiagnosisCode() { return diagnosisCode; }
    public void setDiagnosisCode(CVType diagnosisCode) { this.diagnosisCode = diagnosisCode; }
    public String getDiagnosisType() { return diagnosisType; }
    public void setDiagnosisType(String diagnosisType) { this.diagnosisType = diagnosisType; }
    public DatePeriodType getDiagnosisTimePeriod() { return diagnosisTimePeriod; }
    public void setDiagnosisTimePeriod(DatePeriodType diagnosisTimePeriod) { this.diagnosisTimePeriod = diagnosisTimePeriod; }
    public Boolean getChronicCondition() { return chronicCondition; }
    public void setChronicCondition(Boolean chronicCondition) { this.chronicCondition = chronicCondition; }
    public String getAssertedDate() { return assertedDate; }
    public void setAssertedDate(String assertedDate) { this.assertedDate = assertedDate; }
}
