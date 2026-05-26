package se.inera.ehds.config;

/**
 * Styr hur bryggan exponerar data från ett tjänstekontrakt utåt som FHIR.
 *
 * RESOURCE_PER_ELEMENT: Varje SOAP-element → en enskild FHIR-resurs.
 *   Används för resursorienterade informationsmängder (GetDiagnosis → Condition,
 *   GetDocumentList → DocumentReference, GetMedication → MedicationStatement).
 *
 * COMPOSITION_ASSEMBLY: Flera SOAP-element → en FHIR Composition med sektioner,
 *   i linje med EURIDICE dokumentutbyte (IHE XDS-style).
 *   Används för journalantecknings-TK:er (GetCareDocumentation → Composition).
 */
public enum OutputMode {
    RESOURCE_PER_ELEMENT,
    COMPOSITION_ASSEMBLY
}
