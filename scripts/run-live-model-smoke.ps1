[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = (Resolve-Path (Join-Path $ScriptDir "..")).Path
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

$MainJavaHome = if (-not [string]::IsNullOrWhiteSpace($env:JAVA_AI_MAIN_JAVA_HOME)) {
    $env:JAVA_AI_MAIN_JAVA_HOME
} else {
    $env:JAVA_HOME
}
if ([string]::IsNullOrWhiteSpace($MainJavaHome)) {
    Stop-WithError "Set JAVA_AI_MAIN_JAVA_HOME to a full JDK 21 or newer."
}

$Java = Join-Path $MainJavaHome "bin\java.exe"
$Javac = Join-Path $MainJavaHome "bin\javac.exe"
if (-not (Test-Path $Java) -or -not (Test-Path $Javac)) {
    Stop-WithError "JAVA_AI_MAIN_JAVA_HOME must contain bin\java.exe and bin\javac.exe: $MainJavaHome"
}

$JavacOutput = (& $Javac -version 2>&1 | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or $JavacOutput -notmatch '^javac\s+(.+)$') {
    Stop-WithError "Unable to parse javac version: $JavacOutput"
}
$Version = $Matches[1]
$Major = if ($Version.StartsWith("1.")) {
    [int](($Version.Substring(2) -split '[._]')[0])
} else {
    [int](($Version -split '[._]')[0])
}
if ($Major -lt 21) {
    Stop-WithError "The live model smoke test requires JDK 21 or newer."
}

$Commit = (& git -C $ProjectRoot rev-parse HEAD 2>$null | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($Commit)) {
    $Commit = "unknown"
}

$PreviousJavaHome = $env:JAVA_HOME
$PreviousPath = $env:Path
try {
    $env:JAVA_HOME = $MainJavaHome
    $env:Path = "$(Join-Path $MainJavaHome 'bin')$([IO.Path]::PathSeparator)$PreviousPath"
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
    if ($null -eq $PreviousJavaHome) {
        Remove-Item Env:JAVA_HOME -ErrorAction SilentlyContinue
    } else {
        $env:JAVA_HOME = $PreviousJavaHome
    }
    $env:Path = $PreviousPath
}

Write-Host "LIVE_MODEL report written to $ReportPath"
