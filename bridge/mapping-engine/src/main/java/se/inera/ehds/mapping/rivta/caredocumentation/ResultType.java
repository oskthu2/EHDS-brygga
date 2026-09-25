package se.inera.ehds.mapping.rivta.caredocumentation;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * GetCareDocumentation v3.0 uses resultText for technical error detail — unlike the
 * PatientSummaryHeader-based contracts (GetDiagnosis, old GetDocumentList) which carry
 * message + logId. There is no logId field here.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ResultType", propOrder = {"resultCode", "resultText"})
public class ResultType {
    private String resultCode;
    private String resultText;

    public String getResultCode() { return resultCode; }
    public void setResultCode(String resultCode) { this.resultCode = resultCode; }
    public String getResultText() { return resultText; }
    public void setResultText(String resultText) { this.resultText = resultText; }
}
