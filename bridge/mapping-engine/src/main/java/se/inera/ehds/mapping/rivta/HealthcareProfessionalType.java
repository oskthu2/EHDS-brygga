package se.inera.ehds.mapping.rivta;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "HealthcareProfessionalType", propOrder = {"personId", "roleAtTime", "authorTime"})
public class HealthcareProfessionalType {
    @XmlElement(namespace = CoreNamespace.VALUE)
    private PersonIdType personId;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private CVType roleAtTime;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String authorTime;

    public PersonIdType getPersonId() { return personId; }
    public void setPersonId(PersonIdType personId) { this.personId = personId; }
    public CVType getRoleAtTime() { return roleAtTime; }
    public void setRoleAtTime(CVType roleAtTime) { this.roleAtTime = roleAtTime; }
    public String getAuthorTime() { return authorTime; }
    public void setAuthorTime(String authorTime) { this.authorTime = authorTime; }
}
