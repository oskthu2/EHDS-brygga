package se.inera.ehds.mapping.rivta.caredocumentation;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RecordType", propOrder = {"id", "timestamp"})
public class RecordType {
    @XmlElement(namespace = CoreNamespace.VALUE)
    private PersonIdType id;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String timestamp;

    public PersonIdType getId() { return id; }
    public void setId(PersonIdType id) { this.id = id; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
