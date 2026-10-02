package se.inera.ehds.mapping.tk;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class RivDateParser {

    private static final ZoneId STOCKHOLM = ZoneId.of("Europe/Stockholm");
    private static final DateTimeFormatter DATE_TIME_WITH_OFFSET =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    private static final DateTimeFormatter INSTANT_UTC =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private RivDateParser() {}

    /**
     * Converts RIVTA integer date strings to ISO 8601.
     * YYYYMMDD        → YYYY-MM-DD
     * YYYYMMDDHHmmss  → YYYY-MM-DDTHH:mm:ss+01:00 / +02:00 (Europe/Stockholm, DST-medveten)
     */
    public static String parse(String d) {
        if (d == null || d.isBlank()) return null;
        String s = d.trim();
        if (s.length() == 8) {
            return s.substring(0, 4) + "-" + s.substring(4, 6) + "-" + s.substring(6, 8);
        }
        if (s.length() >= 14) {
            return toStockholmZonedDateTime(s).format(DATE_TIME_WITH_OFFSET);
        }
        return s;
    }

    /**
     * Converts a RIVTA integer date/datetime string to a true UTC instant
     * (YYYY-MM-DDTHH:mm:ssZ), by interpreting the source as local Europe/Stockholm time
     * and converting — not by appending "Z" to an unconverted local timestamp.
     */
    public static String parseInstant(String d) {
        if (d == null || d.isBlank()) return null;
        String s = d.trim();
        ZonedDateTime stockholm;
        if (s.length() == 8) {
            stockholm = LocalDate.of(
                            Integer.parseInt(s.substring(0, 4)),
                            Integer.parseInt(s.substring(4, 6)),
                            Integer.parseInt(s.substring(6, 8)))
                    .atStartOfDay(STOCKHOLM);
        } else if (s.length() >= 14) {
            stockholm = toStockholmZonedDateTime(s);
        } else {
            return s;
        }
        return stockholm.withZoneSameInstant(ZoneOffset.UTC).format(INSTANT_UTC);
    }

    private static ZonedDateTime toStockholmZonedDateTime(String s) {
        LocalDateTime local = LocalDateTime.of(
                Integer.parseInt(s.substring(0, 4)),
                Integer.parseInt(s.substring(4, 6)),
                Integer.parseInt(s.substring(6, 8)),
                Integer.parseInt(s.substring(8, 10)),
                Integer.parseInt(s.substring(10, 12)),
                Integer.parseInt(s.substring(12, 14)));
        return local.atZone(STOCKHOLM);
    }
}
