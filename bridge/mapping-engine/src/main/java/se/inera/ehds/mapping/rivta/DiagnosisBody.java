package se.inera.ehds.mapping.rivta;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DiagnosisBody", propOrder = {
    "diagnosisCode", "diagnosisType", "diagnosisTimePeriod", "chronicCondition", "relatedDiagnosis"
})
public class DiagnosisBody {
    @XmlElement(namespace = CoreNamespace.VALUE)
    private CVType diagnosisCode;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String diagnosisType;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private DatePeriodType diagnosisTimePeriod;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private Boolean chronicCondition;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private RelatedDiagnosis relatedDiagnosis;

    public CVType getDiagnosisCode() { return diagnosisCode; }
    public void setDiagnosisCode(CVType diagnosisCode) { this.diagnosisCode = diagnosisCode; }
    public String getDiagnosisType() { return diagnosisType; }
    public void setDiagnosisType(String diagnosisType) { this.diagnosisType = diagnosisType; }
    public DatePeriodType getDiagnosisTimePeriod() { return diagnosisTimePeriod; }
    public void setDiagnosisTimePeriod(DatePeriodType diagnosisTimePeriod) { this.diagnosisTimePeriod = diagnosisTimePeriod; }
    public Boolean getChronicCondition() { return chronicCondition; }
    public void setChronicCondition(Boolean chronicCondition) { this.chronicCondition = chronicCondition; }
    public RelatedDiagnosis getRelatedDiagnosis() { return relatedDiagnosis; }
    public void setRelatedDiagnosis(RelatedDiagnosis relatedDiagnosis) { this.relatedDiagnosis = relatedDiagnosis; }
}
