package se.inera.ehds.mapping.rivta;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Real shape (core_components/clinicalprocess_healthcond_description_2.1.xsd):
 * {@code id} (the identifier value, e.g. personnummer) + {@code type} (the identifier
 * scheme/OID, fed through NamingSystemRegistry the same way {@code root} was for
 * IIType). Not to be confused with {@code rivta.caredocumentation.PersonIdType}, which
 * is an unrelated, differently-shaped type in a different domain-version package.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PersonIdType", propOrder = {"id", "type"})
public class PersonIdType {
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String id;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String type;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
