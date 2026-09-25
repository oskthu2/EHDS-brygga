package se.inera.ehds.mapping.rivta.caredocumentation;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;

/**
 * careDocumentation.body.
 * clinicalDocumentNoteText and multimediaEntry are mutually exclusive
 * (FSH invariant getcaredocumentation-body-xor).
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BodyType", propOrder = {
        "clinicalDocumentNoteCode", "clinicalDocumentNoteTitle",
        "clinicalDocumentNoteText", "multimediaEntry", "dissentingOpinion"
})
public class Body {
    private CVType clinicalDocumentNoteCode;
    private String clinicalDocumentNoteTitle;
    private String clinicalDocumentNoteText;
    private MultimediaEntry multimediaEntry;

    @XmlElement(name = "dissentingOpinion")
    private List<DissentingOpinion> dissentingOpinion = new ArrayList<>();

    public CVType getClinicalDocumentNoteCode() { return clinicalDocumentNoteCode; }
    public void setClinicalDocumentNoteCode(CVType clinicalDocumentNoteCode) { this.clinicalDocumentNoteCode = clinicalDocumentNoteCode; }
    public String getClinicalDocumentNoteTitle() { return clinicalDocumentNoteTitle; }
    public void setClinicalDocumentNoteTitle(String clinicalDocumentNoteTitle) { this.clinicalDocumentNoteTitle = clinicalDocumentNoteTitle; }
    public String getClinicalDocumentNoteText() { return clinicalDocumentNoteText; }
    public void setClinicalDocumentNoteText(String clinicalDocumentNoteText) { this.clinicalDocumentNoteText = clinicalDocumentNoteText; }
    public MultimediaEntry getMultimediaEntry() { return multimediaEntry; }
    public void setMultimediaEntry(MultimediaEntry multimediaEntry) { this.multimediaEntry = multimediaEntry; }
    public List<DissentingOpinion> getDissentingOpinion() { return dissentingOpinion; }
    public void setDissentingOpinion(List<DissentingOpinion> dissentingOpinion) { this.dissentingOpinion = dissentingOpinion; }
}
