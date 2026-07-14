$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$ReportPath = if ($env:JAVA_AI_AGENT_LIVE_REPORT_PATH) {
    $env:JAVA_AI_AGENT_LIVE_REPORT_PATH
} else {
    Join-Path $ProjectRoot "docs\reports\lesson-34-agent-live-model-smoke.md"
}

foreach ($Name in @("JAVA_AI_CHAT_API_KEY", "JAVA_AI_CHAT_BASE_URL", "JAVA_AI_CHAT_MODEL")) {
    if (-not (Get-Item "Env:$Name" -ErrorAction SilentlyContinue).Value) {
        throw "$Name is required for the agent live model smoke test."
    }
}

$JavaHome = if ($env:JAVA_AI_MAIN_JAVA_HOME) { $env:JAVA_AI_MAIN_JAVA_HOME } else { $env:JAVA_HOME }
if (-not $JavaHome -or -not (Test-Path (Join-Path $JavaHome "bin\java.exe")) -or
    -not (Test-Path (Join-Path $JavaHome "bin\javac.exe"))) {
    throw "JAVA_AI_MAIN_JAVA_HOME must point to a full JDK 21 or newer."
}

$Commit = try { (git -C $ProjectRoot rev-parse HEAD).Trim() } catch { "unknown" }
$Maven = Join-Path $ProjectRoot "mvnw.cmd"
& $Maven -f (Join-Path $ProjectRoot "pom.xml") `
    -pl services/ticket-agent-service `
    -Dtest=TicketAgentLiveModelSmokeIT `
    "-Djava-ai.agent-smoke.report-path=$ReportPath" `
    "-Djava-ai.agent-smoke.commit=$Commit" `
    test
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Agent LIVE_MODEL report written to $ReportPath"
