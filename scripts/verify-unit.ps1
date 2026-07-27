[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = (Resolve-Path (Join-Path $ScriptDir "..")).Path
$MavenWrapper = Join-Path $ProjectRoot "mvnw.cmd"
$CustomerWeb = Join-Path $ProjectRoot "apps\customer-web"

function Stop-WithError {
    param(
        [Parameter(Mandatory = $true)][string]$Message,
        [int]$ExitCode = 2
    )

    [Console]::Error.WriteLine("ERROR: $Message")
    exit $ExitCode
}

function Get-JavacMajor {
    param([Parameter(Mandatory = $true)][string]$JavaHome)

    $Javac = Join-Path $JavaHome "bin\javac.exe"
    try {
        $Output = (& $Javac -version 2>&1 | Out-String).Trim()
    }
    catch {
        return $null
    }
    if ($LASTEXITCODE -ne 0 -or $Output -notmatch '^javac\s+(.+)$') {
        return $null
    }

    $Version = $Matches[1]
    if ($Version.StartsWith("1.")) {
        return [int](($Version.Substring(2) -split '[._]')[0])
    }
    return [int](($Version -split '[._]')[0])
}

function Test-Jdk {
    param(
        [Parameter(Mandatory = $true)][string]$JavaHome,
        [Parameter(Mandatory = $true)][ValidateSet("Main", "Java8")][string]$Kind
    )

    if (-not (Test-Path (Join-Path $JavaHome "bin\java.exe")) -or
        -not (Test-Path (Join-Path $JavaHome "bin\javac.exe"))) {
        return $false
    }

    $Major = Get-JavacMajor -JavaHome $JavaHome
    if ($null -eq $Major) {
        return $false
    }
    if ($Kind -eq "Main") {
        return $Major -ge 21
    }
    return $Major -eq 8
}

function Get-AutoJdkCandidates {
    $Candidates = [System.Collections.Generic.List[string]]::new()

    if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
        [void]$Candidates.Add($env:JAVA_HOME)
    }

    $JavacCommand = Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($null -ne $JavacCommand) {
        [void]$Candidates.Add((Split-Path -Parent (Split-Path -Parent $JavacCommand.Source)))
    }

    $Roots = @(
        (Join-Path $env:ProgramFiles "Java"),
        (Join-Path $env:ProgramFiles "Eclipse Adoptium"),
        (Join-Path $env:ProgramFiles "Microsoft"),
        (Join-Path $env:ProgramFiles "Amazon Corretto"),
        (Join-Path $env:USERPROFILE ".jdks")
    )

    foreach ($Root in $Roots) {
        if (Test-Path $Root) {
            Get-ChildItem -Path $Root -Directory | ForEach-Object {
                [void]$Candidates.Add($_.FullName)
            }
        }
    }

    return $Candidates | Select-Object -Unique
}

function Select-Jdk {
    param(
        [Parameter(Mandatory = $true)][ValidateSet("Main", "Java8")][string]$Kind,
        [Parameter(Mandatory = $true)][string]$OverrideVariable
    )

    $Override = [Environment]::GetEnvironmentVariable($OverrideVariable)
    if (-not [string]::IsNullOrWhiteSpace($Override)) {
        if (Test-Jdk -JavaHome $Override -Kind $Kind) {
            return $Override
        }
    }

    foreach ($Candidate in Get-AutoJdkCandidates) {
        if (Test-Jdk -JavaHome $Candidate -Kind $Kind) {
            return $Candidate
        }
    }

    if ($Kind -eq "Main") {
        Stop-WithError "No full JDK 21 or newer was found. Install a JDK containing bin\java.exe and bin\javac.exe, then rerun the command."
    }
    Stop-WithError "No full JDK 8 was found. Install a JDK 8 containing bin\java.exe and bin\javac.exe, then rerun the command."
}

function Invoke-CheckedNative {
    param(
        [Parameter(Mandatory = $true)][string]$Command,
        [Parameter(Mandatory = $true)][string[]]$Arguments,
        [Parameter(Mandatory = $true)][string]$Description
    )

    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) {
        Stop-WithError "$Description failed with exit code $LASTEXITCODE." $LASTEXITCODE
    }
}

function Invoke-MavenWithJdk {
    param(
        [Parameter(Mandatory = $true)][string]$JavaHome,
        [Parameter(Mandatory = $true)][string[]]$Arguments,
        [Parameter(Mandatory = $true)][string]$Description
    )

    $PreviousJavaHome = $env:JAVA_HOME
    $PreviousPath = $env:Path
    try {
        $env:JAVA_HOME = $JavaHome
        $env:Path = "$(Join-Path $JavaHome 'bin')$([IO.Path]::PathSeparator)$PreviousPath"
        Invoke-CheckedNative -Command $MavenWrapper -Arguments $Arguments -Description $Description
    }
    finally {
        if ($null -eq $PreviousJavaHome) {
            Remove-Item Env:JAVA_HOME -ErrorAction SilentlyContinue
        }
        else {
            $env:JAVA_HOME = $PreviousJavaHome
        }
        $env:Path = $PreviousPath
    }
}

if (-not (Test-Path $MavenWrapper)) {
    Stop-WithError "Maven wrapper is missing: $MavenWrapper"
}
if ($null -eq (Get-Command node.exe -ErrorAction SilentlyContinue)) {
    Stop-WithError "Node.js is required for repository contract tests."
}
if ($null -eq (Get-Command npm.cmd -ErrorAction SilentlyContinue)) {
    Stop-WithError "npm is required to verify Customer Web."
}

$NodeMajorOutput = (& node.exe -p 'process.versions.node.split(".")[0]' 2>&1 | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or $NodeMajorOutput -notmatch '^\d+$') {
    Stop-WithError "Unable to determine the Node.js major version."
}
$NodeMajor = [int]$NodeMajorOutput
if ($NodeMajor -lt 24) {
    Stop-WithError "Node.js 24 or newer is required; found major $NodeMajor."
}
if (-not (Test-Path (Join-Path $CustomerWeb "package.json"))) {
    Stop-WithError "Customer Web package.json is missing."
}
if (-not (Test-Path (Join-Path $CustomerWeb "package-lock.json"))) {
    Stop-WithError "Customer Web package-lock.json is missing."
}

$MainJavaHome = Select-Jdk -Kind Main -OverrideVariable "JAVA_AI_MAIN_JAVA_HOME"
$Jdk8Home = Select-Jdk -Kind Java8 -OverrideVariable "JAVA_AI_JDK8_HOME"
$MainJavaMajor = Get-JavacMajor -JavaHome $MainJavaHome

Write-Host "Main reactor JDK: $MainJavaHome (javac major $MainJavaMajor)"
if ($MainJavaMajor -gt 21) {
    Write-Host "NOTE: this proves --release 21 compilation on JDK $MainJavaMajor, not execution on a JDK 21 JVM."
}
Write-Host "Java 8 client JDK: $Jdk8Home (javac major 8)"

$ProjectTests = @(Get-ChildItem -Path (Join-Path $ProjectRoot "scripts") -Filter "*.test.mjs" -File)
if ($ProjectTests.Count -eq 0) {
    Stop-WithError "No project contract tests were found under $(Join-Path $ProjectRoot 'scripts')."
}
$NodeTests = @($ProjectTests.FullName)

Invoke-CheckedNative -Command "node.exe" -Arguments (@("--test") + $NodeTests) -Description "Node contract tests"
Invoke-CheckedNative -Command "npm.cmd" -Arguments @("--prefix", $CustomerWeb, "ci", "--no-audit", "--no-fund") -Description "Customer Web dependency installation"
Invoke-CheckedNative -Command "npm.cmd" -Arguments @("--prefix", $CustomerWeb, "run", "typecheck") -Description "Customer Web typecheck"
Invoke-CheckedNative -Command "npm.cmd" -Arguments @("--prefix", $CustomerWeb, "test") -Description "Customer Web tests"
Invoke-CheckedNative -Command "npm.cmd" -Arguments @("--prefix", $CustomerWeb, "run", "build") -Description "Customer Web production build"
Invoke-MavenWithJdk -JavaHome $MainJavaHome -Arguments @("-f", (Join-Path $ProjectRoot "pom.xml"), "verify") -Description "root reactor"
Invoke-MavenWithJdk -JavaHome $MainJavaHome -Arguments @("-f", (Join-Path $ProjectRoot "labs\pom.xml"), "verify") -Description "labs reactor"
Invoke-MavenWithJdk -JavaHome $Jdk8Home -Arguments @("-f", (Join-Path $ProjectRoot "integrations\jdk8-client\pom.xml"), "verify") -Description "Java 8 client"

Write-Host "Project verification passed for Customer Web, root, labs, Java 8 client, and project contracts."
