package se.inera.ehds.mapping.rivta;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;

/**
 * DiagnosisBodyType (core_components/clinicalprocess_healthcond_description_2.1.xsd).
 * {@code typeOfDiagnosis} is restricted to the literal (Swedish) enum values
 * "Huvuddiagnos"/"Bidiagnos" — NOT "HD"/"BY". {@code diagnosisTime} is a single
 * timestamp; the real schema has no period/end-date concept for GetDiagnosis.
 * {@code relatedDiagnosis} is 0..unbounded.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DiagnosisBody", propOrder = {
    "typeOfDiagnosis", "chronicDiagnosis", "diagnosisTime", "diagnosisCode", "relatedDiagnosis"
})
public class DiagnosisBody {
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String typeOfDiagnosis;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private Boolean chronicDiagnosis;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String diagnosisTime;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private CVType diagnosisCode;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private List<RelatedDiagnosis> relatedDiagnosis = new ArrayList<>();

    public String getTypeOfDiagnosis() { return typeOfDiagnosis; }
    public void setTypeOfDiagnosis(String typeOfDiagnosis) { this.typeOfDiagnosis = typeOfDiagnosis; }
    public Boolean getChronicDiagnosis() { return chronicDiagnosis; }
    public void setChronicDiagnosis(Boolean chronicDiagnosis) { this.chronicDiagnosis = chronicDiagnosis; }
    public String getDiagnosisTime() { return diagnosisTime; }
    public void setDiagnosisTime(String diagnosisTime) { this.diagnosisTime = diagnosisTime; }
    public CVType getDiagnosisCode() { return diagnosisCode; }
    public void setDiagnosisCode(CVType diagnosisCode) { this.diagnosisCode = diagnosisCode; }
    public List<RelatedDiagnosis> getRelatedDiagnosis() { return relatedDiagnosis; }
    public void setRelatedDiagnosis(List<RelatedDiagnosis> relatedDiagnosis) { this.relatedDiagnosis = relatedDiagnosis; }
}
