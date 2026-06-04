package se.inera.ehds.mapping.tk;

import org.hl7.fhir.r4.model.Provenance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import se.inera.ehds.mapping.naming.NamingSystemRegistry;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ProvenanceBuilderTest {

    private static final String HSA_SYSTEM = "urn:oid:1.2.752.129.2.1.4.1";
    private static final String PROV_SYS = "http://terminology.hl7.org/CodeSystem/provenance-participant-type";

    private MapperContext ctx;

    @BeforeEach
    void setUp() {
        ctx = new MapperContext("http://electronichealth.se/identifier/personnummer", "190101011234", "SE2321000999-EHDS");
    }

    @Nested
    class Grundstruktur {
        @Test
        void provenance_har_target_referens() {
            Provenance p = ProvenanceBuilder.build("abc-123", "SE111-A", "SE222-B", "20240315", HSA_SYSTEM, ctx);
            assertEquals("urn:uuid:abc-123", p.getTarget().get(0).getReference());
        }

        @Test
        void datumstrang_atta_tecken_ger_recorded_med_tid() {
            Provenance p = ProvenanceBuilder.build("x", "SE111-A", "SE222-B", "20240315", HSA_SYSTEM, ctx);
            assertNotNull(p.getRecorded());
            // YYYYMMDD → recorded sätts till midnatt UTC
            assertTrue(p.getRecordedElement().getValueAsString().startsWith("2024-03-15"));
        }

        @Test
        void null_documentTime_ger_aktuellt_datum() {
            Provenance p = ProvenanceBuilder.build("x", "SE111-A", "SE222-B", null, HSA_SYSTEM, ctx);
            assertNotNull(p.getRecorded());
        }
    }

    @Nested
    class Agenter {
        @Test
        void tre_agenter_vid_fullstandig_indata() {
            Provenance p = ProvenanceBuilder.build("x", "SE111-A", "SE222-B", "20240315", HSA_SYSTEM, ctx);
            assertEquals(3, p.getAgent().size());
        }

        @Test
        void custodian_agent_har_careProviderHsaId() {
            Provenance p = ProvenanceBuilder.build("x", "SE111-A", "SE222-B", "20240315", HSA_SYSTEM, ctx);
            Provenance.ProvenanceAgentComponent custodian = agentByRole(p, "custodian");
            assertNotNull(custodian);
            assertEquals("SE111-A", custodian.getWho().getIdentifier().getValue());
            assertEquals(HSA_SYSTEM, custodian.getWho().getIdentifier().getSystem());
        }

        @Test
        void author_agent_har_careUnitHsaId() {
            Provenance p = ProvenanceBuilder.build("x", "SE111-A", "SE222-B", "20240315", HSA_SYSTEM, ctx);
            Provenance.ProvenanceAgentComponent author = agentByRole(p, "author");
            assertNotNull(author);
            assertEquals("SE222-B", author.getWho().getIdentifier().getValue());
        }

        @Test
        void assembler_agent_har_bridgeHsaId() {
            Provenance p = ProvenanceBuilder.build("x", "SE111-A", "SE222-B", "20240315", HSA_SYSTEM, ctx);
            Provenance.ProvenanceAgentComponent assembler = agentByRole(p, "assembler");
            assertNotNull(assembler);
            assertEquals("SE2321000999-EHDS", assembler.getWho().getIdentifier().getValue());
        }

        @Test
        void null_careProviderHsaId_utelämnar_custodian_agent() {
            Provenance p = ProvenanceBuilder.build("x", null, "SE222-B", "20240315", HSA_SYSTEM, ctx);
            assertNull(agentByRole(p, "custodian"));
            assertEquals(2, p.getAgent().size());
        }

        @Test
        void null_careUnitHsaId_utelämnar_author_agent() {
            Provenance p = ProvenanceBuilder.build("x", "SE111-A", null, "20240315", HSA_SYSTEM, ctx);
            assertNull(agentByRole(p, "author"));
            assertEquals(2, p.getAgent().size());
        }

        @Test
        void null_ctx_utelämnar_assembler_agent() {
            Provenance p = ProvenanceBuilder.build("x", "SE111-A", "SE222-B", "20240315", HSA_SYSTEM, null);
            assertNull(agentByRole(p, "assembler"));
            assertEquals(2, p.getAgent().size());
        }
    }

    private Provenance.ProvenanceAgentComponent agentByRole(Provenance p, String role) {
        return p.getAgent().stream()
                .filter(a -> a.getType().getCoding().stream().anyMatch(c -> role.equals(c.getCode())))
                .findFirst()
                .orElse(null);
    }
}
