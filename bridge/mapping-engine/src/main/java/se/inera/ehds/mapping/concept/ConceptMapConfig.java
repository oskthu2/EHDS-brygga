package se.inera.ehds.mapping.concept;

import java.util.List;

public class ConceptMapConfig {
    private String name;
    private List<ConceptMapEntry> entries;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<ConceptMapEntry> getEntries() { return entries; }
    public void setEntries(List<ConceptMapEntry> entries) { this.entries = entries; }
}
