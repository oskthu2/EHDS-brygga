package se.inera.ehds.mapping.rivta;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CVType", propOrder = {
    "code", "codeSystem", "codeSystemName", "codeSystemVersion", "displayName", "originalText"
})
public class CVType {
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String code;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String codeSystem;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String codeSystemName;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String codeSystemVersion;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String displayName;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String originalText;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getCodeSystem() { return codeSystem; }
    public void setCodeSystem(String codeSystem) { this.codeSystem = codeSystem; }
    public String getCodeSystemName() { return codeSystemName; }
    public void setCodeSystemName(String codeSystemName) { this.codeSystemName = codeSystemName; }
    public String getCodeSystemVersion() { return codeSystemVersion; }
    public void setCodeSystemVersion(String codeSystemVersion) { this.codeSystemVersion = codeSystemVersion; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getOriginalText() { return originalText; }
    public void setOriginalText(String originalText) { this.originalText = originalText; }
}
