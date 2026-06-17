package se.inera.ehds.mapping.tk.getdiagnosis;

import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Provenance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import se.inera.ehds.mapping.concept.ConceptMapRegistry;
import se.inera.ehds.mapping.naming.NamingSystemRegistry;
import se.inera.ehds.mapping.rivta.*;
import se.inera.ehds.mapping.tk.MapperContext;
import se.inera.ehds.mapping.tk.MappedDiagnosisEntry;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GetDiagnosisMapperTest {

    private GetDiagnosisMapper mapper;
    private MapperContext ctx;

    @BeforeEach
    void setUp() {
        NamingSystemRegistry namingSystem = new NamingSystemRegistry();
        ConceptMapRegistry conceptMaps = new ConceptMapRegistry();
        mapper = new GetDiagnosisMapper(namingSystem, conceptMaps);
        ctx = new MapperContext(
                "http://electronichealth.se/identifier/personnummer",
                "190101011234",
                "SE2321000999-EHDS");
    }

    @Nested
    class NullOchTomIndata {
        @Test
        void null_response_ger_tom_lista() {
            assertEquals(List.of(), mapper.map(null, ctx));
        }

        @Test
        void null_diagnosis_ger_tom_lista() {
            GetDiagnosisResponse response = new GetDiagnosisResponse();
            response.setDiagnosis(null);
            assertEquals(List.of(), mapper.map(response, ctx));
        }

        @Test
        void tom_diagnosisList_ger_tom_lista() {
            GetDiagnosisResponse response = new GetDiagnosisResponse();
            response.setDiagnosis(List.of());
            assertEquals(List.of(), mapper.map(response, ctx));
        }

        @Test
        void icke_ok_resultCode_ger_tom_lista() {
            GetDiagnosisResponse response = new GetDiagnosisResponse();
            ResultType result = new ResultType();
            result.setResultCode("ERROR");
            response.setResult(result);
            response.setDiagnosis(List.of(minimalDiagnosis()));
            assertEquals(List.of(), mapper.map(response, ctx));
        }

        @Test
        void ok_resultCode_ger_resultat() {
            GetDiagnosisResponse response = new GetDiagnosisResponse();
            ResultType result = new ResultType();
            result.setResultCode("OK");
            response.setResult(result);
            response.setDiagnosis(List.of(minimalDiagnosis()));
            assertEquals(1, mapper.map(response, ctx).size());
        }

        @Test
        void null_result_behandlas_som_ok() {
            GetDiagnosisResponse response = new GetDiagnosisResponse();
            response.setResult(null);
            response.setDiagnosis(List.of(minimalDiagnosis()));
            assertEquals(1, mapper.map(response, ctx).size());
        }
    }

    @Nested
    class KliniskStatus {
        @Test
        void utan_slutdatum_ger_active() {
            List<MappedDiagnosisEntry> result = mapper.map(responseWith(minimalDiagnosis()), ctx);
            Condition c = result.get(0).condition();
            assertEquals("active", c.getClinicalStatus().getCodingFirstRep().getCode());
        }

        @Test
        void med_slutdatum_ger_resolved() {
            Diagnosis diag = minimalDiagnosis();
            DatePeriodType period = new DatePeriodType();
            period.setStart("20230101");
            period.setEnd("20240101");
            diag.getDiagnosisBody().setDiagnosisTimePeriod(period);

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            Condition c = result.get(0).condition();
            assertEquals("resolved", c.getClinicalStatus().getCodingFirstRep().getCode());
        }

        @Test
        void verificationStatus_sätts_till_confirmed() {
            List<MappedDiagnosisEntry> result = mapper.map(responseWith(minimalDiagnosis()), ctx);
            Condition c = result.get(0).condition();
            assertEquals("confirmed", c.getVerificationStatus().getCodingFirstRep().getCode());
        }
    }

    @Nested
    class DiagnosKategori {
        @Test
        void HD_mappar_till_kv_diagnostyp_HD() {
            Diagnosis diag = minimalDiagnosis();
            diag.getDiagnosisBody().setDiagnosisType("HD");

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            Condition c = result.get(0).condition();
            assertEquals("HD", c.getCategoryFirstRep().getCodingFirstRep().getCode());
            assertEquals("https://terminologitjansten.inera.se/inera-kodverksforvaltning/kodverk/kv_diagnostyp",
                    c.getCategoryFirstRep().getCodingFirstRep().getSystem());
        }

        @Test
        void BY_mappar_till_kv_diagnostyp_BY() {
            Diagnosis diag = minimalDiagnosis();
            diag.getDiagnosisBody().setDiagnosisType("BY");

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            Condition c = result.get(0).condition();
            assertEquals("BY", c.getCategoryFirstRep().getCodingFirstRep().getCode());
        }

        @Test
        void okand_diagnostyp_anvandar_raw_kod() {
            Diagnosis diag = minimalDiagnosis();
            diag.getDiagnosisBody().setDiagnosisType("XX");

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            Condition c = result.get(0).condition();
            assertEquals("XX", c.getCategoryFirstRep().getCodingFirstRep().getCode());
        }
    }

    @Nested
    class DiagnosKod {
        @Test
        void kod_fran_cvtype_mappar_till_condition_code() {
            Diagnosis diag = minimalDiagnosis();
            CVType cv = new CVType();
            cv.setCode("J22");
            cv.setCodeSystem("1.2.752.116.1.1.1.1.3");
            cv.setDisplayName("Akut infektion i nedre luftvägarna, ospecificerad");
            diag.getDiagnosisBody().setDiagnosisCode(cv);

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            Condition c = result.get(0).condition();
            assertEquals("J22", c.getCode().getCodingFirstRep().getCode());
            assertEquals("Akut infektion i nedre luftvägarna, ospecificerad",
                    c.getCode().getCodingFirstRep().getDisplay());
        }

        @Test
        void originalText_används_som_text_om_present() {
            Diagnosis diag = minimalDiagnosis();
            CVType cv = new CVType();
            cv.setCode("J22");
            cv.setCodeSystem("1.2.752.116.1.1.1.1.3");
            cv.setDisplayName("Kodverksnamn");
            cv.setOriginalText("Fritext från journalsystem");
            diag.getDiagnosisBody().setDiagnosisCode(cv);

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            Condition c = result.get(0).condition();
            assertEquals("Fritext från journalsystem", c.getCode().getText());
        }

        @Test
        void displayName_används_som_text_om_originalText_saknas() {
            Diagnosis diag = minimalDiagnosis();
            CVType cv = new CVType();
            cv.setCode("J22");
            cv.setCodeSystem("1.2.752.116.1.1.1.1.3");
            cv.setDisplayName("Kodverksnamn");
            diag.getDiagnosisBody().setDiagnosisCode(cv);

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            Condition c = result.get(0).condition();
            assertEquals("Kodverksnamn", c.getCode().getText());
        }
    }

    @Nested
    class Patient {
        @Test
        void personnummer_oid_konverteras_till_uri() {
            Diagnosis diag = minimalDiagnosis();
            PersonIdType pid = new PersonIdType();
            pid.setRoot("1.2.752.129.2.1.3.1");
            pid.setExtension("190101011234");
            diag.getDiagnosisHeader().setPatientId(pid);

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            Condition c = result.get(0).condition();
            assertEquals("http://electronichealth.se/identifier/personnummer",
                    c.getSubject().getIdentifier().getSystem());
            assertEquals("190101011234", c.getSubject().getIdentifier().getValue());
        }

        @Test
        void okand_oid_ger_urn_fallback() {
            Diagnosis diag = minimalDiagnosis();
            PersonIdType pid = new PersonIdType();
            pid.setRoot("9.9.9.9.9");
            pid.setExtension("12345");
            diag.getDiagnosisHeader().setPatientId(pid);

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            Condition c = result.get(0).condition();
            assertEquals("urn:oid:9.9.9.9.9", c.getSubject().getIdentifier().getSystem());
        }
    }

    @Nested
    class TidperiodOchDatum {
        @Test
        void onset_sätts_fran_startdatum() {
            Diagnosis diag = minimalDiagnosis();
            DatePeriodType period = new DatePeriodType();
            period.setStart("20230601");
            diag.getDiagnosisBody().setDiagnosisTimePeriod(period);

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            Condition c = result.get(0).condition();
            assertTrue(c.getOnset().toString().contains("2023-06-01"));
        }

        @Test
        void abatement_sätts_fran_slutdatum() {
            Diagnosis diag = minimalDiagnosis();
            DatePeriodType period = new DatePeriodType();
            period.setStart("20230601");
            period.setEnd("20231231");
            diag.getDiagnosisBody().setDiagnosisTimePeriod(period);

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            Condition c = result.get(0).condition();
            assertTrue(c.getAbatement().toString().contains("2023-12-31"));
        }

        @Test
        void documentTime_sätts_som_recordedDate() {
            Diagnosis diag = minimalDiagnosis();
            diag.getDiagnosisHeader().setDocumentTime("20240315");

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            Condition c = result.get(0).condition();
            assertEquals("2024-03-15", c.getRecordedDateElement().getValueAsString());
        }

        @Test
        void assertedDate_från_legalAuthenticator_signatureDate_läggs_till_som_extension() {
            Diagnosis diag = minimalDiagnosis();
            LegalAuthenticatorType la = new LegalAuthenticatorType();
            la.setSignatureDate("20240101");
            diag.getDiagnosisHeader().setLegalAuthenticator(la);

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            Condition c = result.get(0).condition();
            assertFalse(c.getExtension().isEmpty());
            assertTrue(c.getExtension().get(0).getUrl().endsWith("ext-asserted-date"));
        }
    }

    @Nested
    class RecorderOchAsserter {
        @Test
        void accountableHealthcareProfessional_mappar_till_recorder() {
            Diagnosis diag = minimalDiagnosis();
            HealthcareProfessionalType ahp = new HealthcareProfessionalType();
            PersonIdType pid = new PersonIdType();
            pid.setRoot("1.2.752.129.2.1.4.1");
            pid.setExtension("SE2321000016-DOK");
            ahp.setPersonId(pid);
            diag.getDiagnosisHeader().setAccountableHealthcareProfessional(ahp);

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            Condition c = result.get(0).condition();
            assertNotNull(c.getRecorder());
            assertEquals("SE2321000016-DOK", c.getRecorder().getIdentifier().getValue());
        }

        @Test
        void legalAuthenticator_hcProfessional_mappar_till_asserter() {
            Diagnosis diag = minimalDiagnosis();
            LegalAuthenticatorType la = new LegalAuthenticatorType();
            HealthcareProfessionalType prof = new HealthcareProfessionalType();
            PersonIdType pid = new PersonIdType();
            pid.setRoot("1.2.752.129.2.1.4.1");
            pid.setExtension("SE2321000016-AUTH");
            prof.setPersonId(pid);
            la.setHcProfessional(prof);
            diag.getDiagnosisHeader().setLegalAuthenticator(la);

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            Condition c = result.get(0).condition();
            assertNotNull(c.getAsserter());
            assertEquals("SE2321000016-AUTH", c.getAsserter().getIdentifier().getValue());
        }

        @Test
        void recorder_och_asserter_saknas_utan_rivta_data() {
            List<MappedDiagnosisEntry> result = mapper.map(responseWith(minimalDiagnosis()), ctx);
            Condition c = result.get(0).condition();
            assertTrue(c.getRecorder().isEmpty());
            assertTrue(c.getAsserter().isEmpty());
        }
    }

    @Nested
    class MetaOchProfil {
        @Test
        void se_ehds_condition_profil_deklareras() {
            List<MappedDiagnosisEntry> result = mapper.map(responseWith(minimalDiagnosis()), ctx);
            Condition c = result.get(0).condition();
            assertTrue(c.getMeta().getProfile().stream()
                    .anyMatch(p -> p.getValue().contains("se-ehds-condition")));
        }

        @Test
        void sourceHsaId_sätts_i_meta_source() {
            Diagnosis diag = minimalDiagnosis();
            diag.getDiagnosisHeader().setSourceSystemHSAId("SE2321000016-4HK5");

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            Condition c = result.get(0).condition();
            assertTrue(c.getMeta().getSource().contains("SE2321000016-4HK5"));
        }
    }

    @Nested
    class ProvenanceAgenter {
        @Test
        void provenance_har_custodian_med_careProviderHsaId() {
            Diagnosis diag = minimalDiagnosis();
            diag.getDiagnosisHeader().setCareProviderHSAId("SE111-PROV");

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            Provenance p = result.get(0).provenance();
            String value = agentValue(p, "custodian");
            assertEquals("SE111-PROV", value);
        }

        @Test
        void provenance_har_author_med_careUnitHsaId() {
            Diagnosis diag = minimalDiagnosis();
            diag.getDiagnosisHeader().setCareUnitHSAId("SE222-UNIT");

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            Provenance p = result.get(0).provenance();
            assertEquals("SE222-UNIT", agentValue(p, "author"));
        }

        @Test
        void provenance_har_assembler_med_bridgeHsaId() {
            List<MappedDiagnosisEntry> result = mapper.map(responseWith(minimalDiagnosis()), ctx);
            Provenance p = result.get(0).provenance();
            assertEquals("SE2321000999-EHDS", agentValue(p, "assembler"));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Diagnosis minimalDiagnosis() {
        DiagnosisHeader header = new DiagnosisHeader();
        PersonIdType pid = new PersonIdType();
        pid.setRoot("1.2.752.129.2.1.3.1");
        pid.setExtension("190101011234");
        header.setPatientId(pid);
        header.setDocumentTime("20240315");
        header.setCareUnitHSAId("SE2321000016-4HK5");
        header.setCareProviderHSAId("SE2321000016-PROV");

        DiagnosisBody body = new DiagnosisBody();
        body.setDiagnosisType("HD");
        CVType cv = new CVType();
        cv.setCode("Z00");
        cv.setCodeSystem("1.2.752.116.1.1.1.1.3");
        cv.setDisplayName("Rutinundersökning");
        body.setDiagnosisCode(cv);

        Diagnosis d = new Diagnosis();
        d.setDiagnosisHeader(header);
        d.setDiagnosisBody(body);
        return d;
    }

    private GetDiagnosisResponse responseWith(Diagnosis... diagnoses) {
        GetDiagnosisResponse r = new GetDiagnosisResponse();
        r.setDiagnosis(List.of(diagnoses));
        return r;
    }

    private String agentValue(Provenance p, String role) {
        return p.getAgent().stream()
                .filter(a -> a.getType().getCoding().stream().anyMatch(c -> role.equals(c.getCode())))
                .findFirst()
                .map(a -> a.getWho().getIdentifier().getValue())
                .orElse(null);
    }
}
