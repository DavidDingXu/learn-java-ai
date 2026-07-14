[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($env:JAVA_AI_EXTERNAL_BASE_URL)) {
    [Console]::Error.WriteLine(
        "ERROR: JAVA_AI_EXTERNAL_BASE_URL is required; refusing to claim external verification without an explicit environment.")
    exit 2
}

$BaseUri = $null
if (-not [Uri]::TryCreate($env:JAVA_AI_EXTERNAL_BASE_URL, [UriKind]::Absolute, [ref]$BaseUri) -or
    ($BaseUri.Scheme -ne "http" -and $BaseUri.Scheme -ne "https")) {
    [Console]::Error.WriteLine(
        "ERROR: JAVA_AI_EXTERNAL_BASE_URL must be an absolute http(s) URL: $env:JAVA_AI_EXTERNAL_BASE_URL")
    exit 2
}

$Builder = [UriBuilder]::new($BaseUri)
$Builder.Path = "$($BaseUri.AbsolutePath.TrimEnd('/'))/actuator/health"
$Builder.Query = ""
$Builder.Fragment = ""
$HealthUri = $Builder.Uri

try {
    $Response = Invoke-RestMethod -Method Get -Uri $HealthUri -TimeoutSec 15
}
catch {
    [Console]::Error.WriteLine("ERROR: External health request failed: $HealthUri`n$($_.Exception.Message)")
    exit 1
}

if ($null -eq $Response -or
    $null -eq $Response.PSObject.Properties["status"] -or
    $Response.status -ne "UP") {
    [Console]::Error.WriteLine("ERROR: External health response does not contain status=UP: $HealthUri")
    exit 1
}

Write-Host "External health smoke passed: $HealthUri"
Write-Host "Scope: one HTTP health endpoint only; no database, vector, object-storage, or end-to-end evidence was produced."
