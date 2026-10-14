package se.inera.ehds.mapping.tools;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import se.inera.ehds.mapping.rivta.caredocumentation.*;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Genererar ett syntetiskt, stort GetCareDocumentationResponse-svar (standard: ~400
 * vårddokument för en patient, ~35-45 MB) för belastnings- och gränstester.
 *
 * Varje dokument har en deterministiskt genererad DocBook-text (rubriker, stycken,
 * punktlistor) på svenska kliniskt färgat fyllnadsinnehåll (ingen LLM, ingen riktig
 * patientdata). Omväxlande läggs texten i clinicalDocumentNoteText (ren DocBook-XML,
 * vanlig XML-textkodning — se DOC-004 i mapping-getcaredocumentation.md) och i
 * multimediaEntry.value (samma DocBook-text, base64-kodad enligt xs:base64Binary) —
 * det XOR-fält i CareDocumentationBodyType som schemat faktiskt definierar för
 * binärt/base64-kodat innehåll.
 *
 * Körs via:
 *   mvn -pl mapping-engine test-compile
 *   mvn -pl mapping-engine dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt
 *   java -cp target/classes:target/test-classes:$(cat /tmp/cp.txt) \
 *        se.inera.ehds.mapping.tools.GenerateLargeGetCareDocumentationSampleTool \
 *        /tmp/out/getcaredocumentation-large-sample.xml 400 9000
 *
 * Argument: [outputPath] [antalDokument] [ordPerDokument]. Två uppmätta varianter som
 * båda ger ~40 MB, bara med olika balans mellan antal och storlek per dokument:
 *   - 380 dokument, 9000 ord/dokument  (standard — längre journalanteckningar)
 *   - 2000 dokument, 1250 ord/dokument (fler, kortare poster)
 * Tidsserien (16 år, 2011-01 till ca 2026-10) sprids alltid över samma period oavsett
 * antal dokument — fler dokument ger tätare mellanrum, inte en längre tidsperiod.
 */
public final class GenerateLargeGetCareDocumentationSampleTool {

    // Region Stockholm (healthcareProvider) / Danderyds sjukhus (careUnit) — riktiga
    // test-HSA-id:n från /mnt/project-files/testdata/test-vardgivare.csv, inte påhittade.
    private static final String HEALTHCARE_PROVIDER_HSA = "SE2321000016";
    private static final String CARE_UNIT_HSA = "SE2321000016-1K2W";
    private static final String CARE_UNIT_NAME = "Danderyds sjukhus";
    private static final String HSA_OID = "1.2.752.129.2.1.4.1";
    private static final String PERSON_OID = "1.2.752.129.2.1.3.1";
    // Fiktivt testpersonnummer, samma skick som i GetCareDocumentationSchemaValidationTest.
    private static final String PATIENT_PERSONNUMMER = "194506069874";
    private static final String NOTE_CODE_OID = "1.2.752.129.2.2.2.11";

    private static final DateTimeFormatter RIV_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final String[][] NOTE_CODES = {
            {"ins", "Inskrivningsanteckning"},
            {"lop", "Löpande journalanteckning"},
            {"utsk", "Utskrivningsanteckning"},
            {"mottagning", "Mottagningsanteckning"},
            {"telefon", "Telefonrådgivning"},
            {"remissvar", "Remissvar"},
    };

    private static final String[] CLINICIAN_NAMES = {
            "Anna Andersson", "Bo Bengtsson", "Cecilia Celander", "David Dahl",
            "Erika Eklund", "Fredrik Forsberg", "Gunilla Gustafsson", "Henrik Hedlund",
            "Ingrid Isaksson", "Johan Jonsson",
    };

    private static final String[] SECTION_TITLES = {
            "Anamnes", "Status", "Bedömning", "Åtgärd", "Uppföljning", "Läkemedel",
            "Provsvar", "Remiss", "Vårdplanering", "Sammanfattning",
    };

    // Liten svensk ordbank för deterministisk, klinikfärgad fyllnadstext. Inga riktiga
    // patientuppgifter — bara vanliga ord/fraser som ger realistisk löptext och längd.
    private static final String[] WORDS = {
            "patienten", "besöker", "mottagningen", "för", "uppföljning", "av", "tidigare",
            "bedömning", "beskriver", "fortsatt", "lindriga", "besvär", "utan", "tecken",
            "till", "försämring", "status", "vid", "undersökningen", "opåverkad", "cirkulatoriskt",
            "och", "respiratoriskt", "stabil", "auskultation", "av", "hjärta", "lungor",
            "utan", "anmärkning", "blodtryck", "och", "puls", "inom", "normala", "referensvärden",
            "patienten", "ordinerades", "fortsatt", "medicinering", "enligt", "tidigare", "plan",
            "återbesök", "planeras", "om", "sex", "veckor", "för", "kontroll", "av", "provsvar",
            "ingen", "ytterligare", "åtgärd", "krävs", "i", "nuläget", "patienten", "informerad",
            "om", "att", "kontakta", "mottagningen", "vid", "nytillkomna", "symtom", "eller",
            "försämring", "journalanteckning", "upprättad", "i", "samråd", "med", "patienten",
            "som", "samtycker", "till", "föreslagen", "vårdplan", "fortsatt", "omhändertagande",
            "sker", "i", "primärvården", "med", "återkoppling", "till", "specialistmottagningen",
            "vid", "behov", "smärtskattning", "genomförd", "utan", "anmärkningsvärda", "fynd",
            "allmäntillstånd", "gott", "orienterad", "till", "tid", "och", "rum", "inga", "nytillkomna",
            "läkemedel", "sedan", "senaste", "besöket", "ingen", "känd", "överkänslighet", "noterad",
    };

    public static void main(String[] args) throws Exception {
        String outputPath = args.length > 0 ? args[0] : "getcaredocumentation-large-sample.xml";
        int documentCount = args.length > 1 ? Integer.parseInt(args[1]) : 380;
        // 9000 ord DocBook-text per dokument ger, tillsammans med header-overhead och att
        // var tredje dokument base64-expanderas (~4/3), en total filstorlek på ~40 MB för
        // 380 dokument (uppmätt).
        int wordsPerDocument = args.length > 2 ? Integer.parseInt(args[2]) : 9_000;

        GetCareDocumentationResponse response = buildResponse(documentCount, wordsPerDocument);

        File outFile = new File(outputPath);
        if (outFile.getParentFile() != null) {
            outFile.getParentFile().mkdirs();
        }

        JAXBContext ctx = JAXBContext.newInstance(GetCareDocumentationResponse.class);
        Marshaller marshaller = ctx.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(outFile), 1 << 20)) {
            marshaller.marshal(response, out);
        }

        long bytes = outFile.length();
        System.out.printf(Locale.ROOT,
                "Skrev %s (%d dokument, %.1f MB) till %s%n",
                "GetCareDocumentationResponse", documentCount, bytes / (1024.0 * 1024.0), outFile.getAbsolutePath());
    }

    // Hela tidsserien spänner ~16 år (2011-01 till ca 2026-10) oavsett antal dokument —
    // fler dokument ger tätare mellanrum snarare än ett längre förlopp.
    private static final long TOTAL_SPAN_DAYS = 16L * 365;

    private static GetCareDocumentationResponse buildResponse(int documentCount, int wordsPerDocument) {
        List<CareDocumentation> documents = new ArrayList<>(documentCount);
        long stepDays = Math.max(1, TOTAL_SPAN_DAYS / documentCount);
        LocalDateTime cursor = LocalDateTime.of(2011, 1, 10, 9, 0, 0);
        for (int i = 0; i < documentCount; i++) {
            documents.add(buildDocument(i, cursor, wordsPerDocument));
            cursor = cursor.plusDays(stepDays).plusHours((i % 5) - 2).plusMinutes((i % 7) * 3);
        }

        ResultType result = new ResultType();
        result.setResultCode("OK");

        GetCareDocumentationResponse response = new GetCareDocumentationResponse();
        response.setCareDocumentation(documents);
        response.setResult(result);
        return response;
    }

    private static CareDocumentation buildDocument(int index, LocalDateTime time, int wordsPerDocument) {
        String timestamp = time.format(RIV_TIMESTAMP);
        String clinicianName = CLINICIAN_NAMES[index % CLINICIAN_NAMES.length];
        String[] noteCodePair = NOTE_CODES[index % NOTE_CODES.length];

        PersonIdType patientId = iid(PERSON_OID, PATIENT_PERSONNUMMER);
        PersonIdType providerHsa = iid(HSA_OID, HEALTHCARE_PROVIDER_HSA);
        PersonIdType careUnitHsa = iid(HSA_OID, CARE_UNIT_HSA);

        AccessControlHeader ach = new AccessControlHeader();
        ach.setAccountableHealthcareProvider(providerHsa);
        ach.setAccountableCareUnit(careUnitHsa);
        ach.setPatientId(patientId);
        ach.setCareProcessId(iid(HSA_OID, CARE_UNIT_HSA + "-process-" + (index / 20)));
        ach.setBlockComparisonTime(timestamp);
        ach.setApprovedForPatient(true);

        RecordType record = new RecordType();
        record.setId(iid(CARE_UNIT_HSA, "rec-" + (index + 1)));
        record.setTimestamp(timestamp);

        Author author = new Author();
        author.setId(careUnitHsa);
        author.setName(clinicianName);
        author.setTimestamp(timestamp);

        Signature signature = new Signature();
        signature.setId(careUnitHsa);
        signature.setName(clinicianName);
        signature.setTimestamp(timestamp);

        Header header = new Header();
        header.setAccessControlHeader(ach);
        header.setSourceSystemId(careUnitHsa);
        header.setRecord(record);
        header.setAuthor(author);
        header.setSignature(signature);

        CVType noteCode = new CVType();
        noteCode.setCode(noteCodePair[0]);
        noteCode.setCodeSystem(NOTE_CODE_OID);
        noteCode.setDisplayName(noteCodePair[1]);

        String title = noteCodePair[1] + " – " + CARE_UNIT_NAME + " (" + index + ")";
        String docBook = buildDocBook(index, title, wordsPerDocument);

        Body body = new Body();
        body.setClinicalDocumentNoteCode(noteCode);
        body.setClinicalDocumentNoteTitle(title);

        // Var tredje dokument läggs som base64-kodad multimediaEntry (det schema-giltiga
        // base64-fältet), övriga som vanlig entitykodad clinicalDocumentNoteText.
        if (index % 3 == 0) {
            MultimediaEntry media = new MultimediaEntry();
            media.setMediaType("application/xml");
            media.setValue(Base64.getEncoder().encodeToString(docBook.getBytes(StandardCharsets.UTF_8)));
            body.setMultimediaEntry(media);
        } else {
            body.setClinicalDocumentNoteText(docBook);
        }

        CareDocumentation doc = new CareDocumentation();
        doc.setHeader(header);
        doc.setBody(body);
        return doc;
    }

    /** Deterministisk DocBook-text: artikel med flera sektioner, stycken och en punktlista. */
    private static String buildDocBook(int seed, String title, int wordCount) {
        Random random = new Random(seed * 31L + 17L);
        StringBuilder xml = new StringBuilder(wordCount * 8);
        xml.append("<article>");
        xml.append("<title>").append(escape(title)).append("</title>");

        int sectionCount = SECTION_TITLES.length;
        int wordsPerSection = Math.max(20, wordCount / sectionCount);
        for (String sectionTitle : SECTION_TITLES) {
            xml.append("<section><title>").append(escape(sectionTitle)).append("</title>");
            int remaining = wordsPerSection;
            while (remaining > 0) {
                int sentenceWords = Math.min(remaining, 8 + random.nextInt(10));
                xml.append("<para>").append(sentence(random, sentenceWords)).append("</para>");
                remaining -= sentenceWords;
            }
            xml.append("<itemizedlist>");
            int items = 3 + random.nextInt(3);
            for (int i = 0; i < items; i++) {
                xml.append("<listitem><para>").append(sentence(random, 5 + random.nextInt(6)))
                        .append("</para></listitem>");
            }
            xml.append("</itemizedlist>");
            xml.append("</section>");
        }
        xml.append("</article>");
        return xml.toString();
    }

    private static String sentence(Random random, int wordCount) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < wordCount; i++) {
            if (i > 0) sb.append(' ');
            String word = WORDS[random.nextInt(WORDS.length)];
            sb.append(i == 0 ? capitalize(word) : word);
        }
        sb.append('.');
        return escape(sb.toString());
    }

    private static String capitalize(String word) {
        return word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static PersonIdType iid(String root, String extension) {
        PersonIdType id = new PersonIdType();
        id.setRoot(root);
        id.setExtension(extension);
        return id;
    }

    private GenerateLargeGetCareDocumentationSampleTool() {
    }
}
