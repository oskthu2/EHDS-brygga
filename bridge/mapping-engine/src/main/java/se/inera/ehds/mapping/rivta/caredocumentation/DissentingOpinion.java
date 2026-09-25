package se.inera.ehds.mapping.rivta.caredocumentation;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/** careDocumentation.body.dissentingOpinion[] – avvikande mening. */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DissentingOpinionType", propOrder = {
        "opinionId", "authorTime", "opinion", "personId", "personName"
})
public class DissentingOpinion {
    private String opinionId;
    private String authorTime;
    private String opinion;
    private PersonIdType personId;
    private String personName;

    public String getOpinionId() { return opinionId; }
    public void setOpinionId(String opinionId) { this.opinionId = opinionId; }
    public String getAuthorTime() { return authorTime; }
    public void setAuthorTime(String authorTime) { this.authorTime = authorTime; }
    public String getOpinion() { return opinion; }
    public void setOpinion(String opinion) { this.opinion = opinion; }
    public PersonIdType getPersonId() { return personId; }
    public void setPersonId(PersonIdType personId) { this.personId = personId; }
    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }
}
