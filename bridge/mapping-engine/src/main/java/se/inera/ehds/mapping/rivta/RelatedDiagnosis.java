package se.inera.ehds.mapping.rivta;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/** RelatedDiagnosisType. Now held as a List on DiagnosisBody (0..unbounded in the real XSD). */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RelatedDiagnosisType", propOrder = {"documentId"})
public class RelatedDiagnosis {
    @XmlElement(namespace = CoreNamespace.VALUE)
    private String documentId;

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
}
