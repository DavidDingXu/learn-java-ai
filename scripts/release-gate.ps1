$ErrorActionPreference = "Stop"

$RootDir = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$RequireExternal = if ($env:JAVA_AI_RELEASE_REQUIRE_EXTERNAL) { $env:JAVA_AI_RELEASE_REQUIRE_EXTERNAL } else { "0" }

& (Join-Path $RootDir "scripts\verify-unit.ps1")
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$SecretPattern = 'sk-[A-Za-z0-9_-]{20,}|memberai\.tech'
$SecretHits = @()
git -C $RootDir ls-files -co --exclude-standard | ForEach-Object {
  if ($_ -notin @("scripts/release-gate.sh", "scripts/release-gate.ps1")) {
    $Path = Join-Path $RootDir $_
    if (Test-Path $Path -PathType Leaf) {
      $SecretHits += Select-String -Path $Path -Pattern $SecretPattern -AllMatches -ErrorAction SilentlyContinue
    }
  }
}
if ($SecretHits.Count -gt 0) {
  $SecretHits | ForEach-Object { [Console]::Error.WriteLine($_.ToString()) }
  throw "Possible secret or private provider endpoint found."
}

if ($RequireExternal -eq "1") {
  if ([string]::IsNullOrWhiteSpace($env:JAVA_AI_EXTERNAL_BASE_URL)) {
    throw "JAVA_AI_EXTERNAL_BASE_URL is required when JAVA_AI_RELEASE_REQUIRE_EXTERNAL=1."
  }
  & (Join-Path $RootDir "scripts\verify-integration.ps1")
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}
elseif ($RequireExternal -ne "0") {
  throw "JAVA_AI_RELEASE_REQUIRE_EXTERNAL must be 0 or 1."
}

Write-Host "Release gate passed. External evidence required: $RequireExternal."
