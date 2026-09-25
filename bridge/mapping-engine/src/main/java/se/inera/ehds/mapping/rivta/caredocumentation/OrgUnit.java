package se.inera.ehds.mapping.rivta.caredocumentation;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OrgUnitType", propOrder = {"orgUnitHSAId", "orgUnitName"})
public class OrgUnit {
    private String orgUnitHSAId;
    private String orgUnitName;

    public String getOrgUnitHSAId() { return orgUnitHSAId; }
    public void setOrgUnitHSAId(String orgUnitHSAId) { this.orgUnitHSAId = orgUnitHSAId; }
    public String getOrgUnitName() { return orgUnitName; }
    public void setOrgUnitName(String orgUnitName) { this.orgUnitName = orgUnitName; }
}
