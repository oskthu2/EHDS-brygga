package se.inera.ehds.mapping.rivta;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "HealthcareProfessionalType", propOrder = {
    "authorTime", "healthcareProfessionalHSAId", "healthcareProfessionalName",
    "healthcareProfessionalRoleCode", "healthcareProfessionalOrgUnit",
    "healthcareProfessionalCareUnitHSAId", "healthcareProfessionalCareGiverHSAId"
})
public class HealthcareProfessionalType {
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String authorTime;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String healthcareProfessionalHSAId;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String healthcareProfessionalName;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private CVType healthcareProfessionalRoleCode;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private OrgUnit healthcareProfessionalOrgUnit;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String healthcareProfessionalCareUnitHSAId;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String healthcareProfessionalCareGiverHSAId;

    public String getAuthorTime() { return authorTime; }
    public void setAuthorTime(String authorTime) { this.authorTime = authorTime; }
    public String getHealthcareProfessionalHSAId() { return healthcareProfessionalHSAId; }
    public void setHealthcareProfessionalHSAId(String healthcareProfessionalHSAId) { this.healthcareProfessionalHSAId = healthcareProfessionalHSAId; }
    public String getHealthcareProfessionalName() { return healthcareProfessionalName; }
    public void setHealthcareProfessionalName(String healthcareProfessionalName) { this.healthcareProfessionalName = healthcareProfessionalName; }
    public CVType getHealthcareProfessionalRoleCode() { return healthcareProfessionalRoleCode; }
    public void setHealthcareProfessionalRoleCode(CVType healthcareProfessionalRoleCode) { this.healthcareProfessionalRoleCode = healthcareProfessionalRoleCode; }
    public OrgUnit getHealthcareProfessionalOrgUnit() { return healthcareProfessionalOrgUnit; }
    public void setHealthcareProfessionalOrgUnit(OrgUnit healthcareProfessionalOrgUnit) { this.healthcareProfessionalOrgUnit = healthcareProfessionalOrgUnit; }
    public String getHealthcareProfessionalCareUnitHSAId() { return healthcareProfessionalCareUnitHSAId; }
    public void setHealthcareProfessionalCareUnitHSAId(String healthcareProfessionalCareUnitHSAId) { this.healthcareProfessionalCareUnitHSAId = healthcareProfessionalCareUnitHSAId; }
    public String getHealthcareProfessionalCareGiverHSAId() { return healthcareProfessionalCareGiverHSAId; }
    public void setHealthcareProfessionalCareGiverHSAId(String healthcareProfessionalCareGiverHSAId) { this.healthcareProfessionalCareGiverHSAId = healthcareProfessionalCareGiverHSAId; }
}
