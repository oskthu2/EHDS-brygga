package se.inera.ehds.fml;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import se.inera.ehds.mapping.rivta.CVType;
import se.inera.ehds.mapping.rivta.DatePeriodType;
import se.inera.ehds.mapping.rivta.Diagnosis;
import se.inera.ehds.mapping.rivta.DiagnosisBody;
import se.inera.ehds.mapping.rivta.DiagnosisHeader;
import se.inera.ehds.mapping.rivta.HealthcareProfessionalType;
import se.inera.ehds.mapping.rivta.LegalAuthenticatorType;
import se.inera.ehds.mapping.rivta.PersonIdType;
import se.inera.ehds.mapping.rivta.RelatedDiagnosis;
import se.inera.ehds.mapping.tk.RivDateParser;

/**
 * Flattens the subset of GetDiagnosis fields covered by the FML evaluation PoC into a
 * FHIR Parameters resource. FML needs a FHIR (or logical-model) StructureDefinition on the
 * source side; the RIVTA JAXB types have none, so Parameters is used as a lightweight stand-in
 * rather than authoring a full logical model for the whole RIVTA schema. This flattening step
 * is itself one of the evaluation's findings (see fml-evaluation.md) — it is NOT part of how
 * the production Java mapper works, which reads the nested RIVTA objects directly.
 */
public final class RivtaDiagnosisParametersAdapter {

    private RivtaDiagnosisParametersAdapter() {}

    public static Parameters toParameters(Diagnosis diag) {
        return toParameters(diag, null);
    }

    public static Parameters toParameters(Diagnosis diag, String bridgeHsaId) {
        Parameters p = new Parameters();
        if (diag == null) return p;
        if (bridgeHsaId != null) {
            p.addParameter().setName("bridgeHsaId").setValue(new StringType(bridgeHsaId));
        }
        DiagnosisHeader header = diag.getDiagnosisHeader();
        DiagnosisBody body = diag.getDiagnosisBody();

        if (header != null) {
            PersonIdType pid = header.getPatientId();
            if (pid != null && pid.getExtension() != null) {
                p.addParameter().setName("personnummer").setValue(new StringType(pid.getExtension()));
            }
            if (header.getSourceSystemHSAId() != null) {
                p.addParameter().setName("sourceHsaId").setValue(new StringType(header.getSourceSystemHSAId()));
            }
            if (header.getCareProviderHSAId() != null) {
                p.addParameter().setName("careProviderHsaId").setValue(new StringType(header.getCareProviderHSAId()));
            }
            if (header.getCareUnitHSAId() != null) {
                p.addParameter().setName("careUnitHsaId").setValue(new StringType(header.getCareUnitHSAId()));
            }
            HealthcareProfessionalType ahp = header.getAccountableHealthcareProfessional();
            if (ahp != null) {
                if (ahp.getPersonId() != null && ahp.getPersonId().getExtension() != null) {
                    p.addParameter().setName("recorderPersonId").setValue(new StringType(ahp.getPersonId().getExtension()));
                }
                if (ahp.getAuthorTime() != null) {
                    p.addParameter().setName("recorderAuthorTime").setValue(new StringType(RivDateParser.parse(ahp.getAuthorTime())));
                    // Provenance.recorded uses the RIVTA date string as-is (true-UTC instant), not
                    // the local-offset variant used for recordedDate above - see fml-evaluation.md.
                    String instant = RivDateParser.parseInstant(ahp.getAuthorTime());
                    if (instant != null) {
                        p.addParameter().setName("recorderAuthorInstant").setValue(new StringType(instant));
                    }
                }
            }
            LegalAuthenticatorType la = header.getLegalAuthenticator();
            if (la != null) {
                if (la.getHcProfessional() != null && la.getHcProfessional().getPersonId() != null
                        && la.getHcProfessional().getPersonId().getExtension() != null) {
                    p.addParameter().setName("asserterPersonId")
                            .setValue(new StringType(la.getHcProfessional().getPersonId().getExtension()));
                }
                if (la.getSignatureDate() != null) {
                    p.addParameter().setName("assertedDate").setValue(new StringType(RivDateParser.parse(la.getSignatureDate())));
                }
            }
        }

        if (body != null) {
            if (body.getDiagnosisType() != null) {
                p.addParameter().setName("diagnosisType").setValue(new StringType(body.getDiagnosisType()));
            }
            CVType dc = body.getDiagnosisCode();
            if (dc != null) {
                if (dc.getCodeSystem() != null) {
                    p.addParameter().setName("diagnosisCodeSystemOid").setValue(new StringType(dc.getCodeSystem()));
                }
                if (dc.getCode() != null) {
                    p.addParameter().setName("diagnosisCode").setValue(new StringType(dc.getCode()));
                }
                if (dc.getDisplayName() != null) {
                    p.addParameter().setName("diagnosisCodeDisplay").setValue(new StringType(dc.getDisplayName()));
                }
            }
            if (body.getChronicCondition() != null) {
                p.addParameter().setName("chronicCondition")
                        .setValue(new StringType(body.getChronicCondition().toString()));
            }
            RelatedDiagnosis related = body.getRelatedDiagnosis();
            if (related != null && related.getDocumentId() != null) {
                p.addParameter().setName("relatedDiagnosisDocumentId").setValue(new StringType(related.getDocumentId()));
            }
            DatePeriodType period = body.getDiagnosisTimePeriod();
            String start = period != null ? period.getStart() : null;
            String end = period != null ? period.getEnd() : null;
            // valueDateTime (as opposed to valueString) could not be read back by the StructureMap
            // engine's generic property reflection on Parameters.parameter in this org.hl7.fhir.r4
            // version ("Attempt to read invalid property 'valueDateTime'") - an adapter-layer quirk
            // of using Parameters as a generic carrier, not a property of FML itself. Dates are
            // therefore passed through as valueString and re-parsed on the target side.
            if (start != null) {
                p.addParameter().setName("onsetStart").setValue(new StringType(RivDateParser.parse(start)));
            }
            if (end != null) {
                p.addParameter().setName("onsetEnd").setValue(new StringType(RivDateParser.parse(end)));
            } else {
                p.addParameter().setName("onsetEndAbsent").setValue(new BooleanType(true));
            }
        }
        return p;
    }
}
