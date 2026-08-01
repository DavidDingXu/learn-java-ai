$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
. (Join-Path $PSScriptRoot "main-java-runtime.ps1")
$ConfigFile = Join-Path $ProjectRoot "config\application.yml"
$ReportPath = if ($env:JAVA_AI_AGENT_LIVE_REPORT_PATH) {
    $env:JAVA_AI_AGENT_LIVE_REPORT_PATH
} else {
    Join-Path $ProjectRoot "docs\reports\lesson-34-agent-live-model-smoke.md"
}

if (-not (Test-Path $ConfigFile)) {
    throw "Missing $ConfigFile. Restore the tracked demo configuration."
}

$Commit = try { (git -C $ProjectRoot rev-parse HEAD).Trim() } catch { "unknown" }
$Maven = Join-Path $ProjectRoot "mvnw.cmd"
$JavaRuntime = Enter-JavaAiMainJdk
try {
    & $Maven -f (Join-Path $ProjectRoot "pom.xml") `
        -pl services/ticket-agent-service `
        -Dtest=TicketAgentLiveModelSmokeIT `
        "-Dspring.config.additional-location=file:$ConfigFile" `
        "-Djava-ai.agent-smoke.report-path=$ReportPath" `
        "-Djava-ai.agent-smoke.commit=$Commit" `
        test
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally {
    Restore-JavaAiEnvironment $JavaRuntime
}

Write-Host "Agent LIVE_MODEL report written to $ReportPath"
