# Mappning: GetCareDocumentation → DocumentReference

## Bakgrund

RIVTA-tjänstekontraktet `clinicalprocess:healthcond:description:GetCareDocumentationResponder:3`
(förkortat `GetCareDocumentation:3`) används för att hämta journalanteckningar ("anteckningar")
från svenska journalsystem. Kontraktet krävs för både NPÖ och 1177 Journal (v3.0).

EHDS-bryggan mappar svarsmeddelandet från detta tjänstekontrakt till FHIR R4-resursen
[DocumentReference](https://hl7.org/fhir/R4/documentreference.html), profilerad som
[SEEHDSDocumentReference](StructureDefinition-se-ehds-document-reference.html).

> **Viktigt — JoL-header v2.2 (inte PatientSummaryHeader)**
>
> Till skillnad från `GetDiagnosis:2` använder `GetCareDocumentation:3` **JoL-header v2.2**.
> PDL-fälten för Sparr kommer **direkt från `accessControlHeader`**, inte från ett nästlat
> `accountableHealthcareProfessional`-block:
>
> - Yttre Sparr: `careDocumentation.header.accessControlHeader.accountableHealthcareProvider`
> - Inre Sparr: `careDocumentation.header.accessControlHeader.accountableCareUnit`

Varje `careDocumentation`-post i svaret ger upphov till exakt en `DocumentReference`.

## Mappningstabell — careDocumentation.header (accessControlHeader)

| RIVTA-element | FHIR-element | Kommentar |
|---|---|---|
| `accessControlHeader.patientId.extension` | `DocumentReference.subject.identifier.value` | Personnummer eller samordningsnummer |
| `accessControlHeader.patientId.root` | `DocumentReference.subject.identifier.system` | OID→URI, se tabell nedan |
| `accessControlHeader.accountableHealthcareProvider` | `Provenance.agent[custodian].who.identifier` | **Yttre Sparr** — läses direkt från accessControlHeader, inte från ett author-block |
| `accessControlHeader.accountableCareUnit` | `Provenance.agent[author].who.identifier` | **Inre Sparr** — läses direkt från accessControlHeader |
| `accessControlHeader.careProcessId` | `DocumentReference.context.related[0].identifier.value` | Referens till individanpassad vårdprocess |
| `accessControlHeader.blockComparisonTime` | `DocumentReference.extension[ext-block-comparison-time]` | Tidpunkt för Sparr-jämförelse; YYYYMMDDHHMMSS → ISO 8601 |
| `accessControlHeader.approvedForPatient` | *Ej mappad* | Se [PDL-001](#öppna-frågor) — inget beslutat FHIR-kodsystem för `meta.security` ännu |

## Mappningstabell — careDocumentation.header (sourceSystemId, record, author, signature)

| RIVTA-element | FHIR-element | Kommentar |
|---|---|---|
| `header.sourceSystemId` | `DocumentReference.meta.source` | Format `urn:oid:1.2.752.129.2.1.4.1#{hsaId}` |
| `header.record.recordId` | `DocumentReference.masterIdentifier.value` | Källsystemets primärnyckel |
| `header.record.timestamp` | `DocumentReference.date` | Journalpostens skapandetid; YYYYMMDDHHMMSS → ISO 8601 |
| `header.author.authorId` | `DocumentReference.author[0]` (Reference(PractitionerRole)) | Logisk referens via HSA-id |
| `header.author.name` | `DocumentReference.author[0].display` | Se [PractitionerRole-förenkling](#practitionerrole-förenkling) nedan |
| `header.author.timestamp` | `Provenance.recorded` | Se [DOC-002](#öppna-frågor) för fallback när `author` saknas |
| `header.author.byRole`, `header.author.orgUnit` | *Ej mappade* | Se [PractitionerRole-förenkling](#practitionerrole-förenkling) |
| `header.signature.signatureId` | `DocumentReference.authenticator` (Reference(PractitionerRole)) | Logisk referens via HSA-id |
| `header.signature.name` | `DocumentReference.authenticator.display` | Se [PractitionerRole-förenkling](#practitionerrole-förenkling) |
| `header.signature.timestamp` | `DocumentReference.extension[ext-signature-time]` | Se [DOC-003](#öppna-frågor) — fältet är valfritt (0..1) i RIVTA:t |
| `header.signature.byRole` | *Ej mappad* | Se [PractitionerRole-förenkling](#practitionerrole-förenkling) |

## Mappningstabell — careDocumentation.body

| RIVTA-element | FHIR-element | Kommentar |
|---|---|---|
| `body.clinicalDocumentNoteCode` | `DocumentReference.type` | Kodsystem ClinicalDocumentNoteCodeCS (OID `1.2.752.129.2.2.2.11`) — se värdegrupp nedan |
| `body.clinicalDocumentNoteTitle` | `DocumentReference.description` samt `content.attachment.title` | Anteckningens titel |
| `body.clinicalDocumentNoteText` | `DocumentReference.content[0].attachment.data` | Fritext, kodas base64 med `contentType: text/plain; charset=utf-8`. XOR med `multimediaEntry` — se [DOC-004](#öppna-frågor) |
| `body.multimediaEntry.mediaType` | `DocumentReference.content[0].attachment.contentType` | MIME-typ (t.ex. `application/pdf`, `image/jpeg`) |
| `body.multimediaEntry.value` | `DocumentReference.content[0].attachment.data` | Redan base64-kodad binärdata från RIVTA — avkodas och skickas vidare oförändrad, inte dubbelkodad |
| `body.multimediaEntry.reference` | `DocumentReference.content[0].attachment.url` | URL till externt dokument. XOR med `value` inom `multimediaEntry` |
| `body.dissentingOpinion[i]` | `DocumentReference.extension[dissenting-opinion][i]` | Se [dissentingOpinion](#dissentingopinion) nedan |

### Värdegrupp — clinicalDocumentNoteCode

| Kod | Anteckningstyp |
|---|---|
| `utr` | Planeringsanteckning (Utredning/planering) |
| `atb` | Åtgärdsanteckning (Åtgärdsbeskrivning) |
| `sam` | Sammanfattande anteckning |
| `sao` | Samordningsanteckning (Samordnad bedömning) |
| `ins` | Inskrivningsanteckning |
| `slu` | Utskrivningsanteckning (Slutanteckning) |
| `auf` | Akutanteckning (Akutanteckning/frisk) |
| `sva` | Specialistanteckning (Specialistvårdsanteckning) |
| `bes` | Besöksanteckning |

Bryggan mappar koden och kodsystemets OID rakt igenom via `NamingSystemRegistry`
(`type.coding.system`, `type.coding.code`, `type.coding.display`) — ingen ConceptMap-översättning
görs, till skillnad från t.ex. diagnostyp i GetDiagnosis-mappningen. `type.text` sätts från
`originalText` om det finns, annars `displayName`.

## Härledda fält / Designbeslut

### DocumentReference.status

`careDocumentation.body` innehåller inget statusfält. `DocumentReference.status` sätts därför
alltid till `current` — antagandet är att källsystemet endast returnerar aktiva, giltiga
journalanteckningar. Makulerade anteckningar förväntas inte returneras.

### XOR-villkor för body-innehåll

Exakt ett av `clinicalDocumentNoteText` och `multimediaEntry` ska förekomma per post, och inom
`multimediaEntry` är `value` och `reference` ömsesidigt uteslutande:

```
invariant: getcaredocumentation-body-xor
description: "Antingen clinicalDocumentNoteText eller multimediaEntry ska anges, ej båda"
expression: "clinicalDocumentNoteText.exists() xor multimediaEntry.exists()"

invariant: getcaredocumentation-multimedia-xor
description: "Antingen value eller reference ska anges i multimediaEntry, ej båda"
expression: "value.exists() xor reference.exists()"
```

Dessa invarianter uttrycks mot **RIVTA-källstrukturen**, inte mot `DocumentReference`:
`SEEHDSDocumentReference` har inga separata `clinicalDocumentNoteText`/`multimediaEntry`-element
att skriva en FHIRPath-regel mot — båda grenarna mynnar ut i samma `content.attachment`. De
deklareras därför inte som FSH `Invariant`-block på profilen, utan dokumenteras här som
förutsättningar bryggan litar på att producenten uppfyller. Mappern själv validerar inte detta
strikt: den använder `clinicalDocumentNoteText` om den finns, annars `multimediaEntry` — vilket
respekterar XOR i den lyckade vägen utan att avvisa en malformad post.

### PractitionerRole-förenkling

Bryggan bygger, precis som för `GetDiagnosis` (`recorder`/`asserter`), **logiska referenser**
mot `PractitionerRole` — den materialiserar aldrig en faktisk `PractitionerRole`-resurs. Det
begränsar vad som kan bäras: `author.name`/`signature.name` placeras i `Reference.display`
(FHIR:s avsedda fält för läsbar text vid en referens), men `byRole` (yrkesroll) och `orgUnit`
(organisationsenhet) saknar en motsvarande plats på en ren `Reference` och mappas därför inte.
Detta är en medveten PoC-begränsning, inte ett förbiseende — att fixa det kräver att bryggan
börjar producera `contained`- eller fristående `PractitionerRole`-resurser, vilket är en större
arkitekturförändring som påverkar alla TK-mappare, inte bara denna.

### Provenance.recorded — fallback när author saknas

`header.author` är valfri (0..1) men `author.timestamp` är obligatorisk inom blocket. Om
`author` saknas helt finns alltså ingen `author.timestamp` att sätta `Provenance.recorded` från.
Bryggan löser detta genom att falla tillbaka på `header.record.timestamp` (se [DOC-002](#öppna-frågor)):

```
recordedTime = author?.timestamp ?? record?.timestamp
```

### dissentingOpinion

Det finns inget standardiserat FHIR R4-element för avvikande meningar i `DocumentReference`.
Varje `dissentingOpinion`-post mappas till en komplex extension med delfälten `opinionId`
(`valueString`), `authorTime` (`valueDateTime`), `opinion` (`valueString`), `personId`
(`valueIdentifier`, OID→URI via `NamingSystemRegistry`) och `personName` (`valueString`).

**Extension-URL:** `https://fhir.inera.se/StructureDefinition/dissenting-opinion` — detta är en
delad, centralt definierad Inera-extension (inte `ehds-brygga.inera.se`-namnrymden som bryggans
egna extensions använder). Den definieras därför **inte** som en `StructureDefinition` i den här
IG:n — att göra det vore att låtsas äga en canonical URL som tillhör en annan part. Bryggans
mappningskod refererar bara URL:en som den är given.

## hasMore (paginering)

`hasMore 0..*` är ett toppnivåelement i svaret, parallellt med `careDocumentation` och `result`,
och indikerar att ytterligare data kan hämtas via `hasMore[i].logicalAddress` och
`hasMore[i].reference`. Det finns inget FHIR-ekvivalent för detta pagineringsmönster.

**Ej mappat.** Se [DOC-001](#öppna-frågor) — mappern läser inte `hasMore` alls i den här
PoC:n; en konsument som behöver fullständig data för en patient med många anteckningar måste
själv hantera paginering på RIVTA-nivå (vilket bryggan idag inte gör åt den).

## result (tekniska svarsfält)

`result.resultCode` avgör om svaret behandlas alls (`OK` krävs, annars returneras en tom lista).
`result.resultText` är teknisk felbeskrivning och mappas inte till FHIR. Till skillnad från
`GetDiagnosis:2` (som ärver PatientSummaryHeaders `message`/`logId`) har `GetCareDocumentation:3`
bara `resultCode`/`resultText` — ingen `logId`.

## OID-till-URI-mappningar

| OID | URI | Beskrivning |
|---|---|---|
| `1.2.752.129.2.1.3.1` | `http://electronichealth.se/identifier/personnummer` | Personnummer |
| `1.2.752.129.2.1.3.3` | `http://electronichealth.se/identifier/samordningsnummer` | Samordningsnummer |
| `1.2.752.129.2.1.4.1` | `urn:oid:1.2.752.129.2.1.4.1` | HSA-id (Inera NTjP) — author, authenticator, meta.source, Provenance |

`clinicalDocumentNoteCode.codeSystem` (OID `1.2.752.129.2.2.2.11`, ClinicalDocumentNoteCodeCS)
har ingen känd URI-mappning och bevaras som `urn:oid:1.2.752.129.2.2.2.11` av
`NamingSystemRegistry`s fallback.

## Provenance

| Agent | Roll | Källa |
|---|---|---|
| `agent[custodian]` | Juridiskt ansvarig vårdgivare | `accessControlHeader.accountableHealthcareProvider` |
| `agent[author]` | Informationsägande vårdenhet | `accessControlHeader.accountableCareUnit` |
| `agent[assembler]` | EHDS-bryggan | `EHDS_BRIDGE_HSA_ID` (env-variabel) |

`Provenance.target` refererar `DocumentReference` via `urn:uuid:{resurs.id}`.
`Provenance.recorded` = `header.author.timestamp`, med `header.record.timestamp` som fallback
(se [DOC-002](#öppna-frågor)).

## Spärr (Sparr)

Spärrkontrollen läser `careProviderHSAId`/`careUnitHSAId`-motsvarande fält från Provenance —
samma mönster som Condition-flödet via `SparrFilterService`. Skillnaden mot `GetDiagnosis` är
bara var i RIVTA-svaret dessa värden hämtas ifrån (`accessControlHeader` direkt, inte ett
nästlat block) — se [DES-005](#bakgrund) ovan.

| Fält | Källa | Syfte |
|---|---|---|
| `Provenance.agent[custodian].who` | `accessControlHeader.accountableHealthcareProvider` | Yttre spärr (organisationsnivå) |
| `Provenance.agent[author].who` | `accessControlHeader.accountableCareUnit` | Inre spärr (avdelningsnivå) |

## Öppna frågor

| ID | Fråga | Status i denna PoC |
|---|---|---|
| DOC-001 | `hasMore 0..*` saknar FHIR-ekvivalent. | Ej mappat — pagineringen hanteras inte alls; se [hasMore](#hasmore-paginering) |
| DOC-002 | `author.timestamp` saknar källa om `author` helt saknas. | Löst: `Provenance.recorded` faller tillbaka på `record.timestamp` |
| DOC-003 | `signature.timestamp` är valfri, till skillnad från PatientSummaryHeader-konventionens obligatoriska `signatureTime`. | Mappas till `extension[ext-signature-time]` när den finns; ingen ersättning när den saknas |
| DOC-004 | Är `clinicalDocumentNoteText` redan entity-encodad DocBook-text som base64-kodas, eller ska den avkodas först? | Ej löst i denna PoC — texten base64-kodas rakt av som den kommer in. Se `guidance-docbook-narrative.md` för hur DocBook-innehåll *i övrigt* transformeras till FHIR Narrative (`DocBookToNarrativeTransformer`), som ännu inte är kopplad in i denna mappning |
| PDL-001 | `approvedForPatient` saknar ett standardiserat FHIR-kodsystem för `meta.security`. | Ej mappat — kräver ett gemensamt beslut om kodsystem innan det kan implementeras |
| GENERAL-001 | RIVTA-tidsstämplar saknar tidszon; FHIR kräver ISO 8601 med tidszon. | Samma kända PoC-begränsning som gäller `GetDiagnosis` — se `README.md`s PoC-begränsningstabell ("Lokal tidzon") |

## PoC-begränsningar

### Binär dokumentdata
`multimediaEntry.value` avkodas och skickas vidare som `attachment.data` — det är alltså
bryggan själv, inte ett separat GetDocument-anrop, som bär binärdata i den här mappningen
(till skillnad från den tidigare, felaktigt namngivna `GetDocumentList`-mappningen som aldrig
hämtade något innehåll alls). Stora binärfiler base64-kodade direkt i FHIR-svaret kan bli
kostsamt för stora dokument — inte optimerat i denna PoC.

### DocBook-innehåll i clinicalDocumentNoteText
Om `clinicalDocumentNoteText` innehåller DocBook-formaterad text (se DOC-004 ovan och
`guidance-docbook-narrative.md`) transformeras den **inte** till FHIR Narrative i denna
mappning — den base64-kodas som rå text. `DocBookToNarrativeTransformer` finns i
mapping-engine och är fullt enhetstestad, men är inte kopplad in i
`GetCareDocumentationMapper`. Att koppla in den (avgöra Strategy A/B enligt
`guidance-docbook-narrative.md`, och om resultatet ska ersätta eller komplettera
`content.attachment`) är ett uppföljningsarbete, inte del av denna PoC.
