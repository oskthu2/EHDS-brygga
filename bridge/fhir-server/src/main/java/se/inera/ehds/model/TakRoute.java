package se.inera.ehds.model;

public class TakRoute {
    private String logicalAddress;
    private String physicalAddress;
    private String description;

    public String getLogicalAddress() { return logicalAddress; }
    public void setLogicalAddress(String logicalAddress) { this.logicalAddress = logicalAddress; }
    public String getPhysicalAddress() { return physicalAddress; }
    public void setPhysicalAddress(String physicalAddress) { this.physicalAddress = physicalAddress; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
