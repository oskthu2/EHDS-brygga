package se.inera.ehds.mapping.rivta.caredocumentation;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "GetCareDocumentationResponse")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GetCareDocumentationResponseType", propOrder = {"result", "careDocumentation", "hasMore"})
public class GetCareDocumentationResponse {

    private ResultType result;

    @XmlElement(name = "careDocumentation")
    private List<CareDocumentation> careDocumentation = new ArrayList<>();

    @XmlElement(name = "hasMore")
    private List<HasMore> hasMore = new ArrayList<>();

    public ResultType getResult() { return result; }
    public void setResult(ResultType result) { this.result = result; }
    public List<CareDocumentation> getCareDocumentation() { return careDocumentation; }
    public void setCareDocumentation(List<CareDocumentation> careDocumentation) { this.careDocumentation = careDocumentation; }
    public List<HasMore> getHasMore() { return hasMore; }
    public void setHasMore(List<HasMore> hasMore) { this.hasMore = hasMore; }
}
