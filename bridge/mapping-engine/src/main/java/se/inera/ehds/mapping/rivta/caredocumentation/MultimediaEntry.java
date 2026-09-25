package se.inera.ehds.mapping.rivta.caredocumentation;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * careDocumentation.body.multimediaEntry.
 * value and reference are mutually exclusive (FSH invariant getcaredocumentation-multimedia-xor).
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MultimediaEntryType", propOrder = {"mediaType", "value", "reference"})
public class MultimediaEntry {
    private String mediaType;
    private String value;
    private String reference;

    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
}
