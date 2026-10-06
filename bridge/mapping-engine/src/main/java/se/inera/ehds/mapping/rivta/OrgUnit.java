package se.inera.ehds.mapping.rivta;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/** OrgUnitType (clinicalprocess_healthcond_description_2.1.xsd). */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OrgUnitType", propOrder = {
    "orgUnitHSAId", "orgUnitName", "orgUnitTelecom", "orgUnitEmail", "orgUnitAddress", "orgUnitLocation"
})
public class OrgUnit {
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String orgUnitHSAId;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String orgUnitName;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String orgUnitTelecom;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String orgUnitEmail;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String orgUnitAddress;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String orgUnitLocation;

    public String getOrgUnitHSAId() { return orgUnitHSAId; }
    public void setOrgUnitHSAId(String orgUnitHSAId) { this.orgUnitHSAId = orgUnitHSAId; }
    public String getOrgUnitName() { return orgUnitName; }
    public void setOrgUnitName(String orgUnitName) { this.orgUnitName = orgUnitName; }
    public String getOrgUnitTelecom() { return orgUnitTelecom; }
    public void setOrgUnitTelecom(String orgUnitTelecom) { this.orgUnitTelecom = orgUnitTelecom; }
    public String getOrgUnitEmail() { return orgUnitEmail; }
    public void setOrgUnitEmail(String orgUnitEmail) { this.orgUnitEmail = orgUnitEmail; }
    public String getOrgUnitAddress() { return orgUnitAddress; }
    public void setOrgUnitAddress(String orgUnitAddress) { this.orgUnitAddress = orgUnitAddress; }
    public String getOrgUnitLocation() { return orgUnitLocation; }
    public void setOrgUnitLocation(String orgUnitLocation) { this.orgUnitLocation = orgUnitLocation; }
}
