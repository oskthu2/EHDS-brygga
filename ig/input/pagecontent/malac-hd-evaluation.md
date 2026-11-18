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
