package se.inera.ehds.mapping.tk;

import org.hl7.fhir.r4.model.*;

import java.util.Date;
import java.util.UUID;

/**
 * Builds the standard three-agent Provenance for any FHIR resource produced by a TK mapper.
 *
 * Roles:
 *   custodian  – juridiskt ansvarig vårdgivare (careProviderHSAId) – yttre Sparr-nyckel
 *   author     – informationsägare vårdenhet  (careUnitHSAId)     – inre Sparr-nyckel
 *   assembler  – EHDS-bryggan                (MapperContext.bridgeHsaId)
 */
public final class ProvenanceBuilder {

    private static final String PROV_SYS =
            "http://terminology.hl7.org/CodeSystem/provenance-participant-type";

    private ProvenanceBuilder() {}

    /**
     * @param targetId          FHIR resource id the Provenance describes
     * @param careProviderHsaId HSA-id for juridiskt ansvarig vårdgivare (may be null)
     * @param careUnitHsaId     HSA-id for informationsägare vårdenhet   (may be null)
     * @param documentTime      RIVTA date string (YYYYMMDDHHmmss or YYYYMMDD); used as recorded
     * @param hsaSystem         resolved URI for the HSA OID (from NamingSystemRegistry)
     * @param ctx               mapper context carrying bridgeHsaId
     */
    public static Provenance build(String targetId,
                                   String careProviderHsaId,
                                   String careUnitHsaId,
                                   String documentTime,
                                   String hsaSystem,
                                   MapperContext ctx) {
        Provenance p = new Provenance();
        p.setId(UUID.randomUUID().toString());
        p.addTarget(new Reference("urn:uuid:" + targetId));

        if (documentTime != null) {
            String iso = RivDateParser.parse(documentTime);
            if (iso != null) {
                p.setRecordedElement(new InstantType(
                        iso.length() == 10 ? iso + "T00:00:00Z" : iso + "Z"));
            } else {
                p.setRecorded(new Date());
            }
        } else {
            p.setRecorded(new Date());
        }

        addAgent(p, "custodian", hsaSystem, careProviderHsaId);
        addAgent(p, "author",    hsaSystem, careUnitHsaId);
        addAgent(p, "assembler", hsaSystem, ctx != null ? ctx.getBridgeHsaId() : null);

        return p;
    }

    private static void addAgent(Provenance p, String role, String system, String value) {
        if (value == null) return;
        p.addAgent()
                .setType(new CodeableConcept()
                        .addCoding(new Coding().setSystem(PROV_SYS).setCode(role)))
                .setWho(new Reference().setIdentifier(
                        new Identifier().setSystem(system).setValue(value)));
    }
}
