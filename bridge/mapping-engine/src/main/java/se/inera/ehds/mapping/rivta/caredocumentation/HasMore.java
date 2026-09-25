package se.inera.ehds.mapping.rivta.caredocumentation;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Top-level pagination marker, parallel to careDocumentation and result in the response.
 * Has no FHIR equivalent — see DOC-001 in mapping-getcaredocumentation.md. Not mapped.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "HasMoreType", propOrder = {"logicalAddress", "reference"})
public class HasMore {
    private String logicalAddress;
    private String reference;

    public String getLogicalAddress() { return logicalAddress; }
    public void setLogicalAddress(String logicalAddress) { this.logicalAddress = logicalAddress; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
}
