package se.inera.ehds.mapping.tk;

import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Provenance;

/**
 * A DocumentReference produced by GetCareDocumentation mapping paired with its Provenance.
 * The Provenance carries organisational provenance (accountableHealthcareProvider as
 * custodian, accountableCareUnit as author) needed for correct Sparr blocking.
 *
 * {@code composition} is present only when {@code clinicalDocumentNoteText} was DocBook-XML
 * with a structural section hierarchy (Strategy B, mapping-getcaredocumentation.md DOC-004) —
 * an additional, structured view of the same content alongside the flat XHTML narrative that
 * always lands in {@code documentReference.content}.
 */
public record MappedDocumentEntry(DocumentReference documentReference, Provenance provenance, Composition composition) {

    public MappedDocumentEntry(DocumentReference documentReference, Provenance provenance) {
        this(documentReference, provenance, null);
    }
}
