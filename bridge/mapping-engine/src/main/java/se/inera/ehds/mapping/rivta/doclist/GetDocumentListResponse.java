package se.inera.ehds.mapping.rivta.doclist;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "GetDocumentListResponse")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GetDocumentListResponseType", propOrder = {"result", "documentEntry"})
public class GetDocumentListResponse {

    private ResultType result;

    @XmlElement(name = "documentEntry")
    private List<DocumentEntry> documentEntry = new ArrayList<>();

    public ResultType getResult() { return result; }
    public void setResult(ResultType result) { this.result = result; }

    public List<DocumentEntry> getDocumentEntry() { return documentEntry; }
    public void setDocumentEntry(List<DocumentEntry> documentEntry) { this.documentEntry = documentEntry; }
}
