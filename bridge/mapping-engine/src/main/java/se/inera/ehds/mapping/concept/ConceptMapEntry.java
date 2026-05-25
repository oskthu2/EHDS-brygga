package se.inera.ehds.mapping.concept;

public class ConceptMapEntry {
    private String sourceCode;
    private String targetSystem;
    private String targetCode;
    private String display;

    public ConceptMapEntry() {}

    public ConceptMapEntry(String sourceCode, String targetSystem, String targetCode, String display) {
        this.sourceCode = sourceCode;
        this.targetSystem = targetSystem;
        this.targetCode = targetCode;
        this.display = display;
    }

    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }
    public String getTargetSystem() { return targetSystem; }
    public void setTargetSystem(String targetSystem) { this.targetSystem = targetSystem; }
    public String getTargetCode() { return targetCode; }
    public void setTargetCode(String targetCode) { this.targetCode = targetCode; }
    public String getDisplay() { return display; }
    public void setDisplay(String display) { this.display = display; }
}
