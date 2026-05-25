package se.inera.ehds.mapping.tk;

public class MapperContext {
    private final String patientSystem;
    private final String patientValue;

    public MapperContext(String patientSystem, String patientValue) {
        this.patientSystem = patientSystem;
        this.patientValue = patientValue;
    }

    public String getPatientSystem() { return patientSystem; }
    public String getPatientValue() { return patientValue; }
}
