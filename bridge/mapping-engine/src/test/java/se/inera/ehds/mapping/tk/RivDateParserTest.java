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
        void fjorton_tecken_vintertid_ger_iso_datumtid_med_plus01offset() {
            assertEquals("2024-03-15T09:30:00+01:00", RivDateParser.parse("20240315093000"));
        }

        @Test
        void fjorton_tecken_med_extra_tecken_ger_iso_datumtid_med_offset() {
            assertEquals("2024-03-15T09:30:00+01:00", RivDateParser.parse("20240315093000999"));
        }

        @Test
        void fjorton_tecken_sommartid_ger_iso_datumtid_med_plus02offset() {
            assertEquals("2024-07-15T09:30:00+02:00", RivDateParser.parse("20240715093000"));
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
        void midnatt_mappas_korrekt_med_offset() {
            assertEquals("2024-12-31T00:00:00+01:00", RivDateParser.parse("20241231000000"));
        }
    }

    @Nested
    class ParseInstant {
        @Test
        void null_returnerar_null() {
            assertNull(RivDateParser.parseInstant(null));
        }

        @Test
        void tom_strang_returnerar_null() {
            assertNull(RivDateParser.parseInstant(""));
        }

        @Test
        void datumtid_vintertid_konverteras_till_verklig_utc_instant() {
            assertEquals("2024-03-15T08:30:00Z", RivDateParser.parseInstant("20240315093000"));
        }

        @Test
        void datumtid_sommartid_konverteras_till_verklig_utc_instant() {
            assertEquals("2024-07-15T07:30:00Z", RivDateParser.parseInstant("20240715093000"));
        }

        @Test
        void enbart_datum_blir_midnatt_lokal_tid_konverterad_till_utc() {
            assertEquals("2024-12-30T23:00:00Z", RivDateParser.parseInstant("20241231"));
        }
    }
}
