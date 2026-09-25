package se.inera.ehds.mapping.rivta.caredocumentation;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
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
        "patientId", "accountableHealthcareProvider", "accountableCareUnit",
        "careProcessId", "blockComparisonTime", "approvedForPatient"
})
public class AccessControlHeader {
    private PersonIdType patientId;
    private String accountableHealthcareProvider;
    private String accountableCareUnit;
    private String careProcessId;
    private String blockComparisonTime;
    private Boolean approvedForPatient;

    public PersonIdType getPatientId() { return patientId; }
    public void setPatientId(PersonIdType patientId) { this.patientId = patientId; }
    public String getAccountableHealthcareProvider() { return accountableHealthcareProvider; }
    public void setAccountableHealthcareProvider(String accountableHealthcareProvider) { this.accountableHealthcareProvider = accountableHealthcareProvider; }
    public String getAccountableCareUnit() { return accountableCareUnit; }
    public void setAccountableCareUnit(String accountableCareUnit) { this.accountableCareUnit = accountableCareUnit; }
    public String getCareProcessId() { return careProcessId; }
    public void setCareProcessId(String careProcessId) { this.careProcessId = careProcessId; }
    public String getBlockComparisonTime() { return blockComparisonTime; }
    public void setBlockComparisonTime(String blockComparisonTime) { this.blockComparisonTime = blockComparisonTime; }
    public Boolean getApprovedForPatient() { return approvedForPatient; }
    public void setApprovedForPatient(Boolean approvedForPatient) { this.approvedForPatient = approvedForPatient; }
}
