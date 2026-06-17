package se.inera.ehds.mapping.rivta;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LegalAuthenticatorType", propOrder = {"hcProfessional", "signatureDate"})
public class LegalAuthenticatorType {
    private HealthcareProfessionalType hcProfessional;
    private String signatureDate;

    public HealthcareProfessionalType getHcProfessional() { return hcProfessional; }
    public void setHcProfessional(HealthcareProfessionalType hcProfessional) { this.hcProfessional = hcProfessional; }
    public String getSignatureDate() { return signatureDate; }
    public void setSignatureDate(String signatureDate) { this.signatureDate = signatureDate; }
}
