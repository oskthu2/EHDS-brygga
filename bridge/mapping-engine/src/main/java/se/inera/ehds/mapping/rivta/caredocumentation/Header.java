package se.inera.ehds.mapping.rivta.caredocumentation;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/** careDocumentation.header – JoL-header v2.2 (not PatientSummaryHeader). */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "HeaderType", propOrder = {
        "accessControlHeader", "sourceSystemId", "record", "author", "signature"
})
public class Header {
    private AccessControlHeader accessControlHeader;
    private String sourceSystemId;
    private RecordType record;
    private Author author;
    private Signature signature;

    public AccessControlHeader getAccessControlHeader() { return accessControlHeader; }
    public void setAccessControlHeader(AccessControlHeader accessControlHeader) { this.accessControlHeader = accessControlHeader; }
    public String getSourceSystemId() { return sourceSystemId; }
    public void setSourceSystemId(String sourceSystemId) { this.sourceSystemId = sourceSystemId; }
    public RecordType getRecord() { return record; }
    public void setRecord(RecordType record) { this.record = record; }
    public Author getAuthor() { return author; }
    public void setAuthor(Author author) { this.author = author; }
    public Signature getSignature() { return signature; }
    public void setSignature(Signature signature) { this.signature = signature; }
}
