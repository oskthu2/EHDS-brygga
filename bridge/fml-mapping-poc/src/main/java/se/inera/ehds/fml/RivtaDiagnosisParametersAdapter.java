package se.inera.ehds.fml;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import se.inera.ehds.mapping.rivta.CVType;
import se.inera.ehds.mapping.rivta.DatePeriodType;
import se.inera.ehds.mapping.rivta.Diagnosis;
import se.inera.ehds.mapping.rivta.DiagnosisBody;
import se.inera.ehds.mapping.rivta.DiagnosisHeader;
import se.inera.ehds.mapping.rivta.PersonIdType;
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
        Parameters p = new Parameters();
        if (diag == null) return p;
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
