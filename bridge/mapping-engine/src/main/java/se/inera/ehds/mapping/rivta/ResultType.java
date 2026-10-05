package se.inera.ehds.mapping.rivta;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ResultType", propOrder = {"resultCode", "resultText", "logId"})
public class ResultType {
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String resultCode;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String resultText;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String logId;

    public String getResultCode() { return resultCode; }
    public void setResultCode(String resultCode) { this.resultCode = resultCode; }
    public String getResultText() { return resultText; }
    public void setResultText(String resultText) { this.resultText = resultText; }
    public String getLogId() { return logId; }
    public void setLogId(String logId) { this.logId = logId; }
}
