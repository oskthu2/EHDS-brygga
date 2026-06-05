package se.inera.ehds.service;

/**
 * SMART-kontext extraherad från JWT Bearer-token i inkommande anrop.
 * Alla fält kan vara null om tokenet saknar respektive claim.
 */
public record SmartContext(
    /** client_id eller azp — eHM-applikationens identitet */
    String clientId,
    /** fhirUser eller sub (skild från clientId) — inloggad vårdpersonal, null vid rent systemanrop */
    String userId,
    /** purpose_of_use-claim eller scope-extrakt (t.ex. "TREAT", "ETREAT") */
    String purpose
) {
    public static SmartContext unknown() {
        return new SmartContext(null, null, null);
    }

    public boolean hasUser() { return userId != null; }
}
