#Requires -Version 5.1
<#
.SYNOPSIS
    EHDS-brygga - Comprehensive E2E test suite (PowerShell)
.DESCRIPTION
    Equivalent to scripts/e2e-test.sh for Windows environments.
    Requires the Docker stack to be running (docker compose up -d).
.PARAMETER GatewayUrl
    Base URL of the gateway. Defaults to http://localhost:8080.
.EXAMPLE
    .\scripts\e2e-test.ps1
    .\scripts\e2e-test.ps1 -GatewayUrl http://localhost:9090
#>
param(
    [string]$GatewayUrl = $(if ($env:GATEWAY_URL) { $env:GATEWAY_URL } else { "http://localhost:8080" })
)

$PatientId     = "191212121212"
$PatientSystem = "urn:oid:1.2.752.129.2.1.3.1"
$VgrHsa        = "SE2321000016-4HK5"
$SllHsa        = "SE2321000098-7XYZ"
$enc           = [uri]::EscapeDataString("|")

$Pass = 0
$Fail = 0

function Ok   { param($msg) Write-Host "  PASS  $msg" -ForegroundColor Green; $script:Pass++ }
function Fail { param($msg) Write-Host "  FAIL  $msg" -ForegroundColor Red;   $script:Fail++ }

function CheckStatus {
    param($Desc, $Url, $Expected = 200)
    try {
        $resp = Invoke-WebRequest -Uri $Url -UseBasicParsing -ErrorAction Stop
        $status = [int]$resp.StatusCode
    } catch {
        $status = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { 0 }
    }
    if ($status -eq $Expected) { Ok "$Desc -> HTTP $status" }
    else { Fail "$Desc -> expected $Expected, got $status [$Url]" }
}

function CheckJson {
    param($Desc, $Url, [scriptblock]$Selector, $Expected)
    try {
        $j = Invoke-RestMethod -Uri $Url -UseBasicParsing -ErrorAction Stop
        $actual = & $Selector $j
        if ("$actual" -eq "$Expected") { Ok "$Desc -> $actual" }
        else { Fail "$Desc -> expected '$Expected', got '$actual'" }
    } catch {
        Fail "$Desc -> error: $_"
    }
}

function CheckJsonContains {
    param($Desc, $Url, [scriptblock]$Selector, $Substring)
    try {
        $j = Invoke-RestMethod -Uri $Url -UseBasicParsing -ErrorAction Stop
        $actual = "$(& $Selector $j)"
        if ($actual -like "*$Substring*") { Ok "$Desc -> contains '$Substring'" }
        else { Fail "$Desc -> expected to contain '$Substring', got '$actual'" }
    } catch {
        Fail "$Desc -> error: $_"
    }
}

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " EHDS-brygga - E2E tests"
Write-Host " Gateway: $GatewayUrl"
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "--- 1. Gateway endpoints ---"
CheckStatus "Health check"         "$GatewayUrl/health"
CheckJson   "Health body"          "$GatewayUrl/health"             { param($j) $j.status }          "ok"
CheckStatus "SMART configuration"  "$GatewayUrl/.well-known/smart-configuration.json"
CheckJsonContains "SMART has token_endpoint" "$GatewayUrl/.well-known/smart-configuration.json" { param($j) $j.token_endpoint } "token"
CheckStatus "OpenID configuration" "$GatewayUrl/.well-known/openid-configuration.json"
CheckJsonContains "OpenID has issuer" "$GatewayUrl/.well-known/openid-configuration.json" { param($j) $j.issuer } "http"
CheckJson   "SMART has private_key_jwt auth method"      "$GatewayUrl/.well-known/smart-configuration.json" {
    param($j)
    $j.token_endpoint_auth_methods_supported -contains "private_key_jwt"
} "True"
CheckJson   "SMART has client-confidential-asymmetric"   "$GatewayUrl/.well-known/smart-configuration.json" {
    param($j)
    $j.capabilities -contains "client-confidential-asymmetric"
} "True"
CheckJson   "SMART scopes_supported populated"           "$GatewayUrl/.well-known/smart-configuration.json" {
    param($j)
    $j.scopes_supported.Count -gt 0
} "True"
CheckJsonContains "OpenID has userinfo_endpoint"         "$GatewayUrl/.well-known/openid-configuration.json" { param($j) $j.userinfo_endpoint } "userinfo"
CheckJson   "OpenID scopes_supported populated"          "$GatewayUrl/.well-known/openid-configuration.json" {
    param($j)
    $j.scopes_supported.Count -gt 0
} "True"
CheckJson   "OpenID claims_supported populated"          "$GatewayUrl/.well-known/openid-configuration.json" {
    param($j)
    $j.claims_supported.Count -gt 0
} "True"

Write-Host ""
Write-Host "--- 2. FHIR CapabilityStatement ---"
CheckStatus "GET /fhir/metadata"           "$GatewayUrl/fhir/metadata"
CheckJson   "metadata resourceType"        "$GatewayUrl/fhir/metadata"   { param($j) $j.resourceType }  "CapabilityStatement"
CheckJsonContains "metadata fhirVersion=4" "$GatewayUrl/fhir/metadata"   { param($j) $j.fhirVersion }   "4."
CheckJson   "metadata instantiates EU HDA RAP"              "$GatewayUrl/fhir/metadata" {
    param($j)
    $j.instantiates -contains "http://hl7.eu/fhir/health-data-api/CapabilityStatement/EEHRxF-ResourceAccessProvider"
} "True"
CheckJson   "metadata implementationGuide EU EPS"           "$GatewayUrl/fhir/metadata" {
    param($j)
    $j.implementationGuide -contains "http://hl7.eu/fhir/eps"
} "True"
CheckJson   "metadata security service SMART-on-FHIR"       "$GatewayUrl/fhir/metadata" {
    param($j)
    $j.rest[0].security.service[0].coding[0].code
} "SMART-on-FHIR"
CheckJson   "metadata security has oauth-uris extension"    "$GatewayUrl/fhir/metadata" {
    param($j)
    ($j.rest[0].security.extension | Where-Object { $_.url -like "*oauth-uris*" }) -ne $null
} "True"
CheckJson   "metadata Condition supportedProfile SE EHDS"   "$GatewayUrl/fhir/metadata" {
    param($j)
    ($j.rest[0].resource | Where-Object { $_.type -eq "Condition" }).supportedProfile -contains
        "https://ehds-brygga.inera.se/fhir/StructureDefinition/se-ehds-condition"
} "True"
CheckJson   "metadata Condition supportedProfile EU EPS"    "$GatewayUrl/fhir/metadata" {
    param($j)
    ($j.rest[0].resource | Where-Object { $_.type -eq "Condition" }).supportedProfile -contains
        "http://hl7.eu/fhir/eps/StructureDefinition/condition-obl-eu-eps"
} "True"
CheckJson   "metadata DocumentReference supportedProfile"   "$GatewayUrl/fhir/metadata" {
    param($j)
    ($j.rest[0].resource | Where-Object { $_.type -eq "DocumentReference" }).supportedProfile -contains
        "https://ehds-brygga.inera.se/fhir/StructureDefinition/se-ehds-document-reference"
} "True"
CheckJson   "metadata Condition has patient.identifier param" "$GatewayUrl/fhir/metadata" {
    param($j)
    @(($j.rest[0].resource | Where-Object { $_.type -eq "Condition" }).searchParam |
      Where-Object { $_.name -eq "patient.identifier" }).Count -ge 1
} "True"

Write-Host ""
Write-Host "--- 3. Condition - aggregated search (all VGs) ---"
$condUrl = "$GatewayUrl/fhir/Condition?patient.identifier=$PatientSystem$enc$PatientId"
CheckStatus "GET Condition all VGs"                 $condUrl
CheckJson   "Condition total = 6 (3 diagnoses x 2 VGs)" $condUrl { param($j) $j.total } "6"
CheckJson   "Condition resourceType = Bundle"       $condUrl { param($j) $j.resourceType } "Bundle"
CheckJson   "Condition type = searchset"            $condUrl { param($j) $j.type } "searchset"

CheckJson "Code J18.9 occurs 2 times" $condUrl {
    param($j)
    ($j.entry | Where-Object { $_.resource.resourceType -eq "Condition" -and $_.resource.code.coding[0].code -eq "J18.9" }).Count
} "2"

CheckJson "Code E11.9 occurs 2 times" $condUrl {
    param($j)
    ($j.entry | Where-Object { $_.resource.resourceType -eq "Condition" -and $_.resource.code.coding[0].code -eq "E11.9" }).Count
} "2"

CheckJson "Code I10 occurs 2 times" $condUrl {
    param($j)
    ($j.entry | Where-Object { $_.resource.resourceType -eq "Condition" -and $_.resource.code.coding[0].code -eq "I10" }).Count
} "2"

CheckJson "Diagnosis code system = ICD-10-SE" $condUrl {
    param($j)
    ($j.entry | Where-Object { $_.resource.resourceType -eq "Condition" } | Select-Object -First 1).resource.code.coding[0].system
} "https://www.icd10.se/"

CheckJson "Patient identifier = $PatientId" $condUrl {
    param($j)
    ($j.entry | Where-Object { $_.resource.resourceType -eq "Condition" } | Select-Object -First 1).resource.subject.identifier.value
} "$PatientId"

CheckJson "Provenance resources present" $condUrl {
    param($j)
    ($j.entry | Where-Object { $_.resource.resourceType -eq "Provenance" }).Count -ge 1
} "True"
CheckJson "Condition meta.profile has SE EHDS profile" $condUrl {
    param($j)
    $cond = ($j.entry | Where-Object { $_.resource.resourceType -eq "Condition" } | Select-Object -First 1).resource
    $cond.meta.profile -contains "https://ehds-brygga.inera.se/fhir/StructureDefinition/se-ehds-condition"
} "True"
CheckJson "Condition meta.profile has EU EPS profile"  $condUrl {
    param($j)
    $cond = ($j.entry | Where-Object { $_.resource.resourceType -eq "Condition" } | Select-Object -First 1).resource
    $cond.meta.profile -contains "http://hl7.eu/fhir/eps/StructureDefinition/condition-obl-eu-eps"
} "True"

Write-Host ""
Write-Host "--- 4. Condition - VG-scoped search ---"
$condVgrUrl = "$GatewayUrl/fhir/$VgrHsa/Condition?patient.identifier=$PatientSystem$enc$PatientId"
$condSllUrl = "$GatewayUrl/fhir/$SllHsa/Condition?patient.identifier=$PatientSystem$enc$PatientId"

CheckStatus "GET Condition VGR-scope"  $condVgrUrl
CheckJson   "Condition VGR total = 3"  $condVgrUrl { param($j) $j.total } "3"
CheckStatus "GET Condition SLL-scope"  $condSllUrl
CheckJson   "Condition SLL total = 3"  $condSllUrl { param($j) $j.total } "3"

Write-Host ""
Write-Host "--- 5. DocumentReference - aggregated search ---"
$docRefUrl    = "$GatewayUrl/fhir/DocumentReference?patient.identifier=$PatientSystem$enc$PatientId"
$docRefVgrUrl = "$GatewayUrl/fhir/$VgrHsa/DocumentReference?patient.identifier=$PatientSystem$enc$PatientId"

CheckStatus "GET DocumentReference all VGs"                 $docRefUrl
CheckJson   "DocumentReference total = 4 (2 docs x 2 VGs)" $docRefUrl { param($j) $j.total } "4"
CheckJson   "DocumentReference resourceType = Bundle"       $docRefUrl { param($j) $j.resourceType } "Bundle"
CheckStatus "GET DocumentReference VGR-scope"               $docRefVgrUrl
CheckJson   "DocumentReference VGR total = 2"               $docRefVgrUrl { param($j) $j.total } "2"

Write-Host ""
Write-Host "--- 6. Edge cases ---"
$unknownUrl = "$GatewayUrl/fhir/Condition?patient.identifier=${PatientSystem}${enc}000000000000"
CheckStatus "Unknown patient returns 200"  $unknownUrl
CheckJson   "Unknown patient total = 0"    $unknownUrl { param($j) $j.total } "0"

Write-Host ""
Write-Host "--- 7. New test patients ---"
$patient200 = "200001011234"
$patient198 = "198505152222"
$patient195 = "195810161234"
$cond200Url = "$GatewayUrl/fhir/Condition?patient.identifier=${PatientSystem}${enc}${patient200}"
$cond198Url = "$GatewayUrl/fhir/Condition?patient.identifier=${PatientSystem}${enc}${patient198}"
$cond195Url = "$GatewayUrl/fhir/Condition?patient.identifier=${PatientSystem}${enc}${patient195}"
$condVgr198 = "$GatewayUrl/fhir/$VgrHsa/Condition?patient.identifier=${PatientSystem}${enc}${patient198}"
$condVgr195 = "$GatewayUrl/fhir/$VgrHsa/Condition?patient.identifier=${PatientSystem}${enc}${patient195}"

# Mock returnerar ALLA diagnoser for patienten oavsett VG -> total = diagnoser x 2 VGs
CheckStatus "GET Condition $patient200 (minimalt)"              $cond200Url
CheckJson   "$patient200 total = 2 (1 diagnos x 2 VGs)"         $cond200Url { param($j) $j.total } "2"
CheckJson   "$patient200 kod = J06.9"                           $cond200Url {
    param($j)
    ($j.entry | Where-Object { $_.resource.resourceType -eq "Condition" } | Select-Object -First 1).resource.code.coding[0].code
} "J06.9"

CheckStatus "GET Condition $patient198 (maximum)"               $cond198Url
CheckJson   "$patient198 total = 10 (5 diagnoser x 2 VGs)"      $cond198Url { param($j) $j.total } "10"
CheckJson   "$patient198 VGR total = 5"                         $condVgr198 { param($j) $j.total } "5"
CheckJson   "$patient198 har assertedDate-extension"            $cond198Url {
    param($j)
    ($j.entry | Where-Object { $_.resource.resourceType -eq "Condition" } |
     ForEach-Object { $_.resource.extension } |
     Where-Object { $_.url -like "*ext-asserted-date*" }).Count -gt 0
} "True"
CheckJson   "$patient198 J44.1 clinicalStatus = resolved"       $cond198Url {
    param($j)
    ($j.entry | Where-Object {
        $_.resource.resourceType -eq "Condition" -and
        $_.resource.code.coding[0].code -eq "J44.1"
    } | Select-Object -First 1).resource.clinicalStatus.coding[0].code
} "resolved"

CheckStatus "GET Condition $patient195 (NPO-demo)"              $cond195Url
CheckJson   "$patient195 total = 12 (6 diagnoser x 2 VGs)"      $cond195Url { param($j) $j.total } "12"
CheckJson   "$patient195 VGR total = 6"                         $condVgr195 { param($j) $j.total } "6"

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Cyan
$total = $Pass + $Fail
if ($Fail -eq 0) {
    Write-Host " All $Pass tests passed." -ForegroundColor Green
    exit 0
} else {
    Write-Host " $Fail of $total tests FAILED." -ForegroundColor Red
    exit 1
}
