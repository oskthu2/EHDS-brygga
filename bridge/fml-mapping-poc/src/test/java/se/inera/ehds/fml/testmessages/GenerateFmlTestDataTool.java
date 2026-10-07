package se.inera.ehds.fml.testmessages;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import se.inera.ehds.fml.GetCareDocumentationJsonSourceBuilder;
import se.inera.ehds.fml.GetDiagnosisJsonSourceBuilder;
import se.inera.ehds.mapping.naming.NamingSystemRegistry;
import se.inera.ehds.mapping.rivta.GetDiagnosisResponse;
import se.inera.ehds.mapping.rivta.caredocumentation.GetCareDocumentationResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Not a test — generates one JSON-source file per bundled SOAP test message under
 * {@code bridge/fml-mapping-poc/fml-test-data/}, for use as the "JSON file" input of the
 * fhir-mapbuilder VS Code extension's "Validate StructureMap" command (see the plugin guide in
 * this module's README). Each file is an instance of {@code inera-ehds-lm-diagnosis} or
 * {@code inera-ehds-lm-care-documentation} — exactly what {@link FmlTestClient} builds in
 * memory, just written to disk so the extension (which needs a file path, not a Java object)
 * can read it.
 *
 * Run with: {@code mvn -pl fml-mapping-poc test-compile exec:java
 * -Dexec.mainClass=se.inera.ehds.fml.testmessages.GenerateFmlTestDataTool
 * -Dexec.classpathScope=test}, or whenever the bundled test messages or the JSON source builders
 * change.
 */
public final class GenerateFmlTestDataTool {

    private record Source(String xmlResource, String outputFileName, boolean isCareDocumentation) {
    }

    private static final java.util.List<Source> SOURCES = java.util.List.of(
            new Source("/testmessages/get-diagnosis-huvuddiagnos.xml",
                    "get-diagnosis-huvuddiagnos.json", false),
            new Source("/testmessages/get-diagnosis-ogiltigt-personnummer.xml",
                    "get-diagnosis-ogiltigt-personnummer.json", false),
            new Source("/testmessages/get-caredocumentation-plattfritext.xml",
                    "get-caredocumentation-plattfritext.json", true),
            new Source("/testmessages/get-caredocumentation-multimedia.xml",
                    "get-caredocumentation-multimedia.json", true)
    );

    public static void main(String[] args) throws Exception {
        Path targetDir = Path.of("fml-test-data");
        Files.createDirectories(targetDir);

        NamingSystemRegistry namingSystem = new NamingSystemRegistry();
        GetDiagnosisJsonSourceBuilder diagnosisBuilder = new GetDiagnosisJsonSourceBuilder(namingSystem);
        GetCareDocumentationJsonSourceBuilder careDocBuilder = new GetCareDocumentationJsonSourceBuilder(namingSystem);

        for (Source source : SOURCES) {
            String json = source.isCareDocumentation()
                    ? careDocBuilder.toJson(unmarshal(source.xmlResource(), GetCareDocumentationResponse.class)
                            .getCareDocumentation().get(0))
                    : diagnosisBuilder.toJson(unmarshal(source.xmlResource(), GetDiagnosisResponse.class)
                            .getDiagnosis().get(0));
            Files.writeString(targetDir.resolve(source.outputFileName()), json, StandardCharsets.UTF_8);
        }

        System.out.println("Skrev " + SOURCES.size() + " JSON-källfiler till " + targetDir.toAbsolutePath());
    }

    private static <T> T unmarshal(String resourcePath, Class<T> type) throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(type);
        Unmarshaller unmarshaller = ctx.createUnmarshaller();
        try (InputStream is = GenerateFmlTestDataTool.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Testmeddelande saknas: " + resourcePath);
            }
            return type.cast(unmarshaller.unmarshal(is));
        }
    }
}
