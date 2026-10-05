package se.inera.ehds.fml;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.inera.ehds.mapping.concept.ConceptMapRegistry;
import se.inera.ehds.mapping.naming.NamingSystemRegistry;
import se.inera.ehds.mapping.rivta.*;
import se.inera.ehds.mapping.tk.MapperContext;
import se.inera.ehds.mapping.tk.MappedDiagnosisEntry;
import se.inera.ehds.mapping.tk.getdiagnosis.GetDiagnosisMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comparative tests: runs the same RIVTA GetDiagnosis fixtures through (a) the production
 * Java mapper (mapping-engine) and (b) the FML StructureMap PoC, and compares the resulting
 * Condition fields. Covers the three decided error-handling behaviours from PR #38
 * (DIAG-001 stop-on-invalid-personnummer, OID-fallback, DIAG-003 data-absent-reason) plus a
 * happy path. See ig/input/pagecontent/fml-evaluation.md for the write-up these tests back.
 */
class GetDiagnosisFmlComparisonTest {

    private static GetDiagnosisMapper javaMapper;
    private static MapperContext ctx;
    private static FmlEngine fml;

    @BeforeAll
    static void setUp() throws Exception {
        javaMapper = new GetDiagnosisMapper(new NamingSystemRegistry(), new ConceptMapRegistry());
        ctx = new MapperContext(
                "http://electronichealth.se/identifier/personnummer",
                "190101011234",
                "SE2321000999-EHDS");
        fml = new FmlEngine();
        if (System.getenv("FML_DEBUG") != null) {
            System.out.println("=== rendered map ===\n" + fml.render() + "\n=== end ===");
        }
    }

    @Test
    void happyPath_sammaKodsystemOchKategori() {
        Diagnosis d = diagnosis("190101011234", "HD", "1.2.752.116.1.1.1.1.3", "J45", "Astma", "20240101", "20240601");

        Condition javaResult = runJava(d);
        Condition fmlResult = runFml(d);

        assertEquals("https://www.icd10.se/", javaResult.getCode().getCodingFirstRep().getSystem());
        assertEquals(javaResult.getCode().getCodingFirstRep().getSystem(), fmlResult.getCode().getCodingFirstRep().getSystem());
        assertEquals(javaResult.getCode().getCodingFirstRep().getCode(), fmlResult.getCode().getCodingFirstRep().getCode());
        assertEquals("HD", fmlResult.getCategoryFirstRep().getCodingFirstRep().getCode());
        assertEquals(javaResult.getCategoryFirstRep().getCodingFirstRep().getCode(),
                fmlResult.getCategoryFirstRep().getCodingFirstRep().getCode());
        assertEquals("resolved", javaResult.getClinicalStatus().getCodingFirstRep().getCode());
        assertEquals("resolved", fmlResult.getClinicalStatus().getCodingFirstRep().getCode());
    }

    @Test
    void diag001_saknatPersonnummer_javaStopparHelaPosten_fmlStopparBaraFaltet() {
        Diagnosis d = diagnosis(null, "HD", "1.2.752.116.1.1.1.1.3", "J45", "Astma", "20240101", null);

        // Java: hela Condition+Provenance-paret filtreras bort - map() returnerar tom lista.
        GetDiagnosisResponse response = okResponse(d);
        assertEquals(List.of(), javaMapper.map(response, ctx), "Java-mappern ska stoppa hela posten");

        // FML: StructureMap-anropet transform() körs alltid på den Condition som skickats in;
        // "stoppa posten" går inte att uttrycka i en enskild regel - det kräver att anroparen
        // (gruppen/koden som itererar över diagnosisList) hoppar över posten baserat på samma
        // villkor, precis som Java-koden gör i sin stream().filter(Objects::nonNull). Detta är
        // den viktigaste skillnaden utvärderingen hittade för "stoppa resurs"-fallet.
        Condition fmlResult = runFml(d);
        assertFalse(fmlResult.hasSubject(), "utan giltigt personnummer ska subject uteblir i FML-resultatet");
    }

    @Test
    void diag003_okandDiagnosTyp_bada_ger_dataAbsentReason() {
        Diagnosis d = diagnosis("190101011234", "OKAND", "1.2.752.116.1.1.1.1.3", "J45", "Astma", "20240101", null);

        Condition javaResult = runJava(d);
        Condition fmlResult = runFml(d);

        assertDataAbsentReasonUnknown(javaResult.getCategoryFirstRep());
        assertDataAbsentReasonUnknown(fmlResult.getCategoryFirstRep());
    }

    @Test
    void okantKodverkOid_bada_ger_urnOidFallback() {
        Diagnosis d = diagnosis("190101011234", "HD", "2.16.840.1.113883.6.999", "X1", "Okänd kod", "20240101", null);

        Condition javaResult = runJava(d);
        Condition fmlResult = runFml(d);

        assertEquals("urn:oid:2.16.840.1.113883.6.999", javaResult.getCode().getCodingFirstRep().getSystem());
        assertEquals(javaResult.getCode().getCodingFirstRep().getSystem(), fmlResult.getCode().getCodingFirstRep().getSystem());
        assertEquals("X1", fmlResult.getCode().getCodingFirstRep().getCode());
    }

    private void assertDataAbsentReasonUnknown(CodeableConcept category) {
        assertTrue(category.getCoding().isEmpty(), "category ska vara utan coding");
        assertEquals(1, category.getExtension().size());
        assertEquals("http://hl7.org/fhir/StructureDefinition/data-absent-reason",
                category.getExtensionFirstRep().getUrl());
        assertEquals("unknown", category.getExtensionFirstRep().getValue().primitiveValue());
    }

    private Condition runJava(Diagnosis d) {
        List<MappedDiagnosisEntry> result = javaMapper.map(okResponse(d), ctx);
        assertEquals(1, result.size(), "förväntade exakt en mappad post från Java-mappern");
        return result.get(0).condition();
    }

    private Condition runFml(Diagnosis d) {
        Parameters source = RivtaDiagnosisParametersAdapter.toParameters(d);
        Condition result = fml.transformDiagnosis(source);
        if (System.getenv("FML_DEBUG") != null) {
            try {
                System.out.println(new org.hl7.fhir.r4.formats.JsonParser().composeString(result));
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    private GetDiagnosisResponse okResponse(Diagnosis d) {
        GetDiagnosisResponse response = new GetDiagnosisResponse();
        ResultType result = new ResultType();
        result.setResultCode("OK");
        response.setResult(result);
        response.setDiagnosis(List.of(d));
        return response;
    }

    private Diagnosis diagnosis(String personnummer, String diagnosisType, String codeSystemOid,
                                 String code, String display, String onsetStart, String onsetEnd) {
        Diagnosis d = new Diagnosis();
        DiagnosisHeader header = new DiagnosisHeader();
        if (personnummer != null) {
            PersonIdType pid = new PersonIdType();
            pid.setRoot("1.2.752.129.2.1.3.1");
            pid.setExtension(personnummer);
            header.setPatientId(pid);
        }
        header.setSourceSystemHSAId("SE2321000016-ABC");
        header.setCareProviderHSAId("SE2321000016-ABC");
        d.setDiagnosisHeader(header);

        DiagnosisBody body = new DiagnosisBody();
        body.setDiagnosisType(diagnosisType);
        CVType dc = new CVType();
        dc.setCodeSystem(codeSystemOid);
        dc.setCode(code);
        dc.setDisplayName(display);
        body.setDiagnosisCode(dc);
        DatePeriodType period = new DatePeriodType();
        period.setStart(onsetStart);
        period.setEnd(onsetEnd);
        body.setDiagnosisTimePeriod(period);
        d.setDiagnosisBody(body);
        return d;
    }
}
