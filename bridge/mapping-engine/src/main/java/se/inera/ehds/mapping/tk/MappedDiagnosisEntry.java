package se.inera.ehds.mapping.tk;

import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Provenance;

/**
 * A Condition produced by GetDiagnosis mapping paired with its Provenance.
 * The Provenance carries organisational provenance (careProviderHSAId, careUnitHSAId,
 * bridge assembler) needed for correct Sparr organisational-level blocking.
 */
public record MappedDiagnosisEntry(Condition condition, Provenance provenance) {}
