package se.inera.ehds.mapping.rivta.caredocumentation;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/** careDocumentation.header.signature – signeringsinformation (JoL-header v2.2). */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SignatureType", propOrder = {"id", "name", "timestamp", "byRole"})
public class Signature {
    @XmlElement(namespace = CoreNamespace.VALUE)
    private PersonIdType id;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String name;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String timestamp;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private CVType byRole;

    public PersonIdType getId() { return id; }
    public void setId(PersonIdType id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public CVType getByRole() { return byRole; }
    public void setByRole(CVType byRole) { this.byRole = byRole; }
}
