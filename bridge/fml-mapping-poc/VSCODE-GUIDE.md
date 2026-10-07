# Jobba med .map-filerna i VS Code (fhir-mapbuilder-pluginet)

Den här guiden beskriver hur man redigerar och validerar FML (`.map`-filerna i
`src/main/resources/fml/`) interaktivt i VS Code, med
[aphp/fhir-mapbuilder](https://github.com/aphp/fhir-mapbuilder) som editor-stöd
och live-validering. Den ersätter inte `FmlTestClient` (se `pom.xml` /
`exec:exec`) – klienten är fortfarande vägen in om man vill köra alla fyra
mappningarna i följd från terminalen. Guiden här är till för själva
redigeringsarbetet: autocomplete, syntax-markering och att se resultatet av en
ändring direkt, fil för fil.

## 1. Förutsättningar

- **Java 21** på PATH (samma som bygget kräver).
- **VS Code**.
- **git**, med en lokal klon av detta repo, på grenen du jobbar på (just nu
  `claude/fml-mapping-9ovqyh`).

## 2. Installera pluginet

Sök efter **fhir-mapbuilder** (publicerad av APHP) i VS Code Marketplace eller
Open VSX och installera det som vanligt. Den publicerade versionen har redan
valideringsmotorn (en Java/Spring Boot-process som startas åt dig i
bakgrunden) inbyggd – man behöver **inte** klona eller bygga
`aphp/fhir-mapbuilder` själv.

## 3. Bygg testpaketet pluginet validerar mot

Pluginet validerar en `.map`-fil mot ett FHIR-paket (`output/package.tgz`) som
innehåller de resurser mappningen refererar till – i vårt fall de två logiska
källmodellerna (`inera-ehds-lm-diagnosis`, `inera-ehds-lm-care-documentation`)
och de två ConceptMaps `translate()`-anropen använder. Kör:

```bash
./scripts/build-fml-test-package.sh
```

Detta skriver `bridge/fml-mapping-poc/output/package.tgz` från de resurser som
redan finns i `src/main/resources/fhir/`. Kör skriptet igen varje gång någon av
dessa fyra filer ändras (`lm-diagnosis.json`, `lm-caredocumentation.json`,
`conceptmap-diagnosis-type.json`, `conceptmap-codesystem-oid.json`).

`output/` är gitignorad – `package.tgz` är ett byggresultat, inte något som
checkas in.

## 4. Öppna rätt mapp som workspace-root

**Viktigt:** pluginet letar alltid efter `output/package.tgz` relativt den
*första* mappen i VS Code-workspacet (hårdkodat i källkoden, går inte att
konfigurera om). Öppna därför **`bridge/fml-mapping-poc`** som workspace-root
i VS Code – inte repo-roten och inte `bridge/`:

```bash
code bridge/fml-mapping-poc
```

Öppnar man fel mapp hittar pluginet inte paketet och klagar på att
`output/package.tgz` saknas.

## 5. Redigera en .map-fil

`.map`-filerna ligger i `src/main/resources/fml/`:

- `get-diagnosis-to-condition.map`
- `get-diagnosis-to-provenance.map`
- `get-caredocumentation-to-documentreference.map`
- `get-caredocumentation-to-provenance.map`

Öppna någon av dem – pluginet ger syntaxmarkering och autocomplete för FML.

**Kommentarstil:** använd `//`, inte `///`. Vår egen motor
(`org.hl7.fhir.r4.utils.StructureMapUtilities`) tolererar båda, men
mapbuilder-pluginets parser (R5-baserad, `matchbox-engine`) tolkar `///` som
reserverad metadata-syntax och kraschar på fritext efter den. Det här är redan
fixat i alla fyra filerna ovan (commit `32ffde9`) – bara något att komma ihåg
om man lägger till en ny `.map`-fil eller kopierar från spiken i
`src/test/resources/fml/lm-diagnosis-spike.map` (som fortfarande använder
`///` eftersom den aldrig körs genom pluginet).

## 6. Testdata att validera mot

Fyra färdiga JSON-källfiler – en instans av respektive logisk källmodell per
bundlat SOAP-testmeddelande – ligger i `fml-test-data/`:

- `get-diagnosis-huvuddiagnos.json`
- `get-diagnosis-ogiltigt-personnummer.json`
- `get-caredocumentation-plattfritext.json`
- `get-caredocumentation-multimedia.json`

De genereras av `GenerateFmlTestDataTool` och är redan incheckade, så man
behöver inte bygga något för att komma igång. Om testmeddelandena i
`src/main/resources/testmessages/` eller JSON-byggarna
(`GetDiagnosisJsonSourceBuilder` / `GetCareDocumentationJsonSourceBuilder`)
ändras, regenerera med:

```bash
mvn -pl fml-mapping-poc test-compile exec:java \
  -Dexec.mainClass=se.inera.ehds.fml.testmessages.GenerateFmlTestDataTool \
  -Dexec.classpathScope=test
```

## 7. Validera

Öppna kommandopaletten (Cmd/Ctrl+Shift+P) i en `.map`-fil och kör:

- **"Load current package and Validate StructureMap"** – laddar om
  `output/package.tgz` och validerar den öppna filen mot senast valda
  testdata (eller ber dig välja första gången).
- **"Validate StructureMap (With input selection)"** – låter dig välja en
  JSON-fil från `fml-test-data/` innan valideringen körs.

Resultatet hamnar i en `fml-generated`-mapp bredvid `.map`-filen, med tre
filtyper per körning:

- `*_result.json` – den resulterande FHIR-resursen (vid lyckad transform).
- `*_error.json` – felmeddelande (t.ex. om en ConceptMap eller logisk modell
  inte kunde slås upp – kontrollera då steg 3, att paketet är nybyggt).
- `*_params.log` – de parametrar/anrop som skickades till motorn.

Jämför `*_result.json` mot vad `FmlTestClient` eller befintliga
jämförelsetester (`*ComparisonTest`) producerar för samma indata – de ska
vara identiska, pluginet kör samma FML-regler, bara via en annan
(R5/matchbox-baserad) motor.

## 8. Git-arbetsflöde

Inget speciellt jämfört med övriga repot:

```bash
git checkout claude/fml-mapping-9ovqyh   # eller skapa en ny gren från den
# ... redigera .map-filer, kör validering i VS Code tills resultatet ser rätt ut ...
git add bridge/fml-mapping-poc/src/main/resources/fml/<filen>.map
git commit -m "Beskriv ändringen"
git push -u origin claude/fml-mapping-9ovqyh
```

Kör gärna hela testsviten innan push, så att ändringen i `.map`-filen inte bara
validerar i pluginet utan också stämmer med jämförelsetesterna i
`fml-mapping-poc`:

```bash
cd bridge && mvn test -pl mapping-engine,fml-mapping-poc
```

## 9. Felsökning

- **"different API token" / porten är upptagen** – en annan instans av
  valideringsbackend:en (eller en gammal process) kör redan på samma port.
  Stäng den gamla VS Code-sessionen/processen och försök igen.
- **Valideringen är långsam eller kraschar med minnesfel** – höj heapen via
  inställningen `FhirMapBuilder.javaVmArgs` (t.ex. `-Xmx2g`) i VS Code
  settings.
- **"There is no output\\package.tgz file in this project!"** – antingen är
  fel mapp öppnad som workspace-root (steg 4) eller så har
  `build-fml-test-package.sh` inte körts än (steg 3).
- **Parserfel om "Found '-' expecting '='" eller liknande kring `///`** – en
  `.map`-fil använder fortfarande tripel-slash-kommentarer, se steg 5.
