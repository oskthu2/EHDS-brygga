package se.inera.ehds.config;

import java.util.List;

public class ServiceContractConfig {
    private String id;
    private String namespace;
    private String soapAction;
    private String fhirResource;
    private String fhirPath;
    private String transformer;
    private boolean useTAK = true;
    private boolean useEI = true;
    private boolean useSparr = true;
    private boolean useLogg = true;
    private List<SearchParamConfig> searchParams;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }
    public String getSoapAction() { return soapAction; }
    public void setSoapAction(String soapAction) { this.soapAction = soapAction; }
    public String getFhirResource() { return fhirResource; }
    public void setFhirResource(String fhirResource) { this.fhirResource = fhirResource; }
    public String getFhirPath() { return fhirPath; }
    public void setFhirPath(String fhirPath) { this.fhirPath = fhirPath; }
    public String getTransformer() { return transformer; }
    public void setTransformer(String transformer) { this.transformer = transformer; }
    public boolean isUseTAK() { return useTAK; }
    public void setUseTAK(boolean useTAK) { this.useTAK = useTAK; }
    public boolean isUseEI() { return useEI; }
    public void setUseEI(boolean useEI) { this.useEI = useEI; }
    public boolean isUseSparr() { return useSparr; }
    public void setUseSparr(boolean useSparr) { this.useSparr = useSparr; }
    public boolean isUseLogg() { return useLogg; }
    public void setUseLogg(boolean useLogg) { this.useLogg = useLogg; }
    public List<SearchParamConfig> getSearchParams() { return searchParams; }
    public void setSearchParams(List<SearchParamConfig> searchParams) { this.searchParams = searchParams; }

    public static class SearchParamConfig {
        private String name;
        private String description;
        private boolean required;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }
    }
}
