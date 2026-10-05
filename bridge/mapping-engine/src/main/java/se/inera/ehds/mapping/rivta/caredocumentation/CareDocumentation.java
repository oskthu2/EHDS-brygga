package se.inera.ehds.mapping.rivta.caredocumentation;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/** One careDocumentation entry — gives rise to exactly one DocumentReference. */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CareDocumentationType", propOrder = {"header", "body"})
public class CareDocumentation {
    @XmlElement(namespace = CoreNamespace.VALUE)
    private Header header;
    @XmlElement(namespace = CoreNamespace.VALUE)
    private Body body;

    public Header getHeader() { return header; }
    public void setHeader(Header header) { this.header = header; }
    public Body getBody() { return body; }
    public void setBody(Body body) { this.body = body; }
}
