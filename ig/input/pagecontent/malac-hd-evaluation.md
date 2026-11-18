# Utvärdering: MaLaC-HD som mappningsmotor (FML → Python)

Den här sidan hör till en egen gren (`claude/project-thread-fow2p9`), inte till
produktionsbryggan. Den Java-baserade mapping-engine som bryggan faktiskt kör är opåverkad.
Syftet var att svara på en annan konkret fråga än den `fml-mapping-poc` redan besvarat
(se `fml-evaluation.md`): kan [MaLaC-HD](https://pypi.org/project/malac-hd/) – en FML-
**kompilator** som genererar fristående Python-kod, i stället för en motor som tolkar
`.map`-filen vid varje anrop – ge samma eller bättre resultat, och hur ser Oskars tänkta
pipeline (tjänstekontraktsspecifik FML + ett delat FML-bibliotek av RIVTA-byggstenar →
StructureMap/ConceptMap → MaLaC-HD → genererad kod) ut i praktiken?

Allt arbete ligger i `bridge/malac-hd-mapping-poc/` – ett eget Maven-modul vars enda uppgift
är att jämföra; all faktisk FML/Python-mappning ligger under dess `python/`-katalog, inte i
Java. `mapping-engine` är orörd.

**Uppdatering (samma gren, samma PR, en andra arbetsomgång):** avsnitten nedan fram till
"Feasibility-bedömning" beskriver den URSPRUNGLIGA PoC:n – MaLaC-HD ensamt, mot en egen
`generateDS`-genererad RIVTA-modell. En andra fråga besvarades sedan på samma gren: kan den
EXAKT SAMMA `.map`-filen köras av en ANNAN motor också – HAPI FHIR:s egen
`StructureMapUtilities` (Java), samma motor som `fml-mapping-poc`/PR #39 redan kör – så att
FML-filen själv blir en genuint delad, portabel artefakt mellan de två spåren, inte två
isärglidande filuppsättningar? Se "Den enade källmodellen" och framåt för den delen; de nya
avsnitten ersätter inget ovan utan bygger vidare på det.

## Vad som byggdes

- **`python/model/`** – `rivta_getdiagnosis_model.py`, genererad direkt från de vendored RIVTA-
  XSD:erna med [generateDS](https://pypi.org/project/generateDS/) 2.44.3 (`generate-model.sh`),
  plus `model_plugin.py` som registrerar den som MaLaC-HD:s källmodell via mekanismen
  `list_i_o_modules`.
- **`python/fml/lib-*.map`** – ett delat bibliotek av RIVTA-byggstenar, EN fil per datatyp,
  tänkt att vara oberoende testbara och återanvändningsbara av en framtida
  GetCareDocumentation-mappning: `lib-ii-type.map`, `lib-person-id-type.map`, `lib-cv-type.map`,
  `lib-timestamp-type.map`, `lib-hsa-reference.map`, `lib-org-unit-type.map`,
  `lib-healthcare-professional-type.map`, `lib-legal-authenticator-type.map`. Se
  "Det delade biblioteket i praktiken" nedan för vad som faktiskt gick att återanvända.
- **`python/fml/get-diagnosis-to-condition.map`** – GetDiagnosis → `Condition`, byggd direkt mot
  en genererad Python-modell av hela RIVTA-XSD:n (ingen logisk modell författad), exhaustiv mot
  schemat (se tabellen nedan).
- **`python/fml/get-diagnosis-to-provenance.map`** – GetDiagnosis → `Provenance` (tre agenter:
  custodian/author/assembler), som en separat StructureMap mot samma källa.
- **`python/conceptmaps/diagnosis-type.json`** och **`codesystem-oid.json`** – motsvarar
  `concept-maps/diagnosis-type.yaml` respektive `naming-systems.yaml`.
- **`python/preprocess.py`** – pre-processar RIVTA-XML:en innan MaLaC-HD ser den: GENERAL-001
  (tidszonskonvertering) och OID→URI/meta.source-strängbygget. Se "Varför Python, inte FML, för
  det här" nedan.
- **`python/guard.py`** – DIAG-001 och de två andra posttäckande vakterna (icke-OK
  `resultCode`, saknad header/body), körda INNAN någon transform anropas.
- **`python/run_pipeline.py`** – orkestrerar preprocess → vakt → båda transformerna → eftersnickrar
  `Provenance.target`/`Provenance.recorded`, en gång per `<diagnosis>`-post.
- **`src/main/java/.../JavaMapperDump.java`** + **`JavaMapperDumpTest.java`** – dumpar
  `GetDiagnosisMapper.java`s FAKTISKA, oförändrade utdata för samma två testmeddelanden till
  JSON (`target/java-mapper-dump/*.json`).
- **`compare/run_comparison.py`** – diffar de två sidorna fält för fält (med slumpmässiga
  UUID:er normaliserade bort) och rapporterar varje skillnad.

Kör allt med:
```bash
cd bridge && mvn test -pl malac-hd-mapping-poc -am   # genererar Java-sidans dump
cd bridge/malac-hd-mapping-poc && python3 compare/run_comparison.py
```

## Varför Python, inte Java – och varför ingen logisk modell behövdes

`fml-mapping-poc` (HAPI-motorn) körs i JVM:n och kräver att käll- och målstrukturen är en FHIR-
resurs eller en "logisk modell" (`StructureDefinition` med `kind=logical`) – RIVTA:s JAXB-
klasser är inte det, och den PoC:n löste det genom att platta ut till `Parameters` eller
navigera en författad logisk modell (se `fml-evaluation.md`, "Källdata är inte FHIR").

MaLaC-HD löser samma problem helt annorlunda: det **kompilerar** `.map`-filen till en fristående
Python-fil, och källmodellen behöver bara vara EN GILTIG PYTHON-KLASS – inte en FHIR-resurs,
inte en logisk modell, inte något HAPI-specifikt alls. MaLaC-HD:s egna inbyggda FHIR R4/R5-
modellpaket (`malac.models.fhir.r4`/`r5`) visade sig själva vara `generateDS`-genererad kod.
Det innebar att RIVTA-XSD:erna kunde köras genom SAMMA generator direkt
(`python/model/generate-model.sh` → `rivta_getdiagnosis_model.py`, 7796 rader, en klass per
XSD-`complexType`) och registreras som källmodell via en liten plugin-modul
(`model_plugin.py`, `list_i_o_modules`-mekanismen). **Ingen logisk modell författades, och ingen
JSON-adapter mellan RIVTA och en mellanrepresentation skrevs** – `.map`-filen navigerar
`diagnosis.diagnosisHeader.patientId.id` m.fl. direkt mot RIVTA:s egna fältnamn och nästling,
exakt som HAPI-spåret bara uppnådde genom att hämta en redan publicerad logisk modell utifrån.
Detta är den tydligaste positiva skillnaden mot `fml-mapping-poc`: samma "slippa adapterkod"-
löfte som FML brukar säljas in med, uppnått utan att någon logisk modell eller adapter behövde
skrivas för hand alls.

Priset: utdata är Python, inte en FHIR-resurs direkt – en genererad fristående `.py`-fil som
tar `-s <källfil> -t <målfil> -st/-tt xml/json` på kommandoraden och skriver FHIR-JSON. Att
anropa den från den riktiga Java-bryggan skulle alltså kräva ett process- eller RPC-gränssnitt
(exakt det `run_pipeline.py` gör via `subprocess` i den här PoC:n), inte ett rakt metodanrop –
en genuin arkitektonisk skillnad mot `fml-mapping-poc`, som kör i samma JVM som
`fhir-server`/`ntjp-proxy` redan gör.

## Det delade biblioteket i praktiken

Uppgiften bad specifikt om ett delat bibliotek av RIVTA-byggstenar FÖRST, så att en framtida
GetCareDocumentation-mappning kan återanvända dem. Det gick att bygga (`lib-*.map`, en fil per
datatyp), men två motorbegränsningar (se punkt 2 och 6 nedan) gjorde att **själva
`get-diagnosis-to-condition.map`/`get-diagnosis-to-provenance.map` i slutändan INTE anropar
biblioteksfilerna via `imports`** – varje regel som i princip är "samma mönster som
`lib-hsa-reference.map`" är i stället inlinead direkt i den anropande filen, med en kommentar
som pekar på vilken biblioteksfil den speglar och varför den inte kallas direkt. Biblioteket
fungerar alltså som **dokumentation av mönstret och ett oberoende testbart referensexempel**
(`lib-healthcare-professional-type.map`s `HealthcareProfessionalToOrgUnitReference`-grupp är
bekräftat körbar fristående och gavs rätt utdata i isolerat test), men inte som bokstavligt
återanvänd kod i den levererade mappningen – en konkret, ärlig begränsning av "bygg biblioteket
först"-planen för DEN HÄR motorversionen.

## Varför Python, inte FML, för GENERAL-001 och OID→URI

Två saker flyttades medvetet ut ur `.map`-filerna och in i `preprocess.py`, av två olika skäl:

- **GENERAL-001 (tidszon)**: precis som i `fml-mapping-poc` har FML/FHIRPath ingen
  tidszonsdatabas – `RivDateParser.parse()`/`parseInstant()`s Europe/Stockholm-logik måste
  köras i värdspråket oavsett motor. Samma slutsats, oberoende återbekräftad för MaLaC-HD.
- **OID→URI (`patientId.type`, `diagnosisCode.codeSystem`) och `meta.source`-strängbygget**:
  dessa GICK att uttrycka i FML självt (`translate()` mot `codesystem-oid`-ConceptMap:en för
  det mappade fallet, `&`-konkatenering för `urn:oid:`-fallbacken och för
  Endpoint-URL-bygget) – `lib-person-id-type.map`/`lib-cv-type.map` visar det mappade fallet
  fungerande. Men en konfirmerad motorbugg (punkt 7 nedan) gör att `&` tyst tappar sin högra
  operand när den kommer från den egna (icke-FHIR) RIVTA-modellen, vilket gör
  `urn:oid:`-fallbacken obrukbar i FML för den här motorversionen. Lösning: hela OID→URI-
  slagningen (mappad OCH fallback) samt Endpoint-URL-bygget flyttades till `preprocess.py`,
  som en ren, väldokumenterad Python-funktion i stället för en känd trasig FML-konstruktion.

## Täckning – vad är faktiskt översatt till FML

**GetDiagnosis → Condition + Provenance: samtliga fält/regler exhaustivt översatta, plus EN
genuin tillägg utöver nuvarande Java-mapper.** clinicalStatus, verificationStatus, category
(Huvuddiagnos/Bidiagnos via ConceptMap, data-absent-reason=unknown-fallback), code (inkl.
text-fallback till displayName när originalText saknas, matchar `GetDiagnosisMapper.java`
exakt), subject (logisk identifierarreferens, GENERAL-002), onset, meta.source, meta.profile
(båda de fasta profil-URL:erna), recorder/recordedDate, asserter/assertedDate-extension,
chronicDiagnosis-extension, relatedDiagnosis-extension (0..*), och Provenance (tre agenter:
custodian/author/assembler). `abatement`/period finns inte i det riktiga RIVTA-schemat
(`diagnosisTime` är en enda instant) och är därför korrekt INTE satt, inte en lucka.

**Genuin tillägg**: `documentId` → `Condition.identifier`. Det riktiga XSD-schemat har fältet
(`DiagnosisHeader.documentId`), men `GetDiagnosisMapper.java` mappar det inte i dag – denna
exhaustiva FML-genomgång gjorde det, eftersom uppgiften bad om exhaustiv täckning mot XSD:n.
Det är den ENDA skillnaden som `compare/run_comparison.py` hittar mellan de två mapparnas
utdata för samma indata (se nästa avsnitt).

| Fält/regel | Status | Kommentar |
|---|---|---|
| `documentId` → `Condition.identifier` | Mappat i FML, INTE i Java | Genuin utökning, flaggad ovan |
| `careContactId`, `nullified`, `nullifiedReason` | Inte mappade | Öppna frågor även i Java – `DiagnosisHeader.java` har fälten men `GetDiagnosisMapper.java` läser dem inte, ingenting beslutat att mappa mot |
| `approvedForPatient` | Inte mappat | Samma öppna fråga som i `fml-evaluation.md` (PDL-001) – inget beslutat FHIR-kodverk |
| VG-scope-filter (`ctx.getRequestedVgHsaId()`) | Inte implementerat | Bryggdeployment-kontext, ingen RIVTA/FHIR-mappningsfråga – medvetet utanför scope |

## Konkreta skillnader mot `GetDiagnosisMapper.java` (verifierat av `run_comparison.py`)

Körning mot båda testmeddelandena (giltig huvuddiagnos, ogiltigt personnummer), med
slumpmässiga `Condition.id`/`Provenance.id`/`urn:uuid:`-referenser normaliserade bort:

- **`get-diagnosis-ogiltigt-personnummer.xml`: identiska (båda tomma listor).** DIAG-001-
  vakten (`guard.py`) stoppar posten precis som `GetDiagnosisMapper.mapDiagnosis()` gör.
- **`get-diagnosis-huvuddiagnos.xml`: EN skillnad.** `condition.identifier` finns i Python-
  utdata (`[{"value": "doc-1"}]`), saknas i Java – se "genuin tillägg" ovan. Allt annat fält för
  fält identiskt: `meta.source`, `meta.profile` (båda URL:erna), alla extensions
  (chronicDiagnosis/relatedDiagnosis/assertedDate, jämförda ordningsoberoende), category,
  code (inkl. `text`-fallbacken), subject, onset, recordedDate, recorder, asserter, och hela
  Provenance (tre agenter + target + recorded).

## Konkreta motorbegränsningar/buggar som hittades under arbetet

Dessa upptäcktes genom att faktiskt köra MaLaC-HD 1.10.0 mot riktiga testfall, inte genom att
läsa specifikationen – samma anda som `fml-evaluation.md`s motsvarande avsnitt, en annan
motorimplementation.

1. **`imports` söker bara i SAMMA katalog som den kompilerande filen.** Upptäcktes när
   `fml/lib/`-underkatalogen (som uppgiften ursprungligen föreslog) aldrig hittades –
   `malac/hd/core/fhir/r5/generator/structuremap.py` listar filer i den kompilerande filens
   EGEN katalog (`os.listdir(dirname)`), inte rekursivt eller mot en sökväg i `.map`-filens
   egen `uses`/`imports`-URL. Löst genom att lägga alla `.map`-filer platt i en katalog
   (`python/fml/`) med ett `lib-`-prefix som namnkonvention i stället för underkataloger.
2. **Att kombinera `imports` med mer än en `-sr`-ConceptMap över FLERA filer kraschar med
   `KeyError('@node')`.** `SupportingResource.parse()` i `malac/hd/resources.py` muterar sin
   cachade resursdict på plats (`dct['@node'] = fhir_res`) och kraschar på den ANDRA
   parsningen av SAMMA resurs – bekräftat oberoende av om den importerade filen själv anropar
   `translate()` eller inte. Löst genom att hålla `get-diagnosis-to-condition.map` helt
   självständig (ingen `imports`), med den delade logiken inlineead i stället (se "Det delade
   biblioteket i praktiken" ovan).
3. **Inline `conceptmap { ... }`-block i en `.map`-fil parsas men implementeras inte i
   kodgenereringen.** Kompilerar utan fel, men den genererade dictionaryn
   (`conceptMap_as_7dimension_dict`) blir tom – endast externa `-sr`-JSON-resurser fungerar i
   praktiken i den här versionen.
4. **`where()` måste skrivas i sin BARA/implicita `$this`-form – att återkvalificera med
   "as"-aliaset ger ett villkor som tyst ALDRIG matchar, för både den positiva och den negerade
   regeln.** `src.type as t where t.matches(pattern)` utvärderas alltid falskt (verifierat:
   utdata saknade hela fältet, inget fel kastades); `src.type as t where matches(pattern)`
   (bar form) fungerar korrekt. Samma mönster drabbade även en navigering en nivå djupare
   (`dCode2.displayName` i en `where`-skyddad regel, se punkt 5).
5. **En höger-ledsnavigering (`x.field`) av en nyss "as"-bunden, egen-modell-variabel, PÅ SAMMA
   RADEN som en `where()`-vakt, tappar tyst navigeringen och tilldelar hela objektet i
   stället.** `dCode as dCode2 where originalText.empty() -> tgt.text = dCode2.displayName`
   kompilerar till `codeCc.text = ...dCode2.value...` – `.displayName` försvinner helt ur den
   genererade koden. Löst genom att flytta navigeringen till en EGEN nästlad regel i ett
   `then{}`-block i stället för att skriva den på samma rad som `where()`.
6. **`&`-strängkonkatenering tappar tyst sin HÖGRA operand när den kommer från den egna
   (icke-FHIR) käll-modellen.** `('urn:oid:' & customModelVar)` kompilerar till
   `fhirpath_utils.concat(['urn:oid:'], fhirpath_utils.get(None, 'customModelVar'))` – andra
   argumentet till `get()` är `None` i stället för källobjektet, så resultatet blir alltid
   `'urn:oid:'` med OID:t saknat. Bekräftat med TRE isolerade test: fungerar med en riktig
   FHIR-primitiv som vänster- ELLER högeroperand (`HumanName.family` gav `"prefix-Svensson"`
   korrekt), men inte med en RIVTA-modell-sourcad variabel oavsett ordning. Löst genom att
   flytta HELA OID→URI-slagningen (mappad och fallback) till `preprocess.py` (se ovan) – ingen
   `&`-konkatenering med en RIVTA-variabel finns kvar i någon `.map`-fil.
7. **En variabel som "as"-bundits i ETT steg men används som värde DIREKT (utan en FÄRSK
   "as"-ombindning precis vid användningsplatsen) tilldelas OINSLAGEN – samma symptom oavsett
   om det sker över en gruppanropsgräns eller bara i ett nästlat `then{}`-block.**
   `h.documentId as docId -> tgt.identifier = create('Identifier') as x then { docId ->
   x.value = docId; }` kraschar JSON-exporten med `'str' object has no attribute 'exportJson'`
   – den genererade koden tilldelar råsträngen direkt utan omslagslogik. Löst genom att ALLTID
   göra en färsk `as`-ombindning precis vid tilldelningspunkten (`docId as docId2 -> x.value =
   docId2`), vilket FÅR kompilatorn att generera den villkorade
   `malac.models.fhir.r4.string(value=...)`-omslagningen. Samma bugg orsakade ursprungligen
   `lib-hsa-reference.map`s `HsaIdToIdentifier`-grupp att krascha när den anropades med en rå
   RIVTA-sträng över en gruppgräns – löst genom att inlinea mönstret i stället för att anropa
   gruppen (se punkt 2 i "Det delade biblioteket i praktiken").
8. **Fält typade `uri` (`Meta.source`, `Identifier.system`, `Coding.system`, ...) omsluts
   ALDRIG av kompilatorn vid en VARIABEL-tilldelning, även med punkt 7:s fix – bara fält typade
   `string` (`Identifier.value`, `Coding.code`/`display`) får den villkorade
   omslagningslogiken.** En LITERAL till ett `uri`-fält omsluts dock korrekt
   (`recorderId.system = malac.models.fhir.r4.uri(value='urn:oid:...')` fungerar). Upptäckt
   genom att jämföra genererad kod rad för rad: `m.source = srcSys2` (ingen omslagning alls,
   kraschar) mot `docIdentifier.value = (...string(value=docId2)...)` (omsluten, fungerar) för
   EXAKT samma "fräsch as-ombindning"-mönster. Löst genom att explicit bygga omslaget själv
   (`m.source = create('uri') as metaSrc then { v -> metaSrc.value = v; }`) i stället för att
   förlita sig på kompilatorns (obefintliga) omslagning för `uri`-fält – samma mönster
   tillämpat för `subject.identifier.system` och `code.coding.system`.
9. **Choice-element (`Condition.onset[x]`, `Extension.value[x]`) har inget generiskt fält att
   tilldela – `tgt.onset = v`/`ext.value = v` kompilerar UTAN FEL men sätter ett odeklarerat,
   aldrig-exporterat Python-attribut, så fältet bara tyst försvinner ur utdatan.** Fungerar
   korrekt NÄR högerledet är `create('KonkretTyp')` (kompilatorn löser då choice-varianten från
   typnamnet – `chronicExt.value = create('boolean')...` blev korrekt
   `chronicExt.valueBoolean = ...` i genererad kod), men inte vid en direkt variabeltilldelning.
   Löst genom att alltid namnge choice-varianten explicit i `.map`-filen
   (`tgt.onsetDateTime`/`ext.valueDateTime` i stället för `tgt.onset`/`ext.value`).
10. **Att tilldela samma toppnivåfält (`tgt.meta`) i TVÅ separata regler skriver över, inte
    slår samman – samma "sista tilldelning vinner"-beteende som `fml-evaluation.md` redan
    dokumenterat för flerstegs-målsökvägar (`tgt.subject.identifier.system`).** `meta.source`
    och `meta.profile` måste därför byggas i ETT enda `then{}`-block som delar samma bundna
    `Meta`-variabel, inte som två fristående regler som båda gör `tgt.meta = create('Meta')`.

Ingen av dessa är dokumenterade begränsningar i FML-specifikationen – de är beteenden hos just
den här Python-implementationen av kompilatorn (MaLaC-HD 1.10.0), upptäckta genom att köra den.
Flera (punkt 5, 8, 9, 10) är helt tysta: inget undantag kastas förrän JSON-export-steget (punkt
8), eller aldrig alls (punkt 3, 9, 10) – fältet saknas bara i utdatan. Det är samma typ av
utvärderingsresultat som `fml-evaluation.md` landade i för HAPI-motorn: felsökning sker på
motorimplementationens villkor, och en jämförande testsvit mot den riktiga Java-mappern (som
`run_comparison.py` är) är det som faktiskt fångade punkt 1 ("`identifier` saknas i Java" hade
annars varit lätt att missa åt andra hållet också).

## Den enade källmodellen

Den ursprungliga PoC:n (ovan) och `fml-mapping-poc`/PR #39 använde VARSIN källmodell för exakt
samma `.map`-fil-par: `rivta_getdiagnosis_model.py` (generateDS, rå XSD-modell) här,
`inera-ehds-lm-diagnosis` (EHDS-TK:s publicerade logiska modell, `lm-diagnosis.json`) där. Det
gjorde `.map`-filerna strukturellt olika redan innan motorskillnaderna räknas – två
mappningsuppsättningar som råkar göra samma sak, inte en delad artefakt. Den riktiga strukturella
frågan den här arbetsomgången svarar på är: **byt till EN källmodell för båda spåren**, så att
samma `.map`-fil blir meningsfull att jämföra, eller köra, på båda motorerna.

Beslut: `inera-ehds-lm-diagnosis` (PR #39:s modell) vann, inte en tredje/ny modell och inte
`rivta_getdiagnosis_model.py`. Skälen:

- Den är redan en riktig FHIR `StructureDefinition` (`kind=logical`) – HAPI:s
  `org.hl7.fhir.r4.elementmodel.JsonParser` kan läsa en instans av den direkt, ingen adapter
  behövs på den sidan (oförändrat från PR #39).
- MaLaC-HD kräver INGEN logisk modell för att köra den – se `lm_diagnosis_model.py`
  (`python/model/`), som bara behöver uppfylla kompilatorns två faktiska kontrakt (ett
  toppnivånamn vid kompileringstillfället, en `parse()`-funktion vid körning – se den filens
  modulsträng för den fulla bekräftelsen, gjord genom att faktiskt kompilera och läsa
  kompilatorns egen kod, inte genom att anta). `rivta_getdiagnosis_model.py` behövde därför
  inte bytas ut mot en tredje Python-modell av samma logiska modell – samma generateDS-fria
  "ingen logisk modell behövs"-fördel från den ursprungliga PoC:n håller även här, bara riktad
  mot en annan källmodell.
- `rivta_getdiagnosis_model.py` lever vidare OFÖRÄNDRAD, men ENDAST för de oberoende testbara
  `lib-*.map`-referensfilerna (`model_plugin.py` registrerar nu två källmodeller sida vid
  sida) – den används inte längre av `get-diagnosis-to-condition.map`/
  `get-diagnosis-to-provenance.map`.

Två JSON-byggare producerar nu EXAKT samma `lm-diagnosis`-formade instans från en RIVTA
`<diagnosis>`-post: `python/lm_diagnosis_json_builder.py` (Python) och
`se.inera.ehds.malac.hapi.LmDiagnosisJsonSourceBuilder` (Java, detta moduls `src/main/java`) –
samma fältnamn, samma nästling, samma för-resolverade värden (se nästa avsnitt). Ingen av dem
delar kod med den andra; de är oberoende implementationer av samma kontrakt, av samma skäl som
`HapiFmlEngine`/`HapiMapperDump` (Java) nedan är skrivna från grunden mot PR #39:s
`FmlEngine`/`GetDiagnosisJsonSourceBuilder` som förlaga, inte ett beroende av den ej
sammanslagna grenen.

### En tolkningsgenväg: "xml" ljuger om formatet

En kuriositet värd att notera: MaLaC-HD:s kompilerade CLI-wrapper hårdkodar
`_SOURCE_JSON_SUPPORTED = False` för varje källmodell som inte är en av dess egna inbyggda
FHIR-modeller (`malac/hd/core/fhir/r5/generator/structuremap.py`), oavsett vad källmodellens
egen `parse()`-funktion faktiskt gör – flaggan styr bara wrapperns EGEN `--source-type`-
validering, aldrig anropet den till slut gör (`<modul>.parse(src, silence=True)`, alltid,
oavsett deklarerad källtyp). `lm_diagnosis_model.parse()` läser riktig JSON. Så `run_pipeline.py`
anropar den kompilerade filen med `-st xml` (en medveten "lögn" om formatet) och låter
`parse()` läsa JSON i praktiken – enklaste sättet att hålla samma kommandoradskontrakt som
innan, samtidigt som trådformatet byts till det enda format båda sidor nu delar.

## Vilka buggar överlevde modellbytet

Ingen av de ursprungliga tio buggarna (föregående avsnitt) antogs fortfarande gälla – varje en
omprövades mot den nya modellen genom att faktiskt kompilera och köra, inte genom att läsa om
den gamla listan.

**Överlevde oförändrade (modelloberoende MaLaC-HD-kompilatorbeteenden, inte knutna till den
gamla RIVTA-modellen):** punkt 1 (`imports` samma-katalog), 3 (inline `conceptmap{}` ignoreras),
4 (`where()` måste vara `$this`-formen), 5 (höger-ledsnavigering på samma rad som `where()`
tappas), 7 (färsk `as`-ombindning krävs för rå tilldelning), 9 (choice-element kräver namngiven
variant), 10 (`tgt.meta` i två regler skriver över). Alla sju bekräftade oförändrade genom att
de EXAKT SAMMA konstruktionerna dyker upp i det nya, delade `get-diagnosis-to-condition.map`
och fortsätter kräva samma arbetsomgångar (se filens kommentarer rad för rad).

**Överlevde, men nu en delad-fil-policy, inte bara en MaLaC-HD-bugg (punkt 6, `&`-
konkatenering):** bekräftat fortfarande trasigt för `inera-ehds-lm-diagnosis`-sourcade
variabler (samma `fhirpath_utils.get(None, ...)`-symptom som förut) – men eftersom filen nu
måste kompilera IDENTISKT på HAPI också, och HAPI:s egen version av samma fil tidigare löste
exakt detta med `&` direkt (se `fml-evaluation.md`), flyttades båda OID→URI-slagningarna och
Endpoint-URL-bygget till BÅDA JSON-byggarna i stället för bara till MaLaC-HD-sidans
preprocessing – ett beslut, inte ett tvång, så filen kan stanna byte-för-byte delad (se
`lm_diagnosis_json_builder.py`s modulsträng för den fulla motiveringen).

**Upphörde att vara relevant (var en konsekvens av `rivta_getdiagnosis_model.py`s flata
strängfält, inte av den nya modellens nästlade `Identifier`/`CodeableConcept`-form):** punkt 2
(`imports` + flera `-sr`-ConceptMaps kraschar) gäller fortfarande rent tekniskt för
`lib-*.map`-filerna (oförändrade, oförändrad modell), men påverkar inte längre
`get-diagnosis-to-condition.map`/`get-diagnosis-to-provenance.map` eftersom de aldrig anropat
`imports` ändå (se "Det delade biblioteket i praktiken") – och den enda återstående
`-sr`-ConceptMap:en (`diagnosis-type.json`) används ensam, aldrig tillsammans med en andra.
`codesystem-oid.json`-ConceptMap:en som fanns i den gamla versionen refereras inte längre alls
(se `compile.sh`s kommentar) – OID-slagningen sker nu helt utanför FML (föregående stycke), så
den ConceptMap:en behövs inte.

**Punkt 8 (uri-fält omsluts aldrig vid variabeltilldelning) överlevde på MaLaC-HD-sidan, men
fick en ny HALVA på HAPI-sidan** – se nästa avsnitt, ny bugg "uri-fält".

## Den portabla delmängden av FML

Att köra SAMMA fil på två motorer avslöjade ett nytt lager konflikter utöver MaLaC-HD:s egna –
platser där MaLaC-HD och HAPI kräver RAKT MOTSATTA FML-former för samma sak, inte bara platser
där en motor råkar ha en egen begränsning. Hittade genom att faktiskt köra varje kombination
mot båda motorerna och diffa resultatet (inte genom att läsa någon specifikation) – se
`get-diagnosis-to-condition.map`s och `get-diagnosis-to-provenance.map`s egna
header-kommentarer för den fulla, ordagrant bevarade redogörelsen; det här avsnittet
sammanfattar.

**11. `create('dateTime')` kraschar MaLaC-HD:s KOMPILATOR när källvärdet kommer från en egen
modell.** `_handle_target_copy()` (`malac/hd/core/fhir/r5/generator/structuremap.py`) gör
`hasattr(utils.get_module(src_type), 'dateTime')` för att avgöra om en sträng→dateTime-
konvertering behövs, och kraschar med `'NoneType' object has no attribute '__module__'` när
`src_type` är okänd – alltid sant för ett värde som kommer från en egen (icke-inbyggd)
källmodell. En ny, annorlunda bugg än punkt 9 (som bara gäller körningstid/tyst tapp, inte en
kompilatorkrasch).

**12. Choice-element av typ `dateTime`, sourcat från en egen källmodell, har INGEN FML-form som
fungerar på BÅDA motorerna.** Fyra kombinationer testade, alla fyra trasiga på minst en motor:

| Form | MaLaC-HD | HAPI |
|---|---|---|
| Namngiven variant, rå tilldelning (`tgt.onsetDateTime = v`) | OK (punkt 9 ovan) | `Attempt to write to invalid property 'onsetDateTime'` |
| Generisk variant, rå tilldelning (`tgt.onset = v`) | Tyst tappad (punkt 9) | OK |
| Generisk variant + `create('dateTime')` | Kompilatorkrasch (punkt 11) | OK |
| Namngiven variant + `create('dateTime')` | Kompilatorkrasch (punkt 11) | Samma fel som rad 1 |

Löst genom att INTE sätta `Condition.onset[x]` eller `ext-asserted-date`-extensionens
`Extension.value[x]` i FML alls – draget till drivrutinen (`run_pipeline.py`/
`HapiMapperDump.java`), som sätter dem direkt på `transform()`-resultatet, exakt som
`Provenance.target`/`.recorded` redan gjorde (en riktig `transform()`-anrop har bara EN target
och kan aldrig känna till sin "syskon"-resurs – samma skäl, en annan kategori av fält).

**13. Tre `uri`-typade fält (`Meta.source`, `Identifier.system`, `Coding.system`) korrumperas
TYST av HAPI när de sätts via `create('uri')` från ett NAVIGERAT (icke-literalt) käll-Element –
även när MaLaC-HD:s punkt 8-workaround (explicit `create('uri')`) används.** Bekräftat genom
att faktiskt dumpa och läsa HAPI-sidans utdata, inte genom att anta att punkt 8:s fix
portabelt löste problemet: `meta.source` blev `"value=string[https://tjanstekatalogen...]"`,
`subject.identifier.system` blev `"system=uri[http://electronichealth...]"` – HAPI:s generiska
`Element`s egen `toString()`-representation (`namn=typ[värde]`), inte värdet självt, läckte
rakt in i resultatet, UTAN fel eller varning. Samma `create('uri')`/`.value =`-mönster fungerar
korrekt på HAPI när högerledet är en FML-STRÄNGLITERAL (`meta.profile` i samma fil, oberört).
Eftersom `create('uri')` är precis det MaLaC-HD kräver (punkt 8) OCH precis det som korrumperar
värdet på HAPI, finns ingen enda form som är korrekt på båda – samma "dra ut ur FML"-lösning
som punkt 12, tillämpad på dessa tre fält i stället (se
`run_pipeline.py::_set_uri_fields`/`HapiMapperDump.java::setUriFields`, som läser de redan
resolverade värdena direkt ur samma JSON-instans transformen just konsumerade).

**14. En enda kombinerad flerstegsnavigering (`b.chronicDiagnosis.value as chronic`) kollapsar
TYST till bara första steget i MaLaC-HD:s genererade kod** (`getattr(b, 'chronicDiagnosis',
None)`, `.value`-steget försvinner helt) – bekräftat genom att läsa den genererade koden rad
för rad. HAPI:s generiska Element-modell omsluter ÄVEN bara-primitiva fält (samma anledning som
punkt 13), så `.value` behövs där för att nå det riktiga booleska värdet. Löst genom att dela
upp navigeringen i TVÅ separata regler (bind fältet, SEDAN navigera `.value` i en nästlad
regel) – samma mönster som redan användes för varje annan flerstegssökväg i filen (t.ex.
`sourceSystemHSAId.value`), nu bekräftat nödvändigt här också. `lm_diagnosis_model.py`s
`_BoolValue`-klass finns ENDAST för att göra denna `.value`-navigering till en korrekt no-op
även på MaLaC-HD-sidan (en bar Python-bool skulle annars inte ha något `.value` att navigera
till) – en liten, medveten form-matchning mellan de två sidornas källmodeller, inte en
generaliserad "omslut allt"-lösning.

**15. `create('Provenance_Agent')` (explicit, nästlad `BackboneElement`-typ) fungerar på
MaLaC-HD men AVVISAS av HAPI** ("Unknown Resource or Type Name" – en nästlad
`BackboneElement` utan egen fristående `StructureDefinition` kan inte namnges så). Att HELT
UTELÄMNA `create()` och låta vardera motorn räkna ut typen genom reflektion
(`tgt.agent as x then { ... }`) fungerar korrekt på BÅDA – MER portabelt än den ursprungliga
MaLaC-HD-bara formen, inte mindre. `get-diagnosis-to-provenance.map` använder den utelämnade
formen genomgående.

**Sammanfattning – den portabla delmängden**, för en `.map`-fil som ska kompilera OCH köra
identiskt på MaLaC-HD 1.10.0 och HAPI:s `StructureMapUtilities`, källa en egen/logisk
(icke-inbyggd) modell:

- Undvik `&`-strängkonkatenering med en källmodell-sourcad variabel helt (punkt 6) – gör
  strängbygget i värdspråket, i båda JSON-byggarna om filen är delad.
- Undvik att sätta ett choice-element (`x[x]`) från en källmodell-sourcad variabel helt (punkt
  12) – sätt det på `transform()`-resultatet i drivrutinen i stället.
- Undvik att sätta ett `uri`-typat fält från en NAVIGERAD (icke-literal) källmodell-variabel
  via `create('uri')` (punkt 13) – sätt det på resultatet i drivrutinen, precis som choice-
  elementet. En FML-strängliteral in i samma `uri`-fält är fortfarande säker.
  `create('<primitiv typ>')` + en FML-LITERAL är alltså fortsatt portabelt; det är bara
  "navigerat värde in i en skapad primitiv" som är den gemensamma riskzonen.
  BackboneElement-typer (`Provenance.agent`), tvärtom: UTELÄMNA alltid `create()`, låt
  reflektionen sköta det (punkt 15).
- Navigera ALDRIG mer än ETT steg per regelrad – dela upp i nästlade regler, en navigering per
  steg (punkt 5, 14). Gäller oavsett om det är en modellvariabel eller ett primitivt
  Element-omslutet fält.
- Skriv `where()` i sin bara `$this`-form, aldrig omkvalificerad med ett `as`-alias (punkt 4).
- Gör en FÄRSK `as`-ombindning precis vid varje tilldelningspunkt, aldrig en återanvänd
  modell-bunden variabel direkt (punkt 7).
- Namnge choice-varianten explicit när ett värde FAKTISKT kan sättas i FML (`ext.valueBoolean`,
  inte `ext.value`) – punkt 9, oförändrad.
- Håll varje toppnivåfält som kan nås från flera regler (t.ex. `tgt.meta`) i ETT delat,
  bundet `then{}`-block (punkt 10).

## Den nya tredje jämförelsen: HAPI/Java som en tredje motor

`se.inera.ehds.malac.hapi.HapiFmlEngine` (detta moduls `src/main/java`) kör EXAKT samma
`.map`-filer direkt från disk (`python/fml/get-diagnosis-to-condition.map` och
`...-provenance.map` – inga kopior) via `org.hl7.fhir.r4.utils.StructureMapUtilities`,
designmönstret lånat som förlaga från PR #39:s `FmlEngine`/`GetDiagnosisJsonSourceBuilder` men
skriven om från grunden i den här modulen, utan beroende av den ej sammanslagna grenen.
`HapiMapperDump`/`HapiMapperDumpTest` dumpar dess utdata för samma två testmeddelanden till
`target/hapi-mapper-dump/*.json`, och `compare/run_comparison.py` jämför nu alla TRE parvis
(produktions-`GetDiagnosisMapper.java`, Python/MaLaC-HD, Java/HAPI) i stället för bara två.

**Resultat (samma två testmeddelanden som tidigare):**

- **`get-diagnosis-ogiltigt-personnummer.xml`: identiskt på alla tre** – tom lista från alla
  tre, DIAG-001-vakten stoppar posten lika på alla tre spår (två gånger utanför FML, en gång
  dessutom en andra FHIRPath-baserad kontroll inuti `HapiFmlEngine` för paritet med PR #39:s
  mönster).
- **`get-diagnosis-huvuddiagnos.xml`: TVÅ skillnader, båda förklarade, ingen oförklarad:**
  1. `condition.identifier` (`documentId`): finns i Python/MaLaC-HD OCH Java/HAPI, saknas i
     produktions-Java – samma medvetna, dokumenterade tillägg som den ursprungliga PoC:n redan
     hittade (se "Täckning" ovan), nu bekräftat att BÅDA FML-körningarna gör det lika, eftersom
     det är samma delade `.map`-fil.
  2. `category[0].coding[0].display` ("Huvuddiagnos"): finns i produktions-Java OCH
     Python/MaLaC-HD, saknas ENDAST i Java/HAPI. ConceptMap:en (`diagnosis-type.json`) har
     `display` på målelementet; MaLaC-HD:s genererade `translate()`-kod kopierar det, HAPI:s
     inbyggda `StructureMapUtilities.translate()`-implementation gör det inte för denna
     "Coding"-läges-`translate()`, trots att samma ConceptMap-resurs är cachead och synlig för
     båda. En genuin, ny, isolerad HAPI-begränsning, inte en bugg i den delade `.map`-filen
     eller i något av de två spårens kod – rapporterad här, inte "fixad runt" i FML, eftersom
     det inte finns något FML-mönster som tvingar HAPI:s inbyggda `translate()` att fylla i
     `display` utan en riktig terminologitjänst.

Ingen oförklarad skillnad återstår. Samma slutsats som den ursprungliga tvåvägsjämförelsen,
nu bekräftad över en TREDJE, oberoende motorimplementation också.

## HealthSamurai/Aidbox

Ingen integration försöktes och inget nytt researcharbete lades ned – redan känt och
vidarebefordrat till Oskar: HealthSamurai/Aidbox har ingen FML/StructureMap-motor alls, så det
finns inget att köra den här delade `.map`-filen mot där.

## Feasibility-bedömning av Oskars pipeline-idé

Den föreslagna pipelinen – tjänstekontraktsspecifik FML + ett delat FML-bibliotek av RIVTA-
byggstenar → StructureMap/ConceptMap → MaLaC-HD → genererad kod – **fungerar, med ett viktigt
men hanterbart undantag**:

- **Kompileringssteget fungerar väl och utan att en logisk modell behöver författas**, vilket
  är ett genuint plus mot `fml-mapping-poc`s HAPI-spår – se "Varför Python, inte Java" ovan.
  Detta är den tydligaste vinsten av hela MaLaC-HD-spåret.
- **"Bygg biblioteket först"-delen av planen höll inte bokstavligt** för den här
  motorversionen: `imports`+`-sr`-ConceptMap-kombinationen (punkt 2) tvingade all delad logik
  att inlineas i varje anropande fil i stället för att faktiskt anropas från biblioteket. Om
  arbetet fortsätter mot GetCareDocumentation, är den rimliga vägen antingen (a) vänta på en
  MaLaC-HD-version som fixar punkt 2, eller (b) acceptera inlineing som permanent mönster och
  behålla `lib-*.map`-filerna som levande dokumentation/testbädd snarare än bokstavligt
  återanvänd kod.
- **De tysta buggarna (punkt 5, 8, 9, 10) är det allvarligaste fyndet**, inte
  kompileringsmodellen i sig: ett fält kan saknas helt i utdatan utan någon varning, vilket gör
  "skriv `.map`-filen och lita på den" olämpligt utan en jämförande testsvit (exakt samma
  slutsats `fml-evaluation.md` drog för HAPI-motorn, nu oberoende återbekräftad för en helt
  annan motorimplementation).
- **Slutresultatet, när alla workarounds är tillämpade, är korrekt**: `run_comparison.py` hittar
  EN skillnad mot produktionsmappern, och den är en medveten, dokumenterad utökning
  (`documentId`), inte ett fel.

**Sammanfattning**: pipelinen är genomförbar och gav, efter tio dokumenterade
motor-workarounds, en `Condition`/`Provenance`-mappning som är verifierat likvärdig med
produktionsmappern. Den är INTE "skriv `.map`-filen en gång och lita på den" – varje
icke-trivial regel (ConceptMap-fallback, choice-element, `uri`-fält, delad gruppanrop) krävde
att faktiskt köra motorn och läsa genererad Python-kod för att hitta rätt arbetsomgång. Att
fortsätta pipelinen till fler tjänstekontrakt är rimligt, men bör budgetera tid för att hitta
samma klass av tysta motor-quirks igen, inte bara tid för att skriva `.map`-filerna.

### Tillägg efter modellbytet och det tredje motortestet

Slutsatsen ovan håller, men med en skärpning: "verifierat likvärdig" gäller nu över TRE
oberoende motorimplementationer av samma `.map`-fil (produktions-Java, MaLaC-HD, HAPI), inte
bara två, och den delade filen visade sig behöva FEM till arbetsomgångar (punkt 11–15) utöver
de tio ursprungliga – alla av en strängare sort: inte en motors egen begränsning, utan platser
där de två FML-motorerna kräver RAKT MOTSATTA konstruktioner för samma sak, så att "dra ut ur
FML och sätt på `transform()`-resultatet i stället" blev den enda portabla lösningen, inte ett
sista utvägsval. Den positiva nyheten är att detta mönster redan fanns etablerat
(`Provenance.target`/`.recorded`, som aldrig kunde vara något annat ändå) – punkt 11–13
utökar bara samma mönster till fler fält, de kräver inget nytt arkitektoniskt grepp. Den enda
oförklarade kvarvarande skillnaden efter allt detta är `category[0].coding[0].display` som
HAPI:s inbyggda `translate()` inte fyller i – ett genuint, litet, isolerat HAPI-fynd, inte ett
fel i den delade filen eller i någon av drivrutinerna.
