package se.inera.ehds.mapping.tk.getdiagnosis;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Provenance;
import org.hl7.fhir.r4.model.Reference;
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
        void alltid_active_eftersom_schema_saknar_slutdatum() {
            // Det riktiga GetDiagnosis:2-schemat har ingen period/slutdatumskoncept
            // (diagnosisTime är en enda tidpunkt) – clinicalStatus är därför alltid active.
            List<MappedDiagnosisEntry> result = mapper.map(responseWith(minimalDiagnosis()), ctx);
            Condition c = result.get(0).condition();
            assertEquals("active", c.getClinicalStatus().getCodingFirstRep().getCode());
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
        void huvuddiagnos_mappar_till_kv_diagnostyp_HD() {
            Diagnosis diag = minimalDiagnosis();
            diag.getDiagnosisBody().setTypeOfDiagnosis("Huvuddiagnos");

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            Condition c = result.get(0).condition();
            assertEquals("HD", c.getCategoryFirstRep().getCodingFirstRep().getCode());
            assertEquals("https://terminologitjansten.inera.se/inera-kodverksforvaltning/kodverk/kv_diagnostyp",
                    c.getCategoryFirstRep().getCodingFirstRep().getSystem());
        }

        @Test
        void bidiagnos_mappar_till_kv_diagnostyp_BY() {
            Diagnosis diag = minimalDiagnosis();
            diag.getDiagnosisBody().setTypeOfDiagnosis("Bidiagnos");

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            Condition c = result.get(0).condition();
            assertEquals("BY", c.getCategoryFirstRep().getCodingFirstRep().getCode());
        }

        @Test
        void okand_diagnostyp_ger_data_absent_reason_unknown_ingen_kodning() {
            // DIAG-003: typeOfDiagnosis utan konceptmappning fyller inte category[diagnostyp]
            // med en gissad kod – i stället anges data-absent-reason = unknown.
            Diagnosis diag = minimalDiagnosis();
            diag.getDiagnosisBody().setTypeOfDiagnosis("XX");

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            Condition c = result.get(0).condition();
            CodeableConcept category = c.getCategoryFirstRep();
            assertTrue(category.getCoding().isEmpty());
            Extension ext = category.getExtensionByUrl(
                    "http://hl7.org/fhir/StructureDefinition/data-absent-reason");
            assertNotNull(ext);
            assertEquals("unknown", ((CodeType) ext.getValue()).getCode());
        }
    }

    @Nested
    class KronikerOchRelateradDiagnos {
        @Test
        void chronicDiagnosis_true_ger_extension_chronicDiagnosis_true() {
            Diagnosis diag = minimalDiagnosis();
            diag.getDiagnosisBody().setChronicDiagnosis(true);

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            Condition c = result.get(0).condition();
            Extension ext = c.getExtensionByUrl(
                    "https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-chronic-condition");
            assertNotNull(ext);
            assertTrue(((BooleanType) ext.getValue()).booleanValue());
        }

        @Test
        void chronicDiagnosis_false_ger_extension_chronicDiagnosis_false() {
            Diagnosis diag = minimalDiagnosis();
            diag.getDiagnosisBody().setChronicDiagnosis(false);

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            Condition c = result.get(0).condition();
            Extension ext = c.getExtensionByUrl(
                    "https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-chronic-condition");
            assertNotNull(ext);
            assertFalse(((BooleanType) ext.getValue()).booleanValue());
        }

        @Test
        void saknad_chronicDiagnosis_ger_ingen_extension() {
            List<MappedDiagnosisEntry> result = mapper.map(responseWith(minimalDiagnosis()), ctx);
            Condition c = result.get(0).condition();
            assertNull(c.getExtensionByUrl(
                    "https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-chronic-condition"));
        }

        @Test
        void relatedDiagnosis_documentId_ger_extension_relatedCondition_med_logisk_referens() {
            Diagnosis diag = minimalDiagnosis();
            RelatedDiagnosis related = new RelatedDiagnosis();
            related.setDocumentId("DOC-12345");
            diag.getDiagnosisBody().setRelatedDiagnosis(List.of(related));

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            Condition c = result.get(0).condition();
            Extension ext = c.getExtensionByUrl(
                    "https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-related-condition");
            assertNotNull(ext);
            Reference ref = (Reference) ext.getValue();
            assertEquals("DOC-12345", ref.getIdentifier().getValue());
        }

        @Test
        void flera_relatedDiagnosis_ger_en_extension_per_post() {
            Diagnosis diag = minimalDiagnosis();
            RelatedDiagnosis first = new RelatedDiagnosis();
            first.setDocumentId("DOC-1");
            RelatedDiagnosis second = new RelatedDiagnosis();
            second.setDocumentId("DOC-2");
            diag.getDiagnosisBody().setRelatedDiagnosis(List.of(first, second));

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            Condition c = result.get(0).condition();
            List<Extension> exts = c.getExtensionsByUrl(
                    "https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-related-condition");
            assertEquals(2, exts.size());
            assertEquals("DOC-1", ((Reference) exts.get(0).getValue()).getIdentifier().getValue());
            assertEquals("DOC-2", ((Reference) exts.get(1).getValue()).getIdentifier().getValue());
        }

        @Test
        void saknad_relatedDiagnosis_ger_ingen_extension() {
            List<MappedDiagnosisEntry> result = mapper.map(responseWith(minimalDiagnosis()), ctx);
            Condition c = result.get(0).condition();
            assertNull(c.getExtensionByUrl(
                    "https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-related-condition"));
        }

        @Test
        void relatedDiagnosis_utan_documentId_ger_ingen_extension() {
            Diagnosis diag = minimalDiagnosis();
            diag.getDiagnosisBody().setRelatedDiagnosis(List.of(new RelatedDiagnosis()));

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            Condition c = result.get(0).condition();
            assertNull(c.getExtensionByUrl(
                    "https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-related-condition"));
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
            pid.setType("1.2.752.129.2.1.3.1");
            pid.setId("190101011234");
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
            pid.setType("9.9.9.9.9");
            pid.setId("200001019999");
            diag.getDiagnosisHeader().setPatientId(pid);

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            Condition c = result.get(0).condition();
            assertEquals("urn:oid:9.9.9.9.9", c.getSubject().getIdentifier().getSystem());
        }
    }

    @Nested
    class TidOchDatum {
        @Test
        void onset_sätts_fran_diagnosisTime() {
            Diagnosis diag = minimalDiagnosis();
            diag.getDiagnosisBody().setDiagnosisTime("20230601120000");

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            Condition c = result.get(0).condition();
            assertTrue(c.getOnset().toString().contains("2023-06-01"));
        }

        @Test
        void saknad_diagnosisTime_ger_ingen_onset() {
            Diagnosis diag = minimalDiagnosis();
            diag.getDiagnosisBody().setDiagnosisTime(null);

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            Condition c = result.get(0).condition();
            assertFalse(c.hasOnset());
        }

        @Test
        void authorTime_från_accountableHealthcareProfessional_sätts_som_recordedDate() {
            Diagnosis diag = minimalDiagnosis();
            HealthcareProfessionalType ahp = new HealthcareProfessionalType();
            ahp.setAuthorTime("20240315");
            ahp.setHealthcareProfessionalCareGiverHSAId("SE2321000016-PROV");
            diag.getDiagnosisHeader().setAccountableHealthcareProfessional(ahp);

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            Condition c = result.get(0).condition();
            assertEquals("2024-03-15", c.getRecordedDateElement().getValueAsString());
        }

        @Test
        void saknad_accountableHealthcareProfessional_ger_ingen_recordedDate() {
            Diagnosis diag = minimalDiagnosis();
            diag.getDiagnosisHeader().setAccountableHealthcareProfessional(null);
            diag.getDiagnosisHeader().setDocumentTime("20240315");

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            Condition c = result.get(0).condition();
            assertFalse(c.hasRecordedDate());
        }

        @Test
        void assertedDate_från_legalAuthenticator_signatureTime_läggs_till_som_extension() {
            Diagnosis diag = minimalDiagnosis();
            LegalAuthenticatorType la = new LegalAuthenticatorType();
            la.setSignatureTime("20240101");
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
            ahp.setHealthcareProfessionalHSAId("SE2321000016-DOK");
            ahp.setHealthcareProfessionalCareGiverHSAId("SE2321000016-PROV");
            diag.getDiagnosisHeader().setAccountableHealthcareProfessional(ahp);

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            Condition c = result.get(0).condition();
            assertNotNull(c.getRecorder());
            assertEquals("SE2321000016-DOK", c.getRecorder().getIdentifier().getValue());
            assertEquals("urn:oid:1.2.752.129.2.1.4.1",
                    c.getRecorder().getIdentifier().getSystem());
        }

        @Test
        void legalAuthenticator_mappar_till_asserter() {
            Diagnosis diag = minimalDiagnosis();
            LegalAuthenticatorType la = new LegalAuthenticatorType();
            la.setLegalAuthenticatorHSAId("SE2321000016-AUTH");
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
        void provenance_har_custodian_med_healthcareProfessionalCareGiverHSAId() {
            Diagnosis diag = minimalDiagnosis();
            HealthcareProfessionalType ahp = diag.getDiagnosisHeader().getAccountableHealthcareProfessional();
            ahp.setHealthcareProfessionalCareGiverHSAId("SE111-PROV");

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            Provenance p = result.get(0).provenance();
            String value = agentValue(p, "custodian");
            assertEquals("SE111-PROV", value);
        }

        @Test
        void provenance_har_author_med_healthcareProfessionalCareUnitHSAId() {
            Diagnosis diag = minimalDiagnosis();
            HealthcareProfessionalType ahp = diag.getDiagnosisHeader().getAccountableHealthcareProfessional();
            ahp.setHealthcareProfessionalCareUnitHSAId("SE222-UNIT");

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

        @Test
        void provenance_recorded_kommer_fran_authorTime_inte_documentTime() {
            Diagnosis diag = minimalDiagnosis();
            diag.getDiagnosisHeader().setDocumentTime("20200101");
            diag.getDiagnosisHeader().getAccountableHealthcareProfessional().setAuthorTime("20240315120000");

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            Provenance p = result.get(0).provenance();
            assertTrue(p.getRecordedElement().getValueAsString().startsWith("2024-03-15"));
        }
    }

    @Nested
    class VardgivarFiltrering {
        // Två vårdgivare, BC_TEST_VG1 och BC_TEST_VG2, i samma system: många diagnoser
        // per vårdgivare i samma svar. Ett VG-scopat anrop (MapperContext.requestedVgHsaId)
        // ska bara ge tillbaka poster för den efterfrågade vårdgivaren. Vårdgivaren hämtas
        // från accountableHealthcareProfessional.healthcareProfessionalCareGiverHSAId –
        // den riktiga headern har inget eget careProviderHSAId-fält.

        @Test
        void vg_scopat_anrop_ger_bara_poster_for_efterfragad_vardgivare() {
            List<Diagnosis> diagnoses = List.of(
                    diagnosisFor("BC_TEST_VG1", "J18.9"),
                    diagnosisFor("BC_TEST_VG1", "E11.9"),
                    diagnosisFor("BC_TEST_VG1", "I10"),
                    diagnosisFor("BC_TEST_VG2", "F32.1"),
                    diagnosisFor("BC_TEST_VG2", "N18.3"));

            MapperContext scoped = new MapperContext(
                    "http://electronichealth.se/identifier/personnummer", "190101011234",
                    "SE2321000999-EHDS", "BC_TEST_VG1");

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diagnoses.toArray(new Diagnosis[0])), scoped);

            assertEquals(3, result.size());
            assertTrue(result.stream()
                    .allMatch(e -> "BC_TEST_VG1".equals(agentValue(e.provenance(), "custodian"))));
        }

        @Test
        void vg_scopat_anrop_for_andra_vardgivaren_ger_bara_dess_poster() {
            List<Diagnosis> diagnoses = List.of(
                    diagnosisFor("BC_TEST_VG1", "J18.9"),
                    diagnosisFor("BC_TEST_VG1", "E11.9"),
                    diagnosisFor("BC_TEST_VG2", "F32.1"),
                    diagnosisFor("BC_TEST_VG2", "N18.3"),
                    diagnosisFor("BC_TEST_VG2", "I50.9"));

            MapperContext scoped = new MapperContext(
                    "http://electronichealth.se/identifier/personnummer", "190101011234",
                    "SE2321000999-EHDS", "BC_TEST_VG2");

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diagnoses.toArray(new Diagnosis[0])), scoped);

            assertEquals(3, result.size());
            assertTrue(result.stream()
                    .allMatch(e -> "BC_TEST_VG2".equals(agentValue(e.provenance(), "custodian"))));
        }

        @Test
        void ej_vg_scopat_anrop_ger_alla_vardgivares_poster() {
            List<Diagnosis> diagnoses = List.of(
                    diagnosisFor("BC_TEST_VG1", "J18.9"),
                    diagnosisFor("BC_TEST_VG2", "F32.1"));

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diagnoses.toArray(new Diagnosis[0])), ctx);

            assertEquals(2, result.size());
        }

        @Test
        void saknad_accountableHealthcareProfessional_ger_null_safe_filtrering_inte_npe() {
            // Malformat svar (schemat kräver accountableHealthcareProfessional, men mappningen
            // ska inte krascha om det likväl saknas) – posten filtreras bort vid VG-scopat anrop
            // eftersom careGiverHsaId blir null och aldrig matchar ett konkret requestedVgHsaId.
            Diagnosis diag = minimalDiagnosis();
            diag.getDiagnosisHeader().setAccountableHealthcareProfessional(null);

            MapperContext scoped = new MapperContext(
                    "http://electronichealth.se/identifier/personnummer", "190101011234",
                    "SE2321000999-EHDS", "BC_TEST_VG1");

            assertDoesNotThrow(() -> mapper.map(responseWith(diag), scoped));
            assertEquals(List.of(), mapper.map(responseWith(diag), scoped));
        }

        private Diagnosis diagnosisFor(String careGiverHsaId, String diagnosisCode) {
            Diagnosis diag = minimalDiagnosis();
            diag.getDiagnosisHeader().getAccountableHealthcareProfessional()
                    .setHealthcareProfessionalCareGiverHSAId(careGiverHsaId);
            diag.getDiagnosisBody().getDiagnosisCode().setCode(diagnosisCode);
            return diag;
        }
    }

    @Nested
    class NegativaAcceptanskriterier {
        // Testfall från acceptanskriterierna (Del A: GetDiagnosis → Condition),
        // de gulmarkerade negativa fallen utöver null/tomt indata och icke-OK ResultCode
        // som redan täcks av NullOchTomIndata ovan.

        @Test
        void diagnoskod_fran_kodverk_utan_oid_mappning_ger_urn_oid_fallback() {
            Diagnosis diag = minimalDiagnosis();
            CVType cv = new CVType();
            cv.setCode("XYZ-99");
            cv.setCodeSystem("1.2.3.4.5.999999"); // okänt kodverk, saknas i naming-systems.yaml
            cv.setDisplayName("Okänt kodverk");
            diag.getDiagnosisBody().setDiagnosisCode(cv);

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            Condition c = result.get(0).condition();
            assertEquals("urn:oid:1.2.3.4.5.999999", c.getCode().getCodingFirstRep().getSystem());
            assertEquals("XYZ-99", c.getCode().getCodingFirstRep().getCode());
        }

        @Test
        void diagnostyp_utan_konceptmappning_ger_data_absent_reason_ingen_undantag() {
            // DIAG-003 (dokumenterad i mapping-getdiagnosis.md): okänd typeOfDiagnosis fyller
            // inte category[diagnostyp] med en gissad kod – data-absent-reason=unknown anges
            // i stället. Se även DiagnosKategori.okand_diagnostyp_ger_data_absent_reason_unknown_ingen_kodning.
            Diagnosis diag = minimalDiagnosis();
            diag.getDiagnosisBody().setTypeOfDiagnosis("OKÄND-TYP");

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            assertEquals(1, result.size());
            Condition c = result.get(0).condition();
            assertTrue(c.getCategoryFirstRep().getCoding().isEmpty());
        }

        @Test
        void saknat_personnummer_stoppar_posten() {
            // DIAG-001 (dokumenterad i mapping-getdiagnosis.md): saknas patientId helt i
            // TK-svaret kastas hela diagnosposten bort – ingen Condition utan tillförlitlig
            // patientidentifierare levereras.
            Diagnosis diag = minimalDiagnosis();
            diag.getDiagnosisHeader().setPatientId(null);

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            assertEquals(List.of(), result);
        }

        @Test
        void personnummer_i_fel_format_stoppar_posten() {
            // DIAG-001: ett personnummer som inte är 12 siffror (t.ex. fritext eller fel
            // längd) stoppar posten på samma sätt som ett helt saknat personnummer.
            Diagnosis diag = minimalDiagnosis();
            PersonIdType pid = new PersonIdType();
            pid.setType("1.2.752.129.2.1.3.1");
            pid.setId("inte-ett-personnummer");
            diag.getDiagnosisHeader().setPatientId(pid);

            List<MappedDiagnosisEntry> result = mapper.map(responseWith(diag), ctx);
            assertEquals(List.of(), result);
        }

        @Test
        void personnummer_stoppar_posten_men_paverkar_inte_ovriga_i_samma_svar() {
            Diagnosis utanGiltigtPersonnummer = minimalDiagnosis();
            PersonIdType ogiltig = new PersonIdType();
            ogiltig.setType("1.2.752.129.2.1.3.1");
            ogiltig.setId("123");
            utanGiltigtPersonnummer.getDiagnosisHeader().setPatientId(ogiltig);

            List<MappedDiagnosisEntry> result =
                    mapper.map(responseWith(utanGiltigtPersonnummer, minimalDiagnosis()), ctx);
            assertEquals(1, result.size());
        }

        @Test
        void diagnospost_med_null_diagnosisHeader_filtreras_bort_men_paverkar_inte_ovriga() {
            Diagnosis utanHeader = minimalDiagnosis();
            utanHeader.setDiagnosisHeader(null);

            List<MappedDiagnosisEntry> result =
                    mapper.map(responseWith(utanHeader, minimalDiagnosis()), ctx);
            assertEquals(1, result.size());
        }

        @Test
        void diagnospost_med_null_diagnosisBody_filtreras_bort_men_paverkar_inte_ovriga() {
            Diagnosis utanBody = minimalDiagnosis();
            utanBody.setDiagnosisBody(null);

            List<MappedDiagnosisEntry> result =
                    mapper.map(responseWith(utanBody, minimalDiagnosis()), ctx);
            assertEquals(1, result.size());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Diagnosis minimalDiagnosis() {
        DiagnosisHeader header = new DiagnosisHeader();
        PersonIdType pid = new PersonIdType();
        pid.setType("1.2.752.129.2.1.3.1");
        pid.setId("190101011234");
        header.setPatientId(pid);
        header.setDocumentTime("20240315");
        header.setDocumentId("doc-1");
        header.setSourceSystemHSAId("SE2321000016-4HK5");
        header.setApprovedForPatient(true);

        HealthcareProfessionalType ahp = new HealthcareProfessionalType();
        ahp.setAuthorTime("20240315");
        ahp.setHealthcareProfessionalCareUnitHSAId("SE2321000016-4HK5");
        ahp.setHealthcareProfessionalCareGiverHSAId("SE2321000016-PROV");
        header.setAccountableHealthcareProfessional(ahp);

        DiagnosisBody body = new DiagnosisBody();
        body.setTypeOfDiagnosis("Huvuddiagnos");
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
