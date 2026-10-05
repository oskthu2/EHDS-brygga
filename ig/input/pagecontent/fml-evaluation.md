# Utvärdering: FHIR Mapping Language (FML) som konfigurationsspråk för mappningen

Den här sidan hör till en egen utvecklingsgren (`claude/fml-mapping-9ovqyh`), inte till
produktionsbryggan. Den Java-baserade mapping-engine som bryggan faktiskt kör är opåverkad.
Syftet var att svara på en konkret fråga: kan [FHIR Mapping Language](https://build.fhir.org/ig/HL7/mapping-language-ig/branches/main/en/)
(FML), körd via HL7:s `StructureMap`-motor, ersätta eller komplettera Java-mapparna i
`bridge/mapping-engine`, med fokus på tre saker Oskar efterfrågade:

1. Kan felhantering (default-värde / data-absent-reason-flagga / stoppa resursen) styras via
   konfiguration i stället för Java-kod?
2. Är dokumentutbyte (Composition/Bundle) och det resursorienterade API:et två skilda spår,
   eller samma?
3. Hur lätt är det att lägga till fler resurser (Encounter, Observation, ...) ovanpå andra
   tjänstekontrakt?

## Vad som byggdes

Ett nytt Maven-modul, `bridge/fml-mapping-poc`, med:

- `get-diagnosis-to-condition.map` – GetDiagnosis → `Condition`: personnummer→subject (DIAG-001),
  diagnostyp→category via ConceptMap (DIAG-003), diagnoskod→code med OID→URI-slagning och
  urn:oid-fallback, onset/abatement, meta.source, clinicalStatus/verificationStatus, recorder/
  recordedDate, asserter/assertedDate-extension, chronicCondition-extension,
  relatedDiagnosis-extension.
- `get-diagnosis-to-provenance.map` – GetDiagnosis → `Provenance` (tre agenter: custodian/author/
  assembler), som en **separat** StructureMap – se "Fjärde begränsningen" nedan för varför den
  inte kunde vara en andra target i samma grupp.
- `get-caredocumentation-to-documentreference.map` – GetCareDocumentation → `DocumentReference`:
  status, masterIdentifier, date, meta.source, subject, context.related (careProcessId),
  blockComparisonTime-extension, type (clinicalDocumentNoteCode), description, innehåll
  (fritext icke-DocBook, samt båda multimediaEntry-grenarna: värde och referens), author,
  authenticator, signatureTime-extension, dissentingOpinion[0]-extension (se "Vad som INTE
  täcks" nedan för DocBook-narrativet/Composition, och varför bara post 0 av
  dissentingOpinion-listan tas med).
- Tre `ConceptMap`-resurser (`diagnosis-type`, `codesystem-oid`, ...) som motsvarar de YAML-filer
  Java-mapparna redan läser (`concept-maps/diagnosis-type.yaml`, `naming-systems.yaml`).
- `FmlEngine` – kör `org.hl7.fhir.r4.utils.StructureMapUtilities` offline (ingen
  packages.fhir.org-åtkomst krävs, se "Driftsfynd" nedan); en metod per målresurstyp
  (`transformDiagnosis`, `transformDiagnosisProvenance`, `transformCareDocumentation`).
- `RivtaDiagnosisParametersAdapter` / `RivtaCareDocumentationParametersAdapter` – plattar ut de
  RIVTA-fält som används till en FHIR `Parameters`-resurs (se "Källdata är inte FHIR" nedan för
  varför).
- `GetDiagnosisFmlComparisonTest` (7 tester) och `GetCareDocumentationFmlComparisonTest`
  (5 tester) – jämförande tester som körs genom **både** den riktiga Java-mappern och FML-motorn
  på samma indata och jämför resultatet fält för fält. Alla 12 är gröna.

Kör dem med `cd bridge && mvn test -pl fml-mapping-poc -am`.

## Täckning – vad är faktiskt översatt till FML

Siffrorna nedan räknar fält/regler, inte rader kod, och är avstämda mot de nuvarande
Java-mapparna (`GetDiagnosisMapper.java`, `GetCareDocumentationMapper.java`) på denna grens
startpunkt.

**GetDiagnosis → Condition + Provenance: 15 av 15 fält/regler översatta (100 %).**
clinicalStatus, verificationStatus, category (inkl. DIAG-003), code (inkl. OID-fallback), subject
(inkl. DIAG-001), onset, abatement, meta.source, recorder, recordedDate, asserter, assertedDate,
chronicCondition, relatedDiagnosis, och Provenance (tre agenter). Den enda posten som inte är en
"regel" i vanlig mening är VG-scope-filtret (`requestedVgHsaId`) – se "Fjärde begränsningen" nedan;
det är en semantisk gräns i motorn (stoppa en hel post), inte ett fält som saknar en regel.

**GetCareDocumentation → DocumentReference: 13 av 15 fält/regler översatta (cirka 87 %).**
Översatt: status, masterIdentifier, date, meta.source, subject, context.related,
blockComparisonTime, type, description, content (fritext icke-DocBook + båda
multimediaEntry-grenarna, räknas som en post), author, authenticator, signatureTime,
dissentingOpinion[0]-extension. **Explicit INTE översatt** (se nästa avsnitt för varför och hur
allvarligt varje fall är):

| Fält/regel | Status | Orsak |
|---|---|---|
| DocBook→narrative (`text/html`-attachment) | Kan inte uttryckas i FML | kräver anropet till `DocBookToNarrativeTransformer`, egen Java-logik |
| Composition "Strategy B" (sektionsträd) | Kan inte uttryckas i FML | samma skäl – bygger på samma transformer |
| `approvedForPatient` (PDL-001) | Öppen fråga även i Java | inget beslutat FHIR-kodverk ännu – ingenting att jämföra mot |

De två första är **genuint arkitektoniskt blockerade**: de kräver att en godtycklig
Java-funktion anropas mitt i en mappningsregel, vilket FML inte har någon mekanism för (ingen
extension-punkt för användardefinierade transformer användes eller hittades i denna
motorversion). `multimediaEntry` (båda grenarna) och `dissentingOpinion[0]` – som i en tidigare
version av denna sida stod som "inte påbörjade" – är nu översatta och verifierade med egna
jämförande tester; det enda kvarstående undantaget för `dissentingOpinion` är att bara den FÖRSTA
posten i listan bärs genom `Parameters`-adaptern (se adapterns klasskommentar) – en begränsning i
PoC:ns platta mellanrepresentation, inte i FML:s regel-mekanik, som fungerar identiskt för varje
ytterligare post om adaptern byggde ut dem (t.ex. med ett index i parameternamnet).

## Källdata är inte FHIR – den första friktionspunkten

FML kräver att käll- och målstrukturen är en FHIR-resurs eller en "logisk modell"
(en `StructureDefinition` med `kind=logical`). RIVTA-typerna i `mapping-engine/.../rivta/*.java`
är vanliga JAXB-klasser utan någon sådan definition. Två vägar fanns:

- **Författa en logisk modell** för hela RIVTA-schemat (en `StructureDefinition` per
  tjänstekontrakts svarstyp). Detta är den "riktiga" lösningen för produktion, men är ett
  betydande engångsarbete per tjänstekontrakt, och kräver att den logiska modellen hålls i synk
  med RIVTA-scheman separat från de redan existerande JAXB-klasserna.
- **Platta ut till `Parameters`** (vad PoC:n gör): en vanlig FHIR-resurs utan egen definition
  som behöver registreras. Snabbt att komma igång med, men tappar RIVTA:s verkliga nästlade
  struktur och typning – adaptern i `RivtaDiagnosisParametersAdapter` motsvarar i praktiken en
  bit av den logik som annars låg i Java-mappern, bara flyttad till ett annat ställe.

**Slutsats:** FML löser inte "slippa skriva adapterkod för RIVTA" – det flyttar adapterarbetet
från mappningslogik (vad Java-mapparna gör i dag) till modellering (en logisk modell, eller en
plattare mellanrepresentation). Den delen av vinsten som brukar säljas in med FML
("konfiguration istället för kod") gäller mappningen FHIR→FHIR, inte det första steget
RIVTA-XML→FHIR-kompatibel struktur.

### Rättelse: den logiska modellen behöver inte författas – den finns redan

Ovanstående "betydande engångsarbete" gäller att författa en logisk modell från noll. Oskar
påpekade att `inera-ab/EHDS-TK` redan publicerar en: `inera-ehds-lm-diagnosis`
(`input/fsh/logicalmodels/IneraEHDSLMDiagnosis.fsh`), och motsvarande finns för **alla** TK:er i
det repot, GetCareDocumentation och framtida Observation/Encounter-liknande kontrakt inkluderat
(`IneraEHDSLMCareDocumentation.fsh`, `IneraEHDSLMObservations.fsh`, m.fl. i
`input/fsh/logicalmodels/`). Varje modell är redan markerad `Characteristics: #can-be-target`,
vilket är precis den FSH-flaggan FML-motorn kräver för att acceptera en logisk modell som
källa/mål.

Verifierat i den här sessionen (`LogicalModelSpikeTest`, spike – inte en del av den levererade
täckningen): den publicerade, byggda `StructureDefinition`-JSON:en för
`inera-ehds-lm-diagnosis` (hämtad från IG:ns `gh-pages`-gren, eftersom `inera-ab.github.io` är
blockerat från denna sandlåda – se "Driftsfynd" nedan) går att registrera direkt i samma
`SimpleWorkerContext` som resten av denna PoC, och en `StructureMap` som deklarerar den som
`source` via `uses "https://fhir.inera.se/ig/ehds-tk/StructureDefinition/inera-ehds-lm-diagnosis"
alias LmDiagnosis as source` parsar och navigerar den nästlade strukturen
(`diagnosis.diagnosisHeader.documentId`, `diagnosis.diagnosisBody.diagnosisTime`) utan fel.

**Detta ändrar slutsatsen ovan påtagligt**, men inte helt: adapterarbetet (RIVTA-XML → en
instans av den logiska modellens Java/FHIR-representation) måste fortfarande göras NÅGONSTANS –
antingen ett eget JAXB→logisk-modell-serialiseringssteg, eller genom att RIVTA-avkodningen byggs
om att producera den logiska modellens form direkt i stället för dagens JAXB-POJO:er. Vad som
FAKTISKT försvinner med den redan-publicerade modellen är (a) arbetet att *författa och
versionshantera* den logiska modellen själv (redan gjort, och av Oskar själv i ett annat repo
som ändå måste hållas i synk med mappningsreglerna), och (b) den PoC-specifika
Parameters-plattningen som tappar struktur och typning (se ovan) – man mappar mot RIVTA:s egna
fältnamn och nästlingsnivåer, inte en handgjord lista av lösa `Parameters.parameter`-poster. Det
återstår alltså ett adapterlager, men ett tunnare och redan delvis specificerat ett, och
återanvändbart över alla TK:er eftersom samma logiska modeller redan finns för varenda kontrakt
i EHDS-TK – vilket också direkt besvarar punkt 3 (nya resurser): `IneraEHDSLMObservations.fsh`
m.fl. finns redan där, så "lägg till Observation" blir en ny `.map`-fil mot en källa som redan
är definierad, inte ett nytt modelleringsarbete.

## 1. Felhantering: default / data-absent-reason / stoppa resursen

De tre felhanteringsmönstren som redan är beslutade för GetDiagnosis (PR #38, se
`mapping-getdiagnosis.md`) gav en konkret testbädd:

| Mönster | Java-mappern | FML |
|---|---|---|
| **OID utan mappning → fallback-URI** (`urn:oid:<oid>`) | en rad: `namingSystem.oidToUri(oid)` med inbyggd fallback | **Går, men måste skrivas för hand två gånger.** `translate()` mot en `ConceptMap` ger *alltid* ett fast värde för omappade koder (ConceptMap `unmapped.mode`), inte en beräknad sträng. Att bygga `urn:oid:<oid>` kräver en andra, separat regel vars `where()`-villkor är den **bokstavliga negationen** av den mappade regelns villkor (samma OID-lista skriven ut två gånger, en gång rakt och en gång negerad). Ingen mekanism tvingar dem att hållas i synk – lägg till ett kodverk i YAML-filen och FML-regeln vet inte om det. |
| **Diagnostyp utan ConceptMap-mappning → `data-absent-reason=unknown`, ingen gissad kod** (DIAG-003) | `Optional.ifPresentOrElse(...)` | **Går, med samma dubbleringsproblem som ovan.** `ConceptMap`-"unmapped"-läget kan bara ge en kod, inte byta till en helt annan resursform (en extension i stället för en coding). Löst här med samma mönster: två speglade regler. |
| **Ogiltigt/saknat personnummer → stoppa posten** (DIAG-001) | `if (!isValidPersonId(...)) return null;` stoppar **hela** `Condition`+`Provenance`-paret innan något annat fält sätts | **Går inte på regelnivå.** Se nedan. |

### Det here finns ett verkligt (inte bara stilistiskt) gap: att stoppa en hel resurs

Java-mappern kan returnera `null` för hela posten mitt i mappningen. En FML-regel kan bara
avstå från att sätta **ett enskilt fält** (`where()` slår inte till → inget `subject` skapas,
men `Condition`-resursen skapas och skickas vidare ändå, bara utan `subject`). Vårt test
(`diag001_...`) visar precis detta: Java-sidan ger en tom lista (posten finns inte), FML-sidan
ger en `Condition` utan `subject`. **För att verkligen stoppa hela resursen i FML måste villkoret
flyttas upp en nivå**, till den kod som itererar över listan av diagnoser och anropar
`transform()` per post – dvs. tillbaka till Java (eller vilken värdkod som anropar
StructureMap-motorn). FML i sig har ingen "skippa hela målobjektet"-konstruktion inifrån en regel.

**Slutsats för punkt 1:** FML ger konfiguration för två av tre mönster (default/fallback-värde,
data-absent-reason-flagga), men med en viktig brist: ett omappat värde kräver en HANDSKRIVEN,
duplicerad negation av villkoret snarare än ett riktigt "annars"-grenval. Det tredje mönstret
(stoppa hela resursen) kan inte uttryckas i FML-regler alls – det måste ligga i värdkoden runt
StructureMap-anropet, precis som i dag. Att "styra felhantering via konfiguration" blir därför
en delvis sanning: två av tre fall flyttar in i `.map`-filen, det tredje stannar i Java oavsett.

### Fjärde begränsningen (upptäckt när Provenance lades till): en `transform()` tar bara EN target

`StructureMapUtilities.transform(appInfo, source, map, target)` tar en enda `target`-parameter,
och `getInputName()` kastar `This engine does not support multiple source inputs` (texten nämner
bara "source" men gäller identiskt för `target`-moden) så snart en grupp deklarerar mer än en
input av samma mode. Att producera två målresurser (`Condition` + `Provenance`) från samma
källpost med ETT `transform()`-anrop går alltså inte – lösningen här var två helt separata
`StructureMap`-filer (`get-diagnosis-to-condition.map` och `get-diagnosis-to-provenance.map`),
körda med två separata `transform()`-anrop från samma källdata. Det här är ytterligare en punkt
där "konfiguration" har en hård gräns: att orkestrera FLERA målresurser per källpost (vilket varje
TK-mappning i den här bryggan gör – Condition+Provenance, DocumentReference+Provenance+eventuell
Composition) måste ligga i anropande kod, inte i en enda `.map`-fil.

## 2. Dokumentutbyte vs resursorienterat API

Dessa är redan två skilda spår i FML:s egen modell, inte något som behöver uppfinnas:

- En `StructureMap`-grupp mappar **en** källtyp till **en** måltyp (här: RIVTA-diagnos →
  `Condition`). Samma grupp kan återanvändas oförändrad som mål för flera "spår":
  - **Resursorienterat API**: anropa `transform()` en gång per post, FHIR-servern serverar varje
    `Condition` för sig (precis som `fhir-server`-modulen gör i dag).
  - **Dokumentutbyte**: en *annan*, tunn StructureMap-grupp bygger en `Composition`/`Bundle` och
    `include`-ar/anropar diagnos-gruppen per post för att fylla `Bundle.entry`. Den delade
    mappningslogiken (fält-för-fält RIVTA→Condition) skrivs en gång; bara "paketeringen" skiljer
    sig åt mellan spåren.
- FML:s `imports`-direktiv låter en `.map`-fil återanvända grupper från en annan, vilket är
  precis den strukturen: en gemensam uppsättning per-resurs-mappningar, och separata tunna
  dokument- respektive API-mappningar ovanpå.

**Slutsats för punkt 2:** modellen stämmer redan med det Oskar efterfrågade – dela upp i en
delad kärna (RIVTA-fält → FHIR-resurs, en grupp per resurstyp) och två tunna
paketeringsmappningar (Bundle/Composition respektive enskild resurs). Den här uppdelningen
är dock lika lätt (eller svår) att göra med dagens Java-mappers-arkitektur – `GetDiagnosisMapper`
och `GetCareDocumentationMapper` är redan skilda från hur `fhir-server` serverar resultatet.
FML:s bidrag här är inte en ny förmåga, bara att samma idé uttrycks deklarativt.

## GENERAL-reglerna (tidszon, meta.source, patientreferens)

De tvärgående GENERAL-besluten (se teamminnet: GENERAL-001 tidszon, meta.source-konstruktionen,
patientreferens-som-logisk-identifierare) är redan övade av de fält som översatts ovan, inte en
separat uppgift:

- **GENERAL-001 (tidszon):** RIVTA-tidssträngarna (`YYYYMMDD[HHmmss]`, svensk lokal tid) måste
  konverteras till ISO 8601 med explicit offset INNAN de når FML – `RivDateParser.parse(...)`/
  `parseInstant(...)` körs i adapterlagret (`RivtaDiagnosisParametersAdapter`,
  `RivtaCareDocumentationParametersAdapter`), inte i någon FML-regel. Det är i sig ett fynd: FML
  har ingen inbyggd funktion för att parsa en RIVTA-specifik datumsträng med svensk lokal
  tidszonsoffset – all sådan tolkning måste ske innan källdatat blir en `Parameters`-resurs, dvs.
  i Java. FML-reglerna (`onset`, `recordedDate`, `date`, `blockComparisonTime`, `signatureTime`
  m.fl.) kopierar bara redan-konverterade strängar rakt av.
- **meta.source:** samma `TJÄNSTEKATALOG_BASE + "/Endpoint/" + hsaId`-konstruktion som i Java,
  översatt rakt av med FHIRPath-strängkonkatenering (`&`) i både `get-diagnosis-to-condition.map`
  och `get-caredocumentation-to-documentreference.map` – en beräknad sträng, ingen slagning, så
  den är enkel att uttrycka och redan verifierad av båda jämförande testsviterna.
- **Patientreferens som logisk identifierare:** `subject`/`patientId` byggs i båda `.map`-filerna
  som en `Reference.identifier` (system+value), aldrig en direkt resursreferens – samma mönster
  som Java. Den enda förenklingen mot Java: identifier-systemet hårdkodas till personnummer-URI:n
  direkt i FML-regeln i stället för att slå upp `patientId.root` via `NamingSystemRegistry`
  (samma typ av förenkling som gjordes för HSA-systemet, se kommentarerna i `.map`-filerna) –
  rimligt för en PoC eftersom root-OID:t i praktiken alltid är samma värde i testdatan, men en
  riktig produktionsöversättning skulle behöva en `translate()`/ConceptMap-slagning här också.

## 3. Lägga till nya resurser (Encounter, Observation, ...) ovanpå andra tjänstekontrakt

Det här är där FML:s deklarativa form faktiskt gör skillnad, **förutsatt att källan redan är en
FHIR- eller logisk-modell-struktur** (se friktionspunkten ovan):

- En ny resurstyp är en ny `.map`-fil (eller ny grupp i en befintlig), inte en ny Java-klass,
  ny Maven-test-scaffolding, ny `@Nested`-struktur. Att lägga till `Encounter` ovanpå t.ex.
  GetCareContact-kontraktet skulle i FML-världen vara: skriv regler för de fält kontraktet redan
  exponerar, pek `translate()` mot samma typ av `ConceptMap`-resurser som redan finns för
  diagnostyp/kodverk, klart. Ingen ny `.java`-fil, ingen ny Spring-wiring.
- **Men**: detta förutsätter att adapterarbetet (plattning till `Parameters`, eller en riktig
  logisk modell) redan är gjort för det tjänstekontraktet – annars är "lägg till en ny resurs"
  fortfarande "skriv en ny adapter + en `.map`-fil", inte uppenbart billigare än att skriva en ny
  `XxxMapper.java`-klass som i dag.
- `ConceptMap`- och `NamingSystem`-återanvändning fungerar rakt av: samma `codesystem-oid.json`
  skulle kunna återanvändas av en `Observation`-mappning utan ändring, precis som
  `NamingSystemRegistry` redan delas mellan Java-mapparna i dag.

**Slutsats för punkt 3:** ja, detta är den genuina vinsten – när källan redan är FHIR-formad är
en ny målresurs en ny deklarativ fil, inte ny kod, och delade `ConceptMap`/`NamingSystem`-resurser
fungerar som väntat. Vinsten gäller specifikt "lägg till en resurs ovanpå data som redan finns i
FHIR-form"; för RIVTA-käll­data måste adapterarbetet göras en gång per tjänstekontrakt oavsett.

## Konkreta motorbegränsningar/buggar som hittades under arbetet

Dessa upptäcktes genom att faktiskt köra `org.hl7.fhir.r4` (hapi-fhir-validation 7.4.0,
`org.hl7.fhir.r4` 6.3.11) mot riktiga testfall, inte genom att läsa specifikationen:

1. **`where (A) and (B or C)` – ett parentetiserat OR i ett AND-villkor – gör att motorn helt
   hoppar över regelns `then{}`-block, utan att kasta något fel.** Villkoret utvärderas och
   loggas korrekt (debug-utskriften visar rätt sant/falskt per källpost), men de nästlade
   reglerna körs aldrig – resultatet blir tyst ett fält som saknas, inte ett undantag. Detta är
   den svåraste typen av bugg att upptäcka i produktion: inget kastas, inget loggas som fel.
   Arbetades runt genom att skriva villkoret som en enda regex (`value.matches('^(HD|BY)$')`)
   i stället för `(value = 'HD' or value = 'BY')`.
2. **En transform-parameter till höger om `=` måste vara en redan bunden variabel (`X as v`),
   aldrig en punktnotationsväg som `dcode.value` skriven direkt.** Motorn tolkar hela strängen
   `"dcode.value"` som ETT variabelnamn och kastar `Variable dcode.value not found` – den gör
   ingen egenskaps-navigering i den positionen. Varje fält måste därför bindas i ett eget steg
   (`X.value as v -> ...= v`) innan det kan användas, vilket gör annars enkla en-rads-mappningar
   till tre rader.
3. **`translate(code, map, outputtype)` ignorerar `outputtype` utom för det specialfallet
   `'code'`.** Dokumentationen antyder att `'system'`, `'display'`, `'text'` skulle ge just den
   delen av resultatet, men implementationen i denna version returnerar alltid hela `Coding`-
   objektet om `outputtype` inte exakt är `'code'`. Att tilldela resultatet till ett enskilt
   `system`-fält (`cdg.system = translate(..., 'system')`) kastar
   `Unable to convert a Coding to a Uri`. Lösning: låt `translate()`s resultat bli hela
   Coding-variabeln (`cat.coding = translate(...) as cdg then {...}`), inte ett delfält av den.

4. **Att skapa ett nästlat `BackboneElement` (t.ex. `Provenance.agent`, `DocumentReference.content`,
   `DocumentReference.context`) med `create('Typnamn')` kraschar med `Unknown Resource or Type
   Name`.** `create()` slår upp typnamnet som en egen `StructureDefinition`
   (`ResourceFactory.createResourceOrType`), men ett `BackboneElement` som bara finns nästlat
   inuti en annan resurstyp (t.ex. "Provenance.agent") har ingen egen `StructureDefinition` i
   `profiles-resources.xml` – bara toppnivåresursen "Provenance" har en. Lösning: hoppa över
   `create(...)` helt för den typen av fält (`tgt.agent as custAgent then {...}`, ingen
   `= create(...)` alls) – motorn skapar då backbone-elementet via sin egen
   `dest.makeProperty(...)`-reflektion i stället, vilket fungerar för både 0..1- och
   0..*-element.
5. **Ett FHIRPath-booleanlitteral (`true`/`false`, utan citattecken) som tilldelas ett
   `boolean`-elements `.value` kraschar med `Invalid boolean string: 'BooleanType[true]'`.**
   `PrimitiveType.setProperty()` gör `setValueAsString(value.toString())`; `StringType`
   override:ar `toString()` till att returnera själva textvärdet, men `BooleanType` gör det INTE
   (ärver `PrimitiveType`s standard-`toString()`, `"ClassName[värde]"`), så literalen
   `"BooleanType[true]"` skickas in rakt av i stället för `"true"`. En citerad strängliteral
   (`'true'`/`'false'`) fungerar dock utmärkt, eftersom en `StringType`s `toString()` ger rätt
   råtext och motorns `fromStringValue()` parsar `"true"`/`"false"` till ett booleanvärde oavsett
   källtyp.
6. **En sträng kan inte tilldelas direkt till ett `base64Binary`-fält som är en EGENSKAP på en
   komplex typ (t.ex. `Attachment.data`) – men fungerar fint om fältet är en EGEN,
   skapad `base64Binary`-variabel.** `Attachment.setProperty("data", value)` gör
   `Base.castToBase64Binary(value)`, som kastar `Unable to convert a StringType to a
   Base64Binary` om `value` är en `StringType` i stället för en redan färdig
   `Base64BinaryType`. Lösning: samma bind-mönster som för `dateTime`/`boolean` ovan –
   `att.data = create('base64Binary') as dataEl then { v -> dataEl.value = v; }` – eftersom det
   bygger en riktig `Base64BinaryType` och tilldelar den, i stället för att försöka casta en
   `StringType` direkt.

Ingen av dessa är dokumenterade begränsningar i FML-specen – de är beteenden hos just denna
Java-implementation av motorn, upptäckta genom att köra den. En annan StructureMap-motor
(t.ex. en .NET- eller Node-implementation) kan bete sig annorlunda. Det här är i sig ett
utvärderingsresultat: **felsökning i FML sker på motor-implementationens villkor, med betydligt
sämre felmeddelanden och stacktraces än motsvarande Java-kod**, och "skriv det deklarativt" byter
ut en kompileringsfelsklass (Java) mot en klass av tysta, svårupptäckta körningsfel (FML).

## Driftsfynd: offline-körning

`SimpleWorkerContext.fromClassPath()`/`fromPackage(...)` – de vägar som HAPI:s egen dokumentation
visar – förväntar sig antingen en numera inte längre paketerad `validation.json.zip`, eller
NPM-paketet `hl7.fhir.r4.core` hämtat från `packages.fhir.org`. Det senare är blockerat från den
här miljön (403, samma begränsning som är känd för SUSHI/IG-byggen, se teamminnet). Lösningen var
att bygga en `IWorkerContext` direkt från `profiles-types.xml`/`profiles-resources.xml`/
`extension-definitions.xml`, som fortfarande ligger i `hapi-fhir-validation-resources-r4`-jar:en
men inte är den fil `fromClassPath()` letar efter. Maven Central självt är däremot nåbart genom
proxyn, så `hapi-fhir-validation`-beroendet laddades ner utan problem. En riktig IG-utgåva med
riktiga FHIR-paket (EU-profiler m.m.) skulle sannolikt stöta på samma `packages.fhir.org`-
begränsning som redan är dokumenterad för SUSHI.

`inera-ab.github.io` (där EHDS-TK:s byggda IG-sidor, inklusive `StructureDefinition`-JSON för de
logiska modellerna, publiceras) är **också** blockerat av samma egress-proxy. Den byggda JSON:en
gick trots det att hämta: EHDS-TK:s `gh-pages`-grens innehåll är nåbart via vanlig `git clone`
mot `github.com` (en annan domän än `inera-ab.github.io`), så `git show
<gh-pages-sha>:StructureDefinition-inera-ehds-lm-diagnosis.json` gav samma byggda artefakt som
den blockerade webbsidan skulle ha visat. Ett generellt mönster värt att komma ihåg: när en
`*.github.io`-sida är blockerad men det underliggande repot inte är det, ligger byggartefakterna
oftast kvar, hämtningsbara, på `gh-pages`-grenen.

## Rekommendation

Upptäckten att EHDS-TK redan publicerar riktiga logiska modeller för samtliga tjänstekontrakt
(se rättelsen ovan) väger upp en del av det tidigare "inte värt det just nu" – adapterarbetet är
mindre och mer återanvändbart än PoC:ns `Parameters`-plattning fick det se ut. Rekommendationen
justeras därför:

- **Fortfarande inte värt att migrera befintliga mappare rakt av just nu**, men av ett svagare
  skäl än tidigare: de logiska modellerna finns redan, men de har inte provkörts med RIKTIGA
  RIVTA-instanser genom en serialiserare (den här spiken registrerade bara
  `StructureDefinition`:n och navigerade den tomt – inget faktiskt transform() mot en ifylld
  instans har körts än). Två av tre önskade felhanteringsmönster kräver fortfarande handskriven
  duplicering eller stannar i Java oavsett källmodell.
- **Värt ett konkret nästa steg**, inte bara "hålla ögonen på": kör en riktig
  `transform()`-instans mot `inera-ehds-lm-diagnosis` (bygg en liten
  JAXB→logisk-modell-serialiserare för GetDiagnosis, eller skriv en handgjord testinstans) och
  jämför mot både Java-mappern och denna PoC:s `Parameters`-variant. Om det går lika smidigt som
  den här spikens parse-steg antyder, är EHDS-TK:s logiska modeller den naturliga grunden för en
  eventuell fortsättning – inte en ny `Parameters`-adapter per tjänstekontrakt.
- **Lägga till nya resurser (punkt 3) är nu en starkare vinst än först bedömt**: eftersom
  `IneraEHDSLMObservations.fsh`, `IneraEHDSLMCareDocumentation.fsh` m.fl. redan finns i EHDS-TK,
  är modelleringsarbetet för en ny resurs redan gjort för varje existerande tjänstekontrakt –
  kvar står bara att skriva `.map`-filen och en serialiserare från RIVTA-XML till den logiska
  modellens form.
- Om arbetet fortsätter: bygg en liten testsvit (som `GetDiagnosisFmlComparisonTest` här) runt
  varje regel innan den litas på – de tysta fel som hittades ovan (punkt 1 särskilt) gör
  "skriv och lita på" olämpligt för FML i denna motorversion, oavsett vilken källmodell som
  används.
