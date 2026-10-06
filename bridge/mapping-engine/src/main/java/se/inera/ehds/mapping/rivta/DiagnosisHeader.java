package se.inera.ehds.mapping.rivta;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * PatientSummaryHeaderType (core_components/clinicalprocess_healthcond_description_2.1.xsd).
 * Note: unlike the earlier, unverified version of this class, the real schema has no
 * careUnitHSAId/careProviderHSAId fields on the header at all.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DiagnosisHeader", propOrder = {
    "documentId", "sourceSystemHSAId", "documentTitle", "documentTime", "patientId",
    "accountableHealthcareProfessional", "legalAuthenticator", "approvedForPatient",
    "careContactId", "nullified", "nullifiedReason"
})
public class DiagnosisHeader {
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String documentId;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String sourceSystemHSAId;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String documentTitle;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String documentTime;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private PersonIdType patientId;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private HealthcareProfessionalType accountableHealthcareProfessional;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private LegalAuthenticatorType legalAuthenticator;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private Boolean approvedForPatient;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String careContactId;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private Boolean nullified;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String nullifiedReason;

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public String getSourceSystemHSAId() { return sourceSystemHSAId; }
    public void setSourceSystemHSAId(String sourceSystemHSAId) { this.sourceSystemHSAId = sourceSystemHSAId; }
    public String getDocumentTitle() { return documentTitle; }
    public void setDocumentTitle(String documentTitle) { this.documentTitle = documentTitle; }
    public String getDocumentTime() { return documentTime; }
    public void setDocumentTime(String documentTime) { this.documentTime = documentTime; }
    public PersonIdType getPatientId() { return patientId; }
    public void setPatientId(PersonIdType patientId) { this.patientId = patientId; }
    public HealthcareProfessionalType getAccountableHealthcareProfessional() { return accountableHealthcareProfessional; }
    public void setAccountableHealthcareProfessional(HealthcareProfessionalType accountableHealthcareProfessional) { this.accountableHealthcareProfessional = accountableHealthcareProfessional; }
    public LegalAuthenticatorType getLegalAuthenticator() { return legalAuthenticator; }
    public void setLegalAuthenticator(LegalAuthenticatorType legalAuthenticator) { this.legalAuthenticator = legalAuthenticator; }
    public Boolean getApprovedForPatient() { return approvedForPatient; }
    public void setApprovedForPatient(Boolean approvedForPatient) { this.approvedForPatient = approvedForPatient; }
    public String getCareContactId() { return careContactId; }
    public void setCareContactId(String careContactId) { this.careContactId = careContactId; }
    public Boolean getNullified() { return nullified; }
    public void setNullified(Boolean nullified) { this.nullified = nullified; }
    public String getNullifiedReason() { return nullifiedReason; }
    public void setNullifiedReason(String nullifiedReason) { this.nullifiedReason = nullifiedReason; }
}
