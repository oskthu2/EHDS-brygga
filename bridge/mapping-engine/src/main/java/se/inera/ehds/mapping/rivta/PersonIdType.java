package se.inera.ehds.mapping.rivta;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PersonIdType", propOrder = {"root", "extension"})
public class PersonIdType {
    private String root;
    private String extension;

    public String getRoot() { return root; }
    public void setRoot(String root) { this.root = root; }
    public String getExtension() { return extension; }
    public void setExtension(String extension) { this.extension = extension; }
}
