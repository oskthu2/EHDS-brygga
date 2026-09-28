package se.inera.ehds.mapping.tk;

public class MapperContext {
    private final String patientSystem;
    private final String patientValue;
    private final String bridgeHsaId;
    private final String requestedVgHsaId;

    public MapperContext(String patientSystem, String patientValue, String bridgeHsaId) {
        this(patientSystem, patientValue, bridgeHsaId, null);
    }

    /**
     * @param requestedVgHsaId HSA-id för den vårdgivare anropet gäller (URL-segmentet), eller
     *                         null om anropet inte är VG-scopat. När satt filtreras poster vars
     *                         careProviderHSAId/accountableHealthcareProvider inte matchar bort —
     *                         skydd om bakomliggande system (felaktigt) returnerar poster för
     *                         flera vårdgivare i samma svar.
     */
    public MapperContext(String patientSystem, String patientValue, String bridgeHsaId, String requestedVgHsaId) {
        this.patientSystem = patientSystem;
        this.patientValue = patientValue;
        this.bridgeHsaId = bridgeHsaId;
        this.requestedVgHsaId = requestedVgHsaId;
    }

    public String getPatientSystem() { return patientSystem; }
    public String getPatientValue() { return patientValue; }
    public String getBridgeHsaId() { return bridgeHsaId; }
    public String getRequestedVgHsaId() { return requestedVgHsaId; }
}
