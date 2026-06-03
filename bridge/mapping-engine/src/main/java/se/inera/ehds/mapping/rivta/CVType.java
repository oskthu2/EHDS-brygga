package se.inera.ehds.mapping.rivta;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CVType", propOrder = {"code", "codeSystem", "displayName", "originalText"})
public class CVType {
    private String code;
    private String codeSystem;
    private String displayName;
    private String originalText;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getCodeSystem() { return codeSystem; }
    public void setCodeSystem(String codeSystem) { this.codeSystem = codeSystem; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getOriginalText() { return originalText; }
    public void setOriginalText(String originalText) { this.originalText = originalText; }
}
