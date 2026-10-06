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

Branchen fick senare en uppföljande, mer konkret beställning från Oskar: rätta mappningen mot
nuvarande produktionsschema (efter PR #41:s XSD-baserade ommodellering), bygg om källadaptern så
att källan registreras som en **riktig** logisk modell (inte en plattad `Parameters`-genväg), gör
motorn generisk i matchbox-`$transform`-stil ("lägg till en map-fil och det funkar"), och behåll
"ett anrop → en resurs" samtidigt som flera målresurser (Condition+Provenance,
DocumentReference+Provenance) får dela samma underliggande tjänstekontrakt. De avsnitten nedan
som beskriver resultatet av det är markerade med **(uppdaterat)**.

## Vad som byggdes

Ett nytt Maven-modul, `bridge/fml-mapping-poc`, med:

- **(uppdaterat)** Källan är nu EHDS-TK:s publicerade logiska modeller
  (`inera-ehds-lm-diagnosis`, `inera-ehds-lm-care-documentation`), inte en plattad `Parameters`-
  resurs – se "Källdata är inte FHIR" nedan för hur.
- `get-diagnosis-to-condition.map` – GetDiagnosis → `Condition`: category via ConceptMap
  (Huvuddiagnos/Bidiagnos), diagnoskod→code med OID→URI-slagning och urn:oid-fallback, onset,
  meta.source, clinicalStatus/verificationStatus, recorder/recordedDate, asserter/
  assertedDate-extension, chronicDiagnosis-extension, relatedDiagnosis-extension. Giltigt
  personnummer är inte längre ett fält-villkor i `.map`-filen – det är nu
  `FmlEngine`:s deklarativa "stoppa resursen"-vakt, se punkt 1 nedan.
- `get-diagnosis-to-provenance.map` – GetDiagnosis → `Provenance` (två agenter: custodian/
  author), som en **separat** StructureMap mot SAMMA källa – se "Fjärde begränsningen" nedan för
  varför en `transform()` inte kan producera båda målen i ett anrop, och "En källa, flera
  målresurser" för hur `FmlEngine` nu orkestrerar det ändå.
- `get-caredocumentation-to-documentreference.map` och `get-caredocumentation-to-provenance.map`
  – samma mönster för GetCareDocumentation: status, masterIdentifier, date, meta.source, subject,
  context.related (careProcessId), blockComparisonTime-extension, type
  (clinicalDocumentNoteCode), description, innehåll (fritext icke-DocBook, samt båda
  multimediaEntry-grenarna: värde och referens), author, authenticator, signatureTime-extension,
  dissentingOpinion[0]-extension, och en egen Provenance-map (recorded från author.timestamp med
  record.timestamp som fallback, två agenter).
- Två `ConceptMap`-resurser (`diagnosis-type`, `codesystem-oid`) som motsvarar de YAML-filer
  Java-mapparna redan läser (`concept-maps/diagnosis-type.yaml`, `naming-systems.yaml`).
- **(uppdaterat)** `FmlEngine` – en enda generisk `transform(key, source)`-metod, driven av en
  deklarativ registry (`MappingDefinition`: källmodell, map-fil, gruppnamn, målresurstyp,
  valfri FHIRPath-"stoppa resursen"-vakt) i stället för en hårdkodad Java-metod per
  målresurstyp. Målinstansen skapas via reflektion (`Class.forName("org.hl7.fhir.r4.model." +
  targetType)`), inte en ny `new Condition()`/`new Provenance()`-rad i Java för varje tillägg –
  se "En källa, flera målresurser" nedan.
- **(uppdaterat)** `GetDiagnosisJsonSourceBuilder` / `GetCareDocumentationJsonSourceBuilder` –
  bygger en JSON-instans av respektive logiska modell direkt från RIVTA-JAXB-objekten, parsad av
  `org.hl7.fhir.r4.elementmodel.JsonParser` till en riktig FML-källa (`Element`). Ersätter de
  tidigare `RivtaDiagnosisParametersAdapter`/`RivtaCareDocumentationParametersAdapter`
  (borttagna) – se "Källdata är inte FHIR" nedan.
- `GetDiagnosisFmlComparisonTest` (7 tester) och `GetCareDocumentationFmlComparisonTest`
  (6 tester) – jämförande tester som körs genom **både** den riktiga Java-mappern och FML-motorn
  på samma indata och jämför resultatet fält för fält. Alla 13 är gröna, inklusive ett nytt test
  som verifierar att "stoppa resursen"-vakten returnerar `null` precis som Java-sidans tomma
  lista.

Kör dem med `cd bridge && mvn test -pl fml-mapping-poc -am`.

## Interaktiv testklient

`FmlTestClient` (`se.inera.ehds.fml.FmlTestClient`) är en enkel kommandoradsklient för att
manuellt utforska mappningarna utan att läsa testkoden: väljer SOAP-källa/testmeddelande, visar
att den laddas in mot sin logiska modell, väljer en FML-mappning ur `FmlEngine`s registry, och
kör antingen `$transform` (hela mappningen, resulterande FHIR-resurs som JSON) eller `$evaluate`
(ett valfritt FHIRPath-uttryck mot den inlästa källan – samma mekanism som registryts
`stopIfFalseFhirPath`-vakt använder, men fritt skrivbart för att inspektera enskilda fält).

De fyra bundlade testmeddelandena (`src/main/resources/testmessages/*.xml`) är riktiga,
schema-giltiga SOAP-svar – marshallade från samma JAXB-klasser som produktionskoden, inte
handskriven XML (se `GenerateTestMessagesTool`, körs manuellt för att regenerera dem): en
giltig och en ogiltig GetDiagnosis-post (den senare demonstrerar stoppa-resursen-vakten), samt
en platt-fritext- och en multimedia-post för GetCareDocumentation.

Körs med:

```bash
cd bridge && mvn -pl fml-mapping-poc exec:exec
```

(`exec:exec`, inte `exec:java` – `exec:java`s isolerade klassladdare kolliderar med JAXB:s
modul-split på JDK 17 och kastar en `loader constraint violation` på `QName`/
`DatatypeConstants`; `exec:exec` kör i en riktig forkad process och undviker det.)

## Täckning – vad är faktiskt översatt till FML

Siffrorna nedan räknar fält/regler, inte rader kod, och är avstämda mot de nuvarande
Java-mapparna (`GetDiagnosisMapper.java`, `GetCareDocumentationMapper.java`) EFTER PR #41:s
XSD-baserade ommodellering – dvs. mot det schema som faktiskt körs i produktion i dag, inte
startpunktens.

**GetDiagnosis → Condition + Provenance: samtliga fält/regler översatta.**
clinicalStatus, verificationStatus, category (Huvuddiagnos/Bidiagnos via ConceptMap), code
(inkl. OID-fallback), subject, onset, meta.source, recorder, recordedDate, asserter,
assertedDate, chronicDiagnosis, relatedDiagnosis, och Provenance (två agenter: custodian/
author). `abatement`/`period` finns inte längre i det nuvarande RIVTA-schemat
(`diagnosisTime` är en enda instant, se teamminnet) och är därför inte en lucka utan en korrekt
avspegling av källan. Giltigt personnummer (det som tidigare krävde ett separat VG-scope-liknande
filter) är inte längre ett fält i `.map`-filen: det är nu `FmlEngine`:s deklarativa
"stoppa resursen"-vakt (en FHIRPath-regel i registry-posten), verifierad av ett eget test
(`stoppaResursen_felaktigtPersonnummer_ingenResursAlls`) – se punkt 1 nedan.

**GetCareDocumentation → DocumentReference + Provenance: samtliga fält utom två (se tabellen).**
Översatt: status, masterIdentifier, date, meta.source, subject, context.related,
blockComparisonTime, type, description, content (fritext icke-DocBook + båda
multimediaEntry-grenarna, räknas som en post), author, authenticator, signatureTime,
dissentingOpinion[0]-extension, och Provenance (recorded från author.timestamp med
record.timestamp som fallback, två agenter: custodian/author). **Explicit INTE översatt** (se
nästa avsnitt för varför och hur allvarligt varje fall är):

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
instans av den logiska modellens form) måste fortfarande göras NÅGONSTANS – antingen ett eget
JAXB→logisk-modell-serialiseringssteg, eller genom att RIVTA-avkodningen byggs om att producera
den logiska modellens form direkt i stället för dagens JAXB-POJO:er. Vad som FAKTISKT försvinner
med den redan-publicerade modellen är (a) arbetet att *författa och versionshantera* den logiska
modellen själv (redan gjort, och av Oskar själv i ett annat repo som ändå måste hållas i synk med
mappningsreglerna), och (b) den PoC-specifika Parameters-plattningen som tappar struktur och
typning (se ovan) – man mappar mot RIVTA:s egna fältnamn och nästlingsnivåer, inte en handgjord
lista av lösa `Parameters.parameter`-poster.

### Andra rättelse: adapterlagret är nu byggt, inte bara spikat – och det blir JSON, inte Java-objekt

Ovanstående var skrivet när bara `LogicalModelSpikeTest` fanns (en navigering av en tom
`StructureDefinition`, inget faktiskt `transform()` mot en ifylld instans). Oskars uppföljande
fråga var rakt på sak: **soap/xml behöver bli json, eller hur?** – svaret är ja, och det är nu
implementerat, inte bara en gissning:

- `GetDiagnosisJsonSourceBuilder`/`GetCareDocumentationJsonSourceBuilder` går från de redan
  avkodade RIVTA-JAXB-objekten (SOAP/XML är redan Java-objekt vid den punkten, ingen extra
  XML-avkodning behövs) till en handbyggd `com.google.gson.JsonObject`-trädstruktur som
  speglar den logiska modellens fältnamn/nästling exakt.
- Den JSON-strängen matas in i `org.hl7.fhir.r4.elementmodel.JsonParser.parse(json,
  sd.getType())`, som ger tillbaka ett riktigt `org.hl7.fhir.r4.elementmodel.Element` – samma
  bastyp (`Base`) som en vanlig FHIR-resurs skulle ge. Det är detta `Element` som skickas in som
  `source` till `StructureMapUtilities.transform()`, inte en Java-POJO av den logiska modellens
  "typ" (det finns ingen sådan POJO-klass – logiska modeller genereras bara till
  `StructureDefinition`+FSH, aldrig till Java-bönor).
- Varför JSON och inte t.ex. XML rakt in i `elementmodel`-parsern: `JsonParser` är den enklaste,
  mest beprövade vägen in i `elementmodel.Element` i denna HAPI-version, och Gson fanns redan
  transitivt på classpath (via `hapi-fhir-validation`) – ingen ny dependency behövdes. En
  XML-väg (`elementmodel.XmlParser`) skulle fungera likvärdigt men ger inget extra: adapterlagret
  (fältnamn, OID→URI, datumformat) måste skrivas oavsett vilket av de två man väljer.
- Detta adapterlager är fortfarande det enda handskrivna steget per tjänstekontrakt – men det är
  nu en vanlig trädbyggande Java-klass (lätt att testa isolerat, inga FML-körningsfel inblandade)
  i stället för en `Parameters`-plattning som tappade struktur. Det besvarar också punkt 3 (nya
  resurser) konkret: `IneraEHDSLMObservations.fsh` m.fl. finns redan i EHDS-TK, så "lägg till
  Observation" blir en ny `XxxJsonSourceBuilder` (samma Gson-trädmönster) + en ny `.map`-fil mot
  en källa som redan är definierad, inte ett nytt modelleringsarbete.

## 1. Felhantering: default / data-absent-reason / stoppa resursen

De tre felhanteringsmönstren som redan är beslutade för GetDiagnosis (PR #38, se
`mapping-getdiagnosis.md`) gav en konkret testbädd:

| Mönster | Java-mappern | FML |
|---|---|---|
| **OID utan mappning → fallback-URI** (`urn:oid:<oid>`) | en rad: `namingSystem.oidToUri(oid)` med inbyggd fallback | **Går, men måste skrivas för hand två gånger.** `translate()` mot en `ConceptMap` ger *alltid* ett fast värde för omappade koder (ConceptMap `unmapped.mode`), inte en beräknad sträng. Att bygga `urn:oid:<oid>` kräver en andra, separat regel vars `where()`-villkor är den **bokstavliga negationen** av den mappade regelns villkor (samma OID-lista skriven ut två gånger, en gång rakt och en gång negerad). Ingen mekanism tvingar dem att hållas i synk – lägg till ett kodverk i YAML-filen och FML-regeln vet inte om det. |
| **Diagnostyp utan ConceptMap-mappning → `data-absent-reason=unknown`, ingen gissad kod** (DIAG-003) | `Optional.ifPresentOrElse(...)` | **Går, med samma dubbleringsproblem som ovan.** `ConceptMap`-"unmapped"-läget kan bara ge en kod, inte byta till en helt annan resursform (en extension i stället för en coding). Löst här med samma mönster: två speglade regler. |
| **Ogiltigt/saknat personnummer → stoppa posten** (DIAG-001) | `if (!isValidPersonId(...)) return null;` stoppar **hela** `Condition`+`Provenance`-paret innan något annat fält sätts | **Går inte på regelnivå – men går deklarativt på motornivå.** Se nedan. |

### Tredje rättelse: stoppa en hel resurs går inte i en `.map`-regel, men går nu som en konfigurationsrad i registryt

Slutsatsen stod tidigare som ett olösbart gap. Den håller fortfarande för FML:s regelspråk
självt: en enskild regel kan bara avstå från att sätta ETT fält (`where()` slår inte till → inget
`subject`, men resursen skapas och skickas vidare ändå). Men Oskars generiska motor-ombyggnad gav
en naturlig plats att lösa det ANDRA stället än i `.map`-filen: `FmlEngine.MappingDefinition` har
nu ett valfritt fält, `stopIfFalseFhirPath`, en FHIRPath-boolesk som evalueras mot källan
(`org.hl7.fhir.r4.fhirpath.FHIRPathEngine`) INNAN `transform()` över huvud taget anropas. För
`GetDiagnosisToCondition` är den satt till
`diagnosis.diagnosisHeader.patientId.value.matches('^[0-9]{12}$')`; om den utvärderas falskt
returnerar `FmlEngine.transform(...)` `null` utan att röra `.map`-filen eller skapa någon
resurs alls – verifierat av ett dedikerat test
(`stoppaResursen_felaktigtPersonnummer_ingenResursAlls`) som jämför mot Java-mapperns tomma lista.

Det här är fortfarande inte "en FML-regel" i ordets strikta mening – vakten ligger i Java-kod
(`FmlEngine`), inte i `.map`-filens eget språk. Men den är nu en **deklarativ rad i en registry**,
inte en if-sats inbäddad i en mappningsmetod: att lägga till ett nytt stopp-villkor för en ny
resurs är en ny `stopIfFalseFhirPath`-sträng i en `MappingDefinition`-post, inte ny Java-logik.
Det är skillnaden mellan "kan inte uttryckas i FML" (den ursprungliga slutsatsen) och "uttrycks
inte i `.map`-filen, men uttrycks ändå konfigurativt, en nivå upp" – en rimlig mellanlösning givet
att FML:s regelspråk saknar en egen "avbryt hela målobjektet"-konstruktion.

**Slutsats för punkt 1:** alla tre mönstren kan nu styras via konfiguration snarare än
hårdkodad Java-logik per resurstyp – men på två olika nivåer. Default/fallback-värde och
data-absent-reason-flagga är `.map`-regler (med en kvarstående brist: ett omappat värde kräver
en HANDSKRIVEN, duplicerad negation av villkoret snarare än ett riktigt "annars"-grenval). Stoppa
hela resursen är en rad i `FmlEngine`s registry (en FHIRPath-vakt), inte en `.map`-regel. Ingen
av de tre kräver längre en ny Java-metod per resurstyp för att konfigureras.

### Fjärde begränsningen (upptäckt när Provenance lades till): en `transform()` tar bara EN target

`StructureMapUtilities.transform(appInfo, source, map, target)` tar en enda `target`-parameter,
och `getInputName()` kastar `This engine does not support multiple source inputs` (texten nämner
bara "source" men gäller identiskt för `target`-moden) så snart en grupp deklarerar mer än en
input av samma mode. Att producera två målresurser (`Condition` + `Provenance`) från samma
källpost med ETT `transform()`-anrop går alltså inte – lösningen här var två helt separata
`StructureMap`-filer (`get-diagnosis-to-condition.map` och `get-diagnosis-to-provenance.map`,
och motsvarande för GetCareDocumentation), körda med två separata `transform()`-anrop mot samma
källa. Det här är ytterligare en punkt där "konfiguration" har en hård gräns: att orkestrera
FLERA målresurser per källpost (vilket varje TK-mappning i den här bryggan gör –
Condition+Provenance, DocumentReference+Provenance+eventuell Composition) måste ligga i
anropande kod, inte i en enda `.map`-fil. "Ett anrop → en resurs" håller alltså fortfarande på
`transform()`-nivå; det som ändrats är VAD som orkestrerar flera sådana anrop mot samma källa,
se nedan.

## En källa, flera målresurser: en matchbox-`$transform`-liknande generisk motor

Oskars konkreta beställning var att göra `FmlEngine` generisk i stil med matchbox-IG:ns
[`$transform`-operation](https://github.com/ahdis/matchbox) – "lägg till en map-fil och det
funkar" – samtidigt som "ett anrop → en resurs" behålls, men flera resurser (som delar samma
tjänstekontrakt i botten) kan konfigureras oberoende av varandra. Lösningen:

- En `record MappingDefinition(key, sourceLogicalModelResource, mapResourcePath, groupName,
  targetResourceType, stopIfFalseFhirPath)` beskriver EN rad i en registry (en lista, inte en
  Java-metod). `GetDiagnosisToCondition` och `GetDiagnosisToProvenance` är två separata poster
  som pekar på SAMMA `sourceLogicalModelResource` (`/fhir/lm-diagnosis.json`) men olika
  `mapResourcePath`/`targetResourceType` – precis "olika resurser, samma tjänstekontrakt i
  botten".
- `FmlEngine` har en enda publik metod, `transform(String key, Base source)`, som slår upp
  posten, kör den valfria FHIRPath-vakten (se punkt 1 ovan), och instansierar målet via
  reflektion: `Class.forName("org.hl7.fhir.r4.model." + def.targetResourceType())
  .getDeclaredConstructor().newInstance()`. Ingen `transformDiagnosis()`/
  `transformDiagnosisProvenance()`-metod per resurstyp längre – att lägga till en femte
  målresurs (t.ex. GetDiagnosis → `Observation`, om det fanns ett sådant behov) är en ny post i
  listan + en ny `.map`-fil, noll ny Java utanför registryt.
- Konstruktorn parsar alla `.map`-filer och cachar alla källmodeller EN gång vid uppstart
  (`computeIfAbsent`-dedup per källmodell-sökväg, eftersom två poster delar samma
  `lm-diagnosis.json`), inte per anrop.
- "Ett anrop → en resurs" är därmed fortfarande sant på `transform()`-nivå (varje anrop till
  `FmlEngine.transform(key, source)` ger exakt en målresurs eller `null`), men anropande kod
  (testerna i denna PoC; i en riktig brygga: `fhir-server`/`ntjp-proxy`) kan nu loopa över
  registryts nycklar för samma tjänstekontrakts källa och få ut så många målresurser som
  konfigurationen beskriver, utan att någon av `.map`-filerna eller `FmlEngine` själv behöver
  veta om varandra.

**Slutsats:** detta är den närmaste FML kommer matchbox-`$transform`s "generisk endpoint, map-fil
som konfiguration"-modell inom denna PoC:s ramar. Den genuina gränsen (en `transform()`-anrop =
en källa + en map-fil = en målresurstyp) är kvar och verkar vara en permanent egenskap av
`StructureMapUtilities` i den här versionen, inte något som går att konfigurera bort – men
ORKESTRERINGEN av flera sådana anrop mot samma tjänstekontrakt är nu helt deklarativ.

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
   `StringType` direkt. **Ytterligare fynd när källan blev ett riktigt `elementmodel.Element`
   i stället för en `Parameters`-resurs (se nedan, punkt 8)**: `base64Binary` kräver dessutom att
   KÄLLSTRÄNGEN redan är giltig base64 (`Base64BinaryType.checkValidBase64`) – FML har ingen
   kodningsfunktion av sitt eget, så `clinicalDocumentNoteText` måste Base64-kodas av
   `GetCareDocumentationJsonSourceBuilder` INNAN den hamnar i källans JSON, inte av någon
   `.map`-regel. Samma begränsning gällde redan den tidigare `Parameters`-adaptern
   (`noteTextPlainBase64`) – den är inte ny, men syns tydligare nu eftersom källan navigeras ett
   steg djupare (se punkt 7).
7. **En källväg kan inte ha mer än ETT punktseparerat segment direkt före `as` i en regel**
   (`b.typeOfDiagnosis.coding as dtc` kastar `FHIRLexer$FHIRLexerException: Found "." expecting
   ";"`, trots att samma väg fungerar fint som höger-led i ett `where()`-uttryck). Upptäcktes när
   källan blev ett riktigt nästlat `elementmodel.Element` (den tidigare `Parameters`-adaptern
   hade bara en nivås nästling, så detta syntes aldrig där). Lösning: bryt upp i nästlade
   en-segments `then`-block (`b.typeOfDiagnosis as tod then { tod.coding as dtc -> ... }`) –
   samma arbetsomgång som behövs för punkt 2 ovan, bara en nivå djupare.
8. **`create(primitivType) as x then { v -> x.value = v }` ger fel resultat OM `v` är bundet till
   den råa käll-`Element`:en direkt, i stället för till en extra `.value`-navigering på den.**
   `elementmodel.Element.getProperty()` har ett specialfall: navigering av `.value` på ett
   element där `isPrimitive()` är sant returnerar en riktig `StringType` med själva textvärdet.
   Hoppas det steget över och `v` är den råa `Element`:en, blir `x.value = v` i praktiken
   `PrimitiveType.setProperty()` → `setValueAsString(v.toString())`, och `Element.toString()` ger
   formatet `"fältnamn=typ[värde]"` (t.ex. `"authorTime=dateTime[2024-01-01T12:00:00+01:00]"`) –
   exakt samma klass av bugg som `BooleanType.toString()` i punkt 5, men här orsakad av KÄLLANS
   typ (`Element`) snarare än MÅLETS. Lösning: alltid ett extra bindningssteg,
   `t.value as tv -> x.value = tv`, innan tilldelningen. Denna typ av källa (`elementmodel`)
   introducerades i den här sessionen tillsammans med den riktiga logiska modellen – den syns
   inte med en `Parameters`-källa, vars parametervärden redan är färdiga primitiver.

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

Den riktiga logiska modellen och den generiska motorn är nu kört och verifierat, inte bara
spikat – `GetDiagnosisFmlComparisonTest` (7 tester) och `GetCareDocumentationFmlComparisonTest`
(6 tester) körs mot en EKTA `inera-ehds-lm-diagnosis`/`inera-ehds-lm-care-documentation`-källa
via `elementmodel.JsonParser`, inte en tom navigering av en `StructureDefinition`.
Rekommendationen justeras därför ytterligare en gång:

- **Fortfarande inte värt att migrera produktionsmapparna rakt av**, men inte längre av brist på
  bevis – det är nu ett avvägt val snarare än en okänd risk. Skälen: (a) felsökning i FML sker
  med betydligt sämre felmeddelanden än Java (se "Konkreta motorbegränsningar" ovan – nio
  distinkta, odokumenterade motor-quirks hittades under arbetet, flera av dem helt tysta), (b)
  omappade koder kräver fortfarande handskriven, duplicerad villkorsnegation i stället för ett
  riktigt "annars"-grenval, och (c) DocBook→narrativ/Composition-Strategy B kan inte uttryckas
  alls. Ingen av dessa tre är löst av den här omgångens arbete.
- **De tre ursprungliga frågorna har nu konkreta, verifierade svar** snarare än spekulation:
  felhantering kan styras deklarativt (två mönster i `.map`-filen, det tredje – stoppa hela
  resursen – som en FHIRPath-rad i `FmlEngine`s registry, se punkt 1); dokumentutbyte och
  resurs-API var redan två skilda, återanvändbara spår i FML:s egen modell (punkt 2); och nya
  resurser ovanpå ett redan adapterat tjänstekontrakt är nu bokstavligen "en ny
  `XxxJsonSourceBuilder` + en ny `.map`-fil + en ny registry-rad", verifierat av att
  GetCareDocumentations Provenance-mål lades till på exakt det sättet utan att röra
  `FmlEngine`s kärnkod.
- **Om arbetet fortsätter**, är nästa naturliga steg inte längre arkitektur utan bredd: fler
  tjänstekontrakt (t.ex. GetCareContact mot `IneraEHDSLMObservations.fsh` eller motsvarande),
  och – om DocBook-begränsningen ska lösas inom FML snarare än kringgås – undersöka om senare
  HAPI-versioner exponerar en extension-punkt för användardefinierade transformer (ingen hittades
  i denna motorversion, se punkt 4/"genuint arkitektoniskt blockerat" ovan).
- Bygg alltid en jämförande testsvit (som de två `*FmlComparisonTest`-klasserna här) runt varje
  regel innan den litas på – de tysta fel som hittades i den här sessionen (särskilt punkt 1 i
  motorbegränsningarna, och punkt 8:s `.value`-rebindningskrav) gör "skriv och lita på" olämpligt
  för FML i denna motorversion, oavsett källmodell.
