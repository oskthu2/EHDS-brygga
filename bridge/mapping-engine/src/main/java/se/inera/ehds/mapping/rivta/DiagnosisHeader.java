package se.inera.ehds.mapping.rivta;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DiagnosisHeader", propOrder = {
    "patientId", "sourceSystemHSAId", "documentTime",
    "careUnitHSAId", "careProviderHSAId",
    "accountableHealthcareProfessional", "legalAuthenticator"
})
public class DiagnosisHeader {
    private PersonIdType patientId;
    private String sourceSystemHSAId;
    private String documentTime;
    private String careUnitHSAId;
    private String careProviderHSAId;
    private HealthcareProfessionalType accountableHealthcareProfessional;
    private LegalAuthenticatorType legalAuthenticator;

    public PersonIdType getPatientId() { return patientId; }
    public void setPatientId(PersonIdType patientId) { this.patientId = patientId; }
    public String getSourceSystemHSAId() { return sourceSystemHSAId; }
    public void setSourceSystemHSAId(String sourceSystemHSAId) { this.sourceSystemHSAId = sourceSystemHSAId; }
    public String getDocumentTime() { return documentTime; }
    public void setDocumentTime(String documentTime) { this.documentTime = documentTime; }
    public String getCareUnitHSAId() { return careUnitHSAId; }
    public void setCareUnitHSAId(String careUnitHSAId) { this.careUnitHSAId = careUnitHSAId; }
    public String getCareProviderHSAId() { return careProviderHSAId; }
    public void setCareProviderHSAId(String careProviderHSAId) { this.careProviderHSAId = careProviderHSAId; }
    public HealthcareProfessionalType getAccountableHealthcareProfessional() { return accountableHealthcareProfessional; }
    public void setAccountableHealthcareProfessional(HealthcareProfessionalType accountableHealthcareProfessional) { this.accountableHealthcareProfessional = accountableHealthcareProfessional; }
    public LegalAuthenticatorType getLegalAuthenticator() { return legalAuthenticator; }
    public void setLegalAuthenticator(LegalAuthenticatorType legalAuthenticator) { this.legalAuthenticator = legalAuthenticator; }
}
