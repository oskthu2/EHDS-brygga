package se.inera.ehds.mapping.tk;

public class MapperContext {
    private final String patientSystem;
    private final String patientValue;
    private final String bridgeHsaId;

    public MapperContext(String patientSystem, String patientValue, String bridgeHsaId) {
        this.patientSystem = patientSystem;
        this.patientValue = patientValue;
        this.bridgeHsaId = bridgeHsaId;
    }

    public String getPatientSystem() { return patientSystem; }
    public String getPatientValue() { return patientValue; }
    public String getBridgeHsaId() { return bridgeHsaId; }
}
