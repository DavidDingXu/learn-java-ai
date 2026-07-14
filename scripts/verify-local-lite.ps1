$ErrorActionPreference = "Stop"

& (Join-Path $PSScriptRoot "verify-unit.ps1")
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host "Local-lite verification passed without requiring Docker or external infrastructure."
