package se.inera.ehds.mapping.rivta;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LegalAuthenticatorType", propOrder = {"hcProfessional", "signatureDate"})
public class LegalAuthenticatorType {
    @XmlElement(namespace = CoreNamespace.VALUE)
    private HealthcareProfessionalType hcProfessional;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String signatureDate;

    public HealthcareProfessionalType getHcProfessional() { return hcProfessional; }
    public void setHcProfessional(HealthcareProfessionalType hcProfessional) { this.hcProfessional = hcProfessional; }
    public String getSignatureDate() { return signatureDate; }
    public void setSignatureDate(String signatureDate) { this.signatureDate = signatureDate; }
}
