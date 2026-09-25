# NDI:s användningsscenarier och EHDS-bryggans roll

Det här dokumentet kartlägger E-hälsomyndighetens fem publicerade användningsscenarier för
den nationella digitala infrastrukturen (NDI) mot EHDS-bryggans arkitektur: vilka scenarier
bryggan redan har stöd för i sitt nuvarande anropsflöde, vilka som kräver ändringar, och vilka
som medvetet lämnas utanför den här PoC:n.

**Källa:** E-hälsomyndighetens portal, "Användningsscenarier"
(`portal.ehalsomyndigheten.se/forutsattningar-och-ramverk/vad-ar-nationell-digital-infrastruktur/anvandningsscenarier`),
sidan uppdaterad 27 augusti 2026. Scenarierna är konceptuella processbeskrivningar, inte
tekniska specifikationer — illustrationerna "visar inte exakt teknisk implementation" enligt
källan själv.

## NDI-komponenter som förekommer i scenarierna

| Förkortning | Namn | Roll |
|---|---|---|
| **PDI** | Patientdataindex | Register över vilka vårdgivare som har hälsodata om en given patient |
| **NTK** | Nationell tjänsteadresseringskatalog | Adressuppslag: vilken fysisk endpoint som svarar för en vårdgivares tjänst |
| **NSF** | Nationell spärrfunktion | Kontroll av patientens spärrar innan hälsodata lämnas ut till personal |
| **NFF** | Nationell företrädarfunktion | Fullmaktskontroll för ombud som agerar å en patients vägnar |
| **NCP** | Nationell kontaktpunkt | Gränsöverskridande utbyte med andra EU-länders motsvarande kontaktpunkt |

## De fem scenarierna, kort

| # | Scenario | Aktör | Involverar |
|---|---|---|---|
| 1 | Personal registrerar hälsodata i Sverige | Vårdpersonal | PDI (registrering) |
| 2 | Patient tittar på egen hälsodata | Patient | PDI, NTK |
| 3 | Patientens ombud tittar på hälsodata | Ombud | PDI, NTK, **NFF** |
| 4 | Personal tittar på hälsodata i Sverige | Vårdpersonal | PDI, NTK, **NSF** |
| 5 | Personal tittar på hälsodata i utlandet | Vårdpersonal | NTK, **NCP** |

I samtliga läs-scenarier (2–5) är mönstret detsamma: en tillgångstjänst slår upp *var* data
finns (PDI/NTK), anropar sedan varje vårdinformationssystem direkt, och varje
vårdinformationssystem fattar sitt eget åtkomstbeslut, lämnar ut data och loggar lokalt —
innan tillgångstjänsten sammanställer och loggar sin egen utlämning. **EHDS-bryggan är,
konceptuellt, ett vårdinformationssystem i den bemärkelsen**: den sitter på producentsidan av
detta anropsmönster, inte på tillgångstjänstsidan. Det är alltså bryggans roll i "Vårdinformationssystem"-swimlanen
i respektive scenario som är relevant nedan, inte hela flödet.

## Scenario 1: Personal registrerar hälsodata i Sverige

Vårdpersonal journalför i sitt EHDS-förordningens "elektroniska hälsodokumentationssystem".
Systemet loggar åtkomsten lokalt och — vid patientens första kontakt med vårdgivaren — anropar
PDI för att registrera att vårdgivaren nu har uppgifter om patienten.

**Bryggans roll:** Ingen idag. Bryggan är renodlat läsande (RIVTA SOAP → FHIR, ingen
persistent lagring, inga skrivande tjänstekontrakt) och deltar inte i journalföringsflödet.
Skrivsidans PDI-registrering är en angelägenhet för respektive vårdgivares egna
vårdinformationssystem, inte för bryggan.

**Status:** Utanför scope.

## Scenario 2: Patient tittar på egen hälsodata via tillgångstjänst

Tillgångstjänsten slår upp PDI + NTK, anropar varje vårdinformationssystem direkt, och varje
system "fattar ett åtkomstbeslut, returnerar efterfrågade hälsodata och loggar att en åtkomst
skett lokalt" — **utan** något NSF-steg. Patienter har rätt att se sin egen hälsodata i sin
helhet; det är därför ingen spärrkontroll med i det här scenariots vårdinformationssystem-lane.

**Bryggans roll:** Samma anropskontrakt som bryggans befintliga VG-scopade `GET
{vgHsaId}/fhir/Condition`-flöde skulle kunna återanvändas för en patient-tillgångstjänst.
Men bryggan gör idag **ingen skillnad på vem som frågar** — `SparrFilterService` körs
ovillkorligt på varje svar, oavsett om anroparen är vårdpersonal eller patienten själv. Om
bryggan ska stödja scenario 2 korrekt måste anropskontexten (patient kontra personal, se
[Audit-händelser](../ig/input/pagecontent/audit-events.md)s `agent[user]`/JWT-claims) fram
till orkestreringen, så att Sparr kan stängas av för patients egen åtkomst.

**Status:** Delvis — samma tekniska mönster, men kräver en ny caller-context-mekanism för att
undvika att patienter felaktigt spärrfiltreras mot sig själva.

## Scenario 3: Patientens ombud tittar på hälsodata via tillgångstjänst

**Parkerat, medvetet.** Se eget avsnitt nedan.

## Scenario 4: Personal tittar på hälsodata i Sverige via tillgångstjänst

Tillgångstjänsten slår upp PDI + NTK, anropar varje vårdinformationssystem, och varje system
"hämtar patientens allmänna spärrar om sådana finns och fattar åtkomstbeslut" (NSF) innan det
"returnerar hälsodata som inte är spärrad" och loggar.

**Bryggans roll:** Detta är i praktiken bryggans **befintliga huvudflöde**. `fhir-server`
tar emot anropet, `SparrFilterService` gör en post-query-spärrkontroll mot
`careProviderHSAId`/`careUnitHSAId` (se arkitekturbeslutet i
[README](../README.md#post-query-spärr-på-organisations--och-avdelningsnivå)), och
`LoggService` loggar utlämningen som `AuditEvent` (se
[Audit-händelser](../ig/input/pagecontent/audit-events.md)). Två skillnader mot NDI:s
målbild är dock värda att notera:

- **NSF vs. `mock-sparr`:** dagens spärrmock modellerar en äldre, egen
  "Säkerhetstjänsten"-liknande kontrakt, inte NDI:s NSF-specifikation. Kontraktsformatet är
  inte verifierat mot NSF.
- **PDI vs. Engagemangsindex (EI):** bryggan har redan ett liknande koncept — EI
  (`mock-ei` / `EiService`, se [t2-katalogtjänster](t2-katalogtjanster.md)) — som avgör vilka
  VG:er som ska frågas vid ett oscopat ("alla VG") anrop. PDI och EI löser konceptuellt samma
  problem (vilka vårdgivare har data om patienten), men det är i dagsläget en tillgångstjänsts
  jobb att fråga PDI, inte bryggans — bryggans EI-anrop sker internt, för att slå upp *vilka
  VG-scopade anrop bryggan själv ska göra*. Relationen mellan dessa två mekanismer, om NDI:s
  PDI ska ersätta eller komplettera EI, är oklar och bör klargöras innan produktion.

**Status:** Störst överlapp med befintlig arkitektur av de fem scenarierna. Gapen är
kontraktsverifiering (NSF), inte arkitektur.

## Scenario 5: Personal tittar på hälsodata i utlandet via tillgångstjänst

Den svenska tillgångstjänsten anropar en nationell kontaktpunkt (NCP), som i sin tur anropar
motsvarande kontaktpunkt i det andra landet. Scenariot beskriver det **utgående** flödet:
svensk personal som vill se en patients data från ett annat land.

**Bryggans roll:** Ingen i den riktning scenariot beskriver — bryggan är inte en NCP och
initierar inga gränsöverskridande anrop. Det mer sannolika framtida beröringspunkten är den
**inkommande** riktningen som scenariot inte beskriver: utländsk personal som via sitt lands
NCP vill se svensk hälsodata. I det fallet skulle den svenska NCP:n anropa vårdinformationssystem
på samma sätt som i scenario 4, och bryggan skulle spela samma roll — men det är spekulation,
inte något NDI hittills publicerat ett scenario för.

**Status:** Utanför scope. Ingen NCP-integration finns eller planeras i den här PoC:n.

## Sammanfattning: NDI-komponent → bryggans motsvarighet idag

| NDI-komponent | Bryggans motsvarighet idag | Gap |
|---|---|---|
| **PDI** | Saknas (delvis överlappande med EI, se scenario 4) | Ingen PDI-integration; relationen till EI oklar |
| **NTK** | `mock-tjanstekatalog` (T1), se [t2-katalogtjänster](t2-katalogtjanster.md) | Samverkansmönstret (T1) är verifierat mot Ineras demomiljö, men NDI:s eget NTK-kontrakt är inte bekräftat identiskt |
| **NSF** | `mock-sparr` / `SparrFilterService` | Äldre/eget kontrakt, inte NDI:s NSF-specifikation |
| **NFF** | Saknas helt | **Parkerat** — se nedan |
| **NCP** | Saknas helt | Utanför scope; endast relevant om Sverige blir mottagare av inkommande gränsöverskridande frågor |

---

## Parkeringsplats: Scenario 3 — Ombud

**Detta scenario är medvetet inte designat i den här iterationen.** Beslutet togs
2026-09-02: ombuds tillgång är ett verkligt behov (NDI-scenario 3), men läggs åt sidan tills
vidare för att inte blockera arbetet med scenario 2 och 4. Det här avsnittet är en
parkeringsplats för frågor och antaganden att ta upp igen när scenariot prioriteras — inte en
lösning.

### Vad scenariot kräver, enligt källan

Flödet är identiskt med scenario 2 (patient ser egen data), förutom två extra steg:

1. Tillgångstjänsten anropar **NFF** för att hämta och visa en lista över vilka personer den
   inloggade användaren är ombud för.
2. Vid själva datauttaget kontrollerar varje vårdinformationssystem — utöver sitt vanliga
   åtkomstbeslut — att ombudet faktiskt får hämta den efterfrågade hälsodata genom att hämta
   ombudets fullmakt från NFF, och loggar att "ett utlämnande" (inte bara en åtkomst) skett.

### Öppna frågor att ta ställning till när scenariot återupptas

- **Hur känner bryggan igen ett ombudsanrop?** Idag skiljer `LoggService` på `agent[system]`
  och `agent[user]` via JWT-claims (`client_id`/`azp` respektive `fhirUser`/`sub`, se
  [Audit-händelser](../ig/input/pagecontent/audit-events.md)). Ombud kräver sannolikt ett
  tredje begrepp — vem är inloggad (ombudet) kontra vem ärendet gäller (huvudmannen) — som inte
  finns i dagens claim-modell.
- **Var görs fullmaktskontrollen?** Enligt scenariot är det vårdinformationssystemet (=
  bryggan, i vår swimlane-tolkning) som hämtar fullmakten från NFF och fattar beslutet — inte
  tillgångstjänsten. Det innebär ett nytt, synkront NFF-anrop i bryggans orkestrering, analogt
  med hur `SparrFilterService` anropas idag efter att SOAP-svaret mappats.
- **Är utlämning till ombud och till patienten själv samma spärrpolicy?** Källan skiljer inte
  ut den frågan; värt att klargöra om ombudsflödet ska ärva scenario 2:s "ingen NSF"-princip
  eller ha egna regler.
- **Loggningskrav:** scenariot använder specifikt ordet "utlämnande" i sista steget, vilket kan
  vara en signal om att ombudsutlämning ska loggas annorlunda än vanlig åtkomst — jämför med
  hur `SEEHDSAuditEventSparrFilter` redan skiljer på loggningstyper.

Ingen av dessa frågor besvaras här. De dokumenteras för att göra det billigare att plocka upp
scenariot senare, inte för att styra en framtida lösning i förväg.
