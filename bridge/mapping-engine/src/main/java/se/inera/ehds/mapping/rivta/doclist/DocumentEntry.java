package se.inera.ehds.mapping.rivta.doclist;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DocumentEntryType", propOrder = {
        "documentId", "patientId", "sourceSystemHSAId", "documentTime",
        "careUnitHSAId", "careProviderHSAId", "title", "typeCode", "statusCode"
})
public class DocumentEntry {

    private String documentId;
    private PersonIdType patientId;
    private String sourceSystemHSAId;
    private String documentTime;
    private String careUnitHSAId;
    private String careProviderHSAId;
    private String title;
    private CVType typeCode;
    private String statusCode;

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public PersonIdType getPatientId() { return patientId; }
    public void setPatientId(PersonIdType patientId) { this.patientId = patientId; }

    public String getSourceSystemHSAId() { return sourceSystemHSAId; }
    public void setSourceSystemHSAId(String sourceSystemHSAId) { this.sourceSystemHSAId = sourceSystemHSAId; }

    public String getDocumentTime() { return documentTime; }
    public void setDocumentTime(String documentTime) { this.documentTime = documentTime; }

    public String getCareUnitHSAId() { return careUnitHSAId; }
    public void setCareUnitHSAId(String careUnitHSAId) { this.careUnitHSAId = careUnitHSAId; }

    public String getCareProviderHSAId() { return careProviderHSAId; }
    public void setCareProviderHSAId(String careProviderHSAId) { this.careProviderHSAId = careProviderHSAId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public CVType getTypeCode() { return typeCode; }
    public void setTypeCode(CVType typeCode) { this.typeCode = typeCode; }

    public String getStatusCode() { return statusCode; }
    public void setStatusCode(String statusCode) { this.statusCode = statusCode; }
}
