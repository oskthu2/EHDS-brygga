package se.inera.ehds.mapping.rivta;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ResultType", propOrder = {"resultCode", "errorCode", "logId", "subCode", "message"})
public class ResultType {
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String resultCode;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String errorCode;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String logId;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String subCode;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String message;

    public String getResultCode() { return resultCode; }
    public void setResultCode(String resultCode) { this.resultCode = resultCode; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getLogId() { return logId; }
    public void setLogId(String logId) { this.logId = logId; }
    public String getSubCode() { return subCode; }
    public void setSubCode(String subCode) { this.subCode = subCode; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
