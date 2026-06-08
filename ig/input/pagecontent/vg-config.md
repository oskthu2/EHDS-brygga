# VG-konfiguration (`vg-config.yaml`)

`vg-config.yaml` är EHDS-brygganss enda källa för VG-anslutningsinformation.
Filen styr tre saker samtidigt:

| Syfte | Beteende |
|---|---|
| **Auktorisering** | Okänt `vgHsaId` i URL:en → 404 på `/metadata`; tom Bundle på resursanrop |
| **CapabilityStatement** | `/{vgHsaId}/fhir/metadata` returnerar ett VG-specifikt CapabilityStatement med enbart de resurstyper som VG:n stöder |
| **Routing** | `endpointUrl` avgör om anropet går via ntjp-proxy (TK) eller direkt mot ett nativt FHIR-API |
| **Tjänstekatalog** | Registret visar vilka VG:er som tillhandahåller vilka resurstyper och hur — underlag för eHM:s tjänstekatalog |

## Konfigurationsformat

```yaml
vgConfigs:
  - vgHsaId: SE2321000016-4HK5          # VG-identifierare (logisk adress i NTjP)
    description: "VGR – Västra Götalandsregionen"
    resources:
      Condition:                          # FHIR-resurstyp
        access: tk                        # "tk" = via ntjp-proxy, "fhir" = nativt FHIR-API
        endpointUrl: http://ntjp-proxy:8091/fhir/SE2321000016-4HK5
      DocumentReference:
        access: tk
        endpointUrl: http://ntjp-proxy:8091/fhir/SE2321000016-4HK5

  - vgHsaId: SE2321000098-7XYZ
    description: "SLL – Stockholms läns landsting"
    resources:
      Condition:
        access: tk
        endpointUrl: http://ntjp-proxy:8091/fhir/SE2321000098-7XYZ
      DocumentReference:
        access: tk
        endpointUrl: http://ntjp-proxy:8091/fhir/SE2321000098-7XYZ
```

## Fält

| Fält | Obligatorisk | Beskrivning |
|---|---|---|
| `vgHsaId` | Ja | VG:ns HSA-id. Används som logisk adress mot NTjP och som URL-segment i `/{vgHsaId}/fhir/`. |
| `description` | Nej | Fritext, används enbart för läsbarhet. |
| `resources` | Ja | Map från FHIR-resurstyp (t.ex. `Condition`, `DocumentReference`) till routing-konfiguration. |
| `resources.<typ>.access` | Ja | `tk` — anrop routas via ntjp-proxy med RIVTA SOAP. `fhir` — anrop går direkt mot VG:ns egna FHIR-API. |
| `resources.<typ>.endpointUrl` | Ja | URL till endpoint. För `access: tk` pekar detta på ntjp-proxy; för `access: fhir` direkt på VG-systemet. |

## Semantik

### Auktorisering och discovery

Bryggan tillåter enbart anrop mot `vgHsaId`-värden som finns i `vg-config.yaml`.

`GET /{vgHsaId}/fhir/metadata` returnerar ett VG-specifikt CapabilityStatement som
enbart listar de resurstyper vars `resources`-post finns i konfigurationen. Okänt
`vgHsaId` ger `404 Not Found`. En konsument kan alltså använda `metadata`-endpointen
för att utforska vad en specifik VG tillhandahåller innan ett patientanrop initieras.

`GET /.well-known/smart-configuration` returnerar gemensam SMART-konfiguration
(authorization, token, introspection, scopes) för hela bryggan.

En VG som saknar post för en given resurstyp anses inte tillhandahålla den resursen —
resurstypen syns inte i CapabilityStatement och anrop returnerar en tom Bundle.

### Routing

`access`-fältet styr om anropet hanteras som RIVTA SOAP (via ntjp-proxy) eller
som nativt FHIR. fhir-server vet inte vilket: den anropar alltid `endpointUrl` med
standard FHIR HTTP GET — ntjp-proxy-sidan hanterar SOAP-translationen internt.

En VG kan använda olika accessmetoder per resurstyp:

```yaml
resources:
  Condition:
    access: tk          # Diagnoser via TK/SOAP
    endpointUrl: http://ntjp-proxy:8091/fhir/SE2321000016-4HK5
  Observation:
    access: fhir        # Observationer direkt mot VG:ns FHIR-server
    endpointUrl: https://fhir.vgr.se/api/r4
```

### Tjänstekatalog

`vg-config.yaml` utgör en maskinläsbar katalog av vilka VG:er som erbjuder vilka
resurstyper och via vilken accessmetod. eHM kan läsa denna fil för att:

- Lista anslutna VG:er och deras stödda FHIR-resurstyper
- Avgöra om en VG-resurs-kombination är tillgänglig innan ett anrop initieras
- Presentera kapabilitetsinformation i en tjänstekatalog

## Lägga till en ny VG

1. Kontakta VG-administratören och bekräfta HSA-id och vilka resurstyper som ska tillhandahållas
2. Lägg till ett nytt block i `vg-config.yaml` med rätt `vgHsaId` och `resources`-poster
3. Sätt `access: tk` och peka `endpointUrl` mot ntjp-proxy-instansen för den VG:n,
   eller `access: fhir` mot VG:ns egna FHIR-server om sådan finns
4. Ingen kodändring i fhir-server eller ntjp-proxy krävs

## Lägga till en ny resurstyp för befintlig VG

1. Implementera TK-specifik mappningsklass och ntjp-proxy-kontroller (se [Arkitektur](architecture.html))
2. Lägg till resurstypens post under VG:ns `resources`-block i `vg-config.yaml`

Bryggan aktiverar automatiskt routing för den nya resurstypen vid nästa start.
