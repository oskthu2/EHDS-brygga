package se.inera.ehds.mapping.rivta;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "HealthcareProfessionalType", propOrder = {"personId", "roleAtTime"})
public class HealthcareProfessionalType {
    private PersonIdType personId;
    private CVType roleAtTime;

    public PersonIdType getPersonId() { return personId; }
    public void setPersonId(PersonIdType personId) { this.personId = personId; }
    public CVType getRoleAtTime() { return roleAtTime; }
    public void setRoleAtTime(CVType roleAtTime) { this.roleAtTime = roleAtTime; }
}
