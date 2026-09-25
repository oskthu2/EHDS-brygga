package se.inera.ehds.mapping.rivta.caredocumentation;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/** careDocumentation.header.signature – signeringsinformation (JoL-header v2.2). */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SignatureType", propOrder = {"signatureId", "name", "timestamp", "byRole"})
public class Signature {
    private String signatureId;
    private String name;
    private String timestamp;
    private CVType byRole;

    public String getSignatureId() { return signatureId; }
    public void setSignatureId(String signatureId) { this.signatureId = signatureId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public CVType getByRole() { return byRole; }
    public void setByRole(CVType byRole) { this.byRole = byRole; }
}
