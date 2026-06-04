package se.inera.ehds.mapping.tk;

public final class RivDateParser {

    private RivDateParser() {}

    /**
     * Converts RIVTA integer date strings to ISO 8601.
     * YYYYMMDD        → YYYY-MM-DD
     * YYYYMMDDHHmmss  → YYYY-MM-DDTHH:mm:ss
     */
    public static String parse(String d) {
        if (d == null || d.isBlank()) return null;
        String s = d.trim();
        if (s.length() == 8) {
            return s.substring(0, 4) + "-" + s.substring(4, 6) + "-" + s.substring(6, 8);
        }
        if (s.length() >= 14) {
            return s.substring(0, 4) + "-" + s.substring(4, 6) + "-" + s.substring(6, 8)
                    + "T" + s.substring(8, 10) + ":" + s.substring(10, 12) + ":" + s.substring(12, 14);
        }
        return s;
    }
}
