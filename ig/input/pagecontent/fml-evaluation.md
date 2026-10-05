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

- `get-diagnosis-to-condition.map` – en FML-översättning av `GetDiagnosisMapper` (delmängd:
  personnummer→subject, diagnostyp→category via ConceptMap, diagnoskod→code med OID→URI-slagning,
  onset/abatement, meta.source, clinicalStatus/verificationStatus).
- Två `ConceptMap`-resurser (`diagnosis-type`, `codesystem-oid`) som motsvarar de YAML-filer
  Java-mapparna redan läser (`concept-maps/diagnosis-type.yaml`, `naming-systems.yaml`).
- `FmlEngine` – kör `org.hl7.fhir.r4.utils.StructureMapUtilities` offline (ingen
  packages.fhir.org-åtkomst krävs, se "Driftsfynd" nedan).
- `RivtaDiagnosisParametersAdapter` – plattar ut de RIVTA-fält som används till en FHIR
  `Parameters`-resurs (se "Källdata är inte FHIR" nedan för varför).
- `GetDiagnosisFmlComparisonTest` – fyra jämförande tester som körs genom **både** den riktiga
  Java-mappern och FML-motorn på samma indata och jämför resultatet: en happy path, samt de tre
  beslutade negativtest-beteendena från PR #38 (DIAG-001 ogiltigt personnummer, DIAG-003 okänd
  diagnostyp, OID-fallback för okänt kodverk). Alla fyra är gröna.

Kör dem med `cd bridge && mvn test -pl fml-mapping-poc -am`.

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

## Rekommendation

- **Inte värt att migrera befintliga mappare just nu.** Den uppmätta nyttan (deklarativ,
  konfigurerbar mappning) realiseras bara fullt ut för FHIR→FHIR-mappning; för RIVTA-källdata
  krävs ett adapterlager som i praktiken är lika mycket arbete som dagens Java-mappers, och två av
  tre önskade felhanteringsmönster kräver handskriven duplicering eller stannar i Java ändå.
- **Värt att hålla ögonen på för nya, enklare tjänstekontrakt** där källan redan är nära FHIR-form
  eller där en logisk modell är lätt att författa – där ger FML en verklig fördel för att snabbt
  lägga till nya målresurser utan ny Java-kod.
- Om arbetet fortsätter: investera i en riktig logisk modell (inte `Parameters`-plattning) för
  minst ett tjänstekontrakt, och bygg en liten testsvit (som `GetDiagnosisFmlComparisonTest` här)
  runt varje regel innan den litas på – de tysta fel som hittades ovan (punkt 1 särskilt) gör
  "skriv och lita på" olämpligt för FML i denna motorversion.
