package se.inera.ehds.model;

public class EiEngagement {
    private String patientId;
    private String patientSystem;
    private String namespace;
    private String logicalAddress;

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getPatientSystem() { return patientSystem; }
    public void setPatientSystem(String patientSystem) { this.patientSystem = patientSystem; }
    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }
    public String getLogicalAddress() { return logicalAddress; }
    public void setLogicalAddress(String logicalAddress) { this.logicalAddress = logicalAddress; }
}
