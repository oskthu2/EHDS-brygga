package se.inera.ehds.malac.hapi;

import org.junit.jupiter.api.Test;
import se.inera.ehds.malac.JavaMapperDump;
import se.inera.ehds.mapping.rivta.GetDiagnosisResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dumps the SAME {@code .map} files MaLaC-HD compiles (../../../python/fml/*.map), run here
 * via HAPI FHIR's StructureMapUtilities ({@link HapiFmlEngine}), for the SAME two fixtures
 * {@link JavaMapperDump} already defines (huvuddiagnosFixture/ogiltigtPersonnummerFixture) -
 * so {@code compare/run_comparison.py} can diff production Java, MaLaC-HD/Python and
 * HAPI/Java pairwise, all three against the SAME input. This is the Java/HAPI counterpart to
 * {@code JavaMapperDumpTest} (production mapper) and {@code run_pipeline.py} (MaLaC-HD).
 */
class HapiMapperDumpTest {

    private static final Path OUT_DIR = Path.of("target", "hapi-mapper-dump");
    /** This module's own base directory (holds pom.xml, python/, fhir/) - Surefire's default
     * working directory for a test run IS the module basedir, so "." already resolves here;
     * made explicit for clarity. */
    private static final Path MODULE_ROOT = Path.of(".").toAbsolutePath().normalize();

    @Test
    void dumpar_huvuddiagnos_fixture_till_json() throws Exception {
        HapiFmlEngine engine = new HapiFmlEngine(MODULE_ROOT);
        GetDiagnosisResponse response = JavaMapperDump.huvuddiagnosFixture();
        String json = HapiMapperDump.mapToJson(response, engine);
        assertEquals(1, HapiMapperDump.map(response, engine).size());
        writeOut("get-diagnosis-huvuddiagnos.json", json);
    }

    @Test
    void dumpar_ogiltigt_personnummer_fixture_till_json() throws Exception {
        HapiFmlEngine engine = new HapiFmlEngine(MODULE_ROOT);
        GetDiagnosisResponse response = JavaMapperDump.ogiltigtPersonnummerFixture();
        assertTrue(HapiMapperDump.map(response, engine).isEmpty(), "DIAG-001: ogiltigt personnummer ska stoppa posten");
        writeOut("get-diagnosis-ogiltigt-personnummer.json", "[]\n");
    }

    private void writeOut(String filename, String content) throws IOException {
        Files.createDirectories(OUT_DIR);
        Files.writeString(OUT_DIR.resolve(filename), content);
    }
}
