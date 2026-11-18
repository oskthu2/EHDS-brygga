package se.inera.ehds.malac;

import org.junit.jupiter.api.Test;
import se.inera.ehds.mapping.rivta.GetDiagnosisResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dumps GetDiagnosisMapper.java's actual output for the two fixtures that
 * ../../python/run_pipeline.py also runs through the MaLaC-HD-generated Python transforms,
 * so ../../compare/run_comparison.py can diff the two side by side. This test does not
 * assert anything about mapping CORRECTNESS beyond what GetDiagnosisMapperTest.java already
 * covers - its only job is producing the comparison fixture files.
 */
class JavaMapperDumpTest {

    private static final Path OUT_DIR = Path.of("target", "java-mapper-dump");

    @Test
    void dumpar_huvuddiagnos_fixture_till_json() throws IOException {
        GetDiagnosisResponse response = JavaMapperDump.huvuddiagnosFixture();
        String json = JavaMapperDump.mapToJson(response);
        assertEquals(1, JavaMapperDump.map(response).size());
        writeOut("get-diagnosis-huvuddiagnos.json", json);
    }

    @Test
    void dumpar_ogiltigt_personnummer_fixture_till_json() throws IOException {
        GetDiagnosisResponse response = JavaMapperDump.ogiltigtPersonnummerFixture();
        assertTrue(JavaMapperDump.map(response).isEmpty(), "DIAG-001: ogiltigt personnummer ska stoppa posten");
        writeOut("get-diagnosis-ogiltigt-personnummer.json", "[]\n");
    }

    private void writeOut(String filename, String content) throws IOException {
        Files.createDirectories(OUT_DIR);
        Files.writeString(OUT_DIR.resolve(filename), content);
    }
}
