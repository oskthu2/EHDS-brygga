package se.inera.ehds.fml;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import se.inera.ehds.mapping.naming.NamingSystemRegistry;
import se.inera.ehds.mapping.rivta.Diagnosis;
import se.inera.ehds.mapping.rivta.GetDiagnosisResponse;
import se.inera.ehds.mapping.rivta.caredocumentation.CareDocumentation;
import se.inera.ehds.mapping.rivta.caredocumentation.GetCareDocumentationResponse;

import org.hl7.fhir.r4.model.Base;

import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

/**
 * Interaktiv testklient för FML-spåret: välj en SOAP-källa/testmeddelande, se den laddas in
 * mot sin publicerade logiska modell (via *JsonSourceBuilder + {@link FmlEngine#parseSource}),
 * välj en FML-mappning ur {@link FmlEngine}s registry, och köra antingen {@code $transform}
 * (hela mappningen, resulterande FHIR-resurs skriven som JSON) eller {@code $evaluate}
 * (ett valfritt FHIRPath-uttryck mot den inlästa källan, för att inspektera enskilda fält –
 * samma mekanism som {@code stopIfFalseFhirPath}-vakten i registryt använder).
 *
 * Körs med: {@code mvn -pl fml-mapping-poc exec:java
 * -Dexec.mainClass=se.inera.ehds.fml.FmlTestClient}
 *
 * De bundlade testmeddelandena under {@code src/main/resources/testmessages/} är riktiga,
 * schema-giltiga SOAP-svar (marshallade från samma JAXB-klasser som produktionskoden
 * använder, se {@link se.inera.ehds.fml.testmessages.GenerateTestMessagesTool}), inte
 * handskriven XML.
 */
public final class FmlTestClient {

    private enum ServiceContract {
        GET_DIAGNOSIS, GET_CARE_DOCUMENTATION
    }

    private record SoapTestMessage(String label, String xmlResource, ServiceContract contract) {
    }

    private static final List<SoapTestMessage> TEST_MESSAGES = List.of(
            new SoapTestMessage("GetDiagnosis – Huvuddiagnos, kronisk, giltigt personnummer",
                    "/testmessages/get-diagnosis-huvuddiagnos.xml", ServiceContract.GET_DIAGNOSIS),
            new SoapTestMessage("GetDiagnosis – Bidiagnos, OGILTIGT personnummer (stoppar resursen)",
                    "/testmessages/get-diagnosis-ogiltigt-personnummer.xml", ServiceContract.GET_DIAGNOSIS),
            new SoapTestMessage("GetCareDocumentation – platt fritext",
                    "/testmessages/get-caredocumentation-plattfritext.xml", ServiceContract.GET_CARE_DOCUMENTATION),
            new SoapTestMessage("GetCareDocumentation – multimediabilaga",
                    "/testmessages/get-caredocumentation-multimedia.xml", ServiceContract.GET_CARE_DOCUMENTATION)
    );

    public static void main(String[] args) throws Exception {
        Scanner in = new Scanner(System.in);
        FmlEngine engine = new FmlEngine();
        NamingSystemRegistry namingSystem = new NamingSystemRegistry();
        GetDiagnosisJsonSourceBuilder diagnosisBuilder = new GetDiagnosisJsonSourceBuilder(namingSystem);
        GetCareDocumentationJsonSourceBuilder careDocBuilder = new GetCareDocumentationJsonSourceBuilder(namingSystem);

        while (true) {
            System.out.println();
            System.out.println("=== FML-testklient: EHDS-brygga ===");
            System.out.println("Välj SOAP-källa / testmeddelande (0 för att avsluta):");
            for (int i = 0; i < TEST_MESSAGES.size(); i++) {
                System.out.printf("  %d) %s%n", i + 1, TEST_MESSAGES.get(i).label());
            }
            int messageChoice = readChoice(in, 0, TEST_MESSAGES.size());
            if (messageChoice == 0) {
                break;
            }
            SoapTestMessage message = TEST_MESSAGES.get(messageChoice - 1);

            String logicalModelResource = message.contract() == ServiceContract.GET_DIAGNOSIS
                    ? "/fhir/lm-diagnosis.json" : "/fhir/lm-caredocumentation.json";
            Base source = loadSource(message, engine, diagnosisBuilder, careDocBuilder, logicalModelResource);

            System.out.println();
            System.out.println("Laddat SOAP-meddelande och tjänstekontraktets logiska modell ("
                    + logicalModelResource + ") – källan är nu ett riktigt FML-element.");

            mappingLoop(in, engine, source, logicalModelResource);
        }
        System.out.println("Avslutar.");
    }

    private static Base loadSource(SoapTestMessage message, FmlEngine engine,
                                    GetDiagnosisJsonSourceBuilder diagnosisBuilder,
                                    GetCareDocumentationJsonSourceBuilder careDocBuilder,
                                    String logicalModelResource) throws Exception {
        if (message.contract() == ServiceContract.GET_DIAGNOSIS) {
            GetDiagnosisResponse response = unmarshal(message.xmlResource(), GetDiagnosisResponse.class);
            Diagnosis diagnosis = response.getDiagnosis().get(0);
            String json = diagnosisBuilder.toJson(diagnosis);
            return engine.parseSource(logicalModelResource, json);
        } else {
            GetCareDocumentationResponse response =
                    unmarshal(message.xmlResource(), GetCareDocumentationResponse.class);
            CareDocumentation entry = response.getCareDocumentation().get(0);
            String json = careDocBuilder.toJson(entry);
            return engine.parseSource(logicalModelResource, json);
        }
    }

    private static void mappingLoop(Scanner in, FmlEngine engine, Base source, String logicalModelResource) {
        List<FmlEngine.MappingDefinition> mappings = engine.mappingsForSource(logicalModelResource);
        while (true) {
            System.out.println();
            System.out.println("Tillgängliga FML-mappningar för denna källa (0 för att byta testmeddelande):");
            for (int i = 0; i < mappings.size(); i++) {
                FmlEngine.MappingDefinition def = mappings.get(i);
                System.out.printf("  %d) %s -> %s%n", i + 1, def.key(), def.targetResourceType());
            }
            int mappingChoice = readChoice(in, 0, mappings.size());
            if (mappingChoice == 0) {
                return;
            }
            actionLoop(in, engine, mappings.get(mappingChoice - 1), source);
        }
    }

    private static void actionLoop(Scanner in, FmlEngine engine, FmlEngine.MappingDefinition def, Base source) {
        while (true) {
            System.out.println();
            System.out.println("Vald mappning: " + def.key() + " (" + def.groupName() + " -> "
                    + def.targetResourceType() + ")");
            System.out.println("  1) $transform – köra mappningen och visa resulterande FHIR");
            System.out.println("  2) $evaluate – evaluera ett FHIRPath-uttryck mot källan");
            System.out.println("  0) Tillbaka till mappningsval");
            int action = readChoice(in, 0, 2);
            if (action == 0) {
                return;
            } else if (action == 1) {
                runTransform(engine, def, source);
            } else {
                runEvaluate(engine, in, source);
            }
        }
    }

    private static void runTransform(FmlEngine engine, FmlEngine.MappingDefinition def, Base source) {
        System.out.println();
        try {
            Base result = engine.transform(def.key(), source);
            if (result == null) {
                System.out.println("$transform gav INGEN resurs – stoppa-resursen-vakten ("
                        + def.stopIfFalseFhirPath() + ") utvärderade falskt mot källan.");
            } else {
                System.out.println("$transform -> " + def.targetResourceType() + ":");
                System.out.println(engine.toJson(result));
            }
        } catch (Exception e) {
            System.out.println("Fel vid $transform: " + e.getMessage());
        }
    }

    private static void runEvaluate(FmlEngine engine, Scanner in, Base source) {
        System.out.print("FHIRPath-uttryck att evaluera mot källan (t.ex. "
                + "\"diagnosis.diagnosisHeader.patientId.value\"): ");
        String expr = in.nextLine().trim();
        if (expr.isEmpty()) {
            return;
        }
        try {
            List<Base> results = engine.evaluate(source, expr);
            System.out.println("$evaluate(\"" + expr + "\") -> " + results.size() + " resultat:");
            for (Base b : results) {
                System.out.println("  " + b.fhirType() + ": " + (b.isPrimitive() ? b.primitiveValue() : b));
            }
        } catch (Exception e) {
            System.out.println("Fel vid $evaluate: " + e.getMessage());
        }
    }

    private static <T> T unmarshal(String resourcePath, Class<T> type) throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(type);
        Unmarshaller unmarshaller = ctx.createUnmarshaller();
        try (InputStream is = FmlTestClient.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalStateException("Testmeddelande saknas: " + resourcePath);
            }
            return type.cast(unmarshaller.unmarshal(is));
        }
    }

    private static int readChoice(Scanner in, int min, int max) {
        while (true) {
            System.out.print("> ");
            if (!in.hasNextLine()) {
                return min;
            }
            String line = in.nextLine().trim();
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // falls through to the error message below
            }
            System.out.println("Ange ett tal mellan " + min + " och " + max + ".");
        }
    }
}
