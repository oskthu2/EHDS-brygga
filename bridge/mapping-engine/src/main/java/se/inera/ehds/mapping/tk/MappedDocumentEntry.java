package se.inera.ehds.mapping.tk;

import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Provenance;

/**
 * A DocumentReference produced by GetDocumentList mapping paired with its Provenance.
 * The Provenance carries organisational provenance (careProviderHSAId as custodian,
 * careUnitHSAId as author) needed for correct Sparr blocking.
 */
public record MappedDocumentEntry(DocumentReference documentReference, Provenance provenance) {}
