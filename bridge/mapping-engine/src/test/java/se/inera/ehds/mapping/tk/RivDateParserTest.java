package se.inera.ehds.mapping.tk;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RivDateParserTest {

    @Nested
    class NullOchTomIndata {
        @Test
        void null_returnerar_null() {
            assertNull(RivDateParser.parse(null));
        }

        @Test
        void tom_strang_returnerar_null() {
            assertNull(RivDateParser.parse(""));
        }

        @Test
        void blanksteg_returnerar_null() {
            assertNull(RivDateParser.parse("   "));
        }
    }

    @Nested
    class DatumFormat {
        @Test
        void atta_tecken_ger_iso_datum() {
            assertEquals("2024-03-15", RivDateParser.parse("20240315"));
        }

        @Test
        void fjorton_tecken_ger_iso_datumtid() {
            assertEquals("2024-03-15T09:30:00", RivDateParser.parse("20240315093000"));
        }

        @Test
        void fjorton_tecken_med_extra_tecken_ger_iso_datumtid() {
            assertEquals("2024-03-15T09:30:00", RivDateParser.parse("20240315093000999"));
        }

        @Test
        void fyra_tecken_returneras_as_is() {
            assertEquals("2024", RivDateParser.parse("2024"));
        }

        @Test
        void ledande_blanksteg_ignoreras() {
            assertEquals("2024-03-15", RivDateParser.parse("  20240315  "));
        }

        @Test
        void arsskifte_mappas_korrekt() {
            assertEquals("2000-01-01", RivDateParser.parse("20000101"));
        }

        @Test
        void midnatt_mappas_korrekt() {
            assertEquals("2024-12-31T00:00:00", RivDateParser.parse("20241231000000"));
        }
    }
}
