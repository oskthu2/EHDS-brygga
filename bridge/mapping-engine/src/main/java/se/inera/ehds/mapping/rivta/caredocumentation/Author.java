package se.inera.ehds.mapping.rivta.caredocumentation;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/** careDocumentation.header.author – dokumentationsansvarig (JoL-header v2.2). */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AuthorType", propOrder = {"authorId", "name", "timestamp", "byRole", "orgUnit"})
public class Author {
    private String authorId;
    private String name;
    private String timestamp;
    private CVType byRole;
    private OrgUnit orgUnit;

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public CVType getByRole() { return byRole; }
    public void setByRole(CVType byRole) { this.byRole = byRole; }
    public OrgUnit getOrgUnit() { return orgUnit; }
    public void setOrgUnit(OrgUnit orgUnit) { this.orgUnit = orgUnit; }
}
