package se.inera.ehds.mapping.rivta;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LegalAuthenticatorType", propOrder = {
    "signatureTime", "legalAuthenticatorHSAId", "legalAuthenticatorName"
})
public class LegalAuthenticatorType {
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String signatureTime;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String legalAuthenticatorHSAId;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String legalAuthenticatorName;

    public String getSignatureTime() { return signatureTime; }
    public void setSignatureTime(String signatureTime) { this.signatureTime = signatureTime; }
    public String getLegalAuthenticatorHSAId() { return legalAuthenticatorHSAId; }
    public void setLegalAuthenticatorHSAId(String legalAuthenticatorHSAId) { this.legalAuthenticatorHSAId = legalAuthenticatorHSAId; }
    public String getLegalAuthenticatorName() { return legalAuthenticatorName; }
    public void setLegalAuthenticatorName(String legalAuthenticatorName) { this.legalAuthenticatorName = legalAuthenticatorName; }
}
