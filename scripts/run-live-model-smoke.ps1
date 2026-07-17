[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = (Resolve-Path (Join-Path $ScriptDir "..")).Path
. (Join-Path $PSScriptRoot "main-java-runtime.ps1")
$MavenWrapper = Join-Path $ProjectRoot "mvnw.cmd"
$ReportPath = if ([string]::IsNullOrWhiteSpace($env:JAVA_AI_LIVE_REPORT_PATH)) {
    Join-Path $ProjectRoot "docs\reports\lesson-04-live-model-smoke.md"
} else {
    $env:JAVA_AI_LIVE_REPORT_PATH
}

function Stop-WithError {
    param([Parameter(Mandatory = $true)][string]$Message)
    [Console]::Error.WriteLine("ERROR: $Message")
    exit 2
}

foreach ($Name in @("JAVA_AI_CHAT_API_KEY", "JAVA_AI_CHAT_BASE_URL", "JAVA_AI_CHAT_MODEL")) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($Name))) {
        Stop-WithError "$Name is required for the live model smoke test."
    }
}

$Commit = (& git -C $ProjectRoot rev-parse HEAD 2>$null | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($Commit)) {
    $Commit = "unknown"
}

$JavaRuntime = Enter-JavaAiMainJdk
try {
    & $MavenWrapper `
        -f (Join-Path $ProjectRoot "pom.xml") `
        -pl services/knowledge-service `
        -Dtest=LiveModelSmokeIT `
        "-Djava-ai.smoke.report-path=$ReportPath" `
        "-Djava-ai.smoke.commit=$Commit" `
        test
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}
finally {
    Restore-JavaAiEnvironment $JavaRuntime
}

Write-Host "LIVE_MODEL report written to $ReportPath"
