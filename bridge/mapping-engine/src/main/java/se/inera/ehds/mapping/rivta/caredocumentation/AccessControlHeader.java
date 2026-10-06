package se.inera.ehds.mapping.rivta.caredocumentation;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * JoL-header v2.2 accessControlHeader.
 *
 * Unlike PatientSummaryHeader-based contracts, the PDL/Sparr fields
 * (accountableHealthcareProvider, accountableCareUnit) live directly here —
 * not nested under an accountableHealthcareProfessional block. See DES-005.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AccessControlHeaderType", propOrder = {
        "accountableHealthcareProvider", "accountableCareUnit", "patientId",
        "careProcessId", "blockComparisonTime", "approvedForPatient"
})
public class AccessControlHeader {
    @XmlElement(namespace = CoreNamespace.VALUE)
    private PersonIdType accountableHealthcareProvider;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private PersonIdType accountableCareUnit;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private PersonIdType patientId;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private PersonIdType careProcessId;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String blockComparisonTime;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private Boolean approvedForPatient;

    public PersonIdType getAccountableHealthcareProvider() { return accountableHealthcareProvider; }
    public void setAccountableHealthcareProvider(PersonIdType accountableHealthcareProvider) { this.accountableHealthcareProvider = accountableHealthcareProvider; }
    public PersonIdType getAccountableCareUnit() { return accountableCareUnit; }
    public void setAccountableCareUnit(PersonIdType accountableCareUnit) { this.accountableCareUnit = accountableCareUnit; }
    public PersonIdType getPatientId() { return patientId; }
    public void setPatientId(PersonIdType patientId) { this.patientId = patientId; }
    public PersonIdType getCareProcessId() { return careProcessId; }
    public void setCareProcessId(PersonIdType careProcessId) { this.careProcessId = careProcessId; }
    public String getBlockComparisonTime() { return blockComparisonTime; }
    public void setBlockComparisonTime(String blockComparisonTime) { this.blockComparisonTime = blockComparisonTime; }
    public Boolean getApprovedForPatient() { return approvedForPatient; }
    public void setApprovedForPatient(Boolean approvedForPatient) { this.approvedForPatient = approvedForPatient; }
}
