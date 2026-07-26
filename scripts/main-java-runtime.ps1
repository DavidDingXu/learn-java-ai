function Get-JavaAiJavacMajor {
    param([Parameter(Mandatory = $true)][string]$JavaHome)

    $Javac = Join-Path $JavaHome "bin\javac.exe"
    try {
        $JavacOutput = (& $Javac -version 2>&1 | Out-String).Trim()
    } catch {
        return $null
    }
    if ($LASTEXITCODE -ne 0 -or $JavacOutput -notmatch '^javac\s+(.+)$') {
        return $null
    }

    $Version = $Matches[1]
    if ($Version.StartsWith("1.")) {
        return [int](($Version.Substring(2) -split '[._]')[0])
    }
    return [int](($Version -split '[._]')[0])
}

function Test-JavaAiMainJdk {
    param([Parameter(Mandatory = $true)][string]$JavaHome)

    if (-not (Test-Path (Join-Path $JavaHome "bin\java.exe")) -or
        -not (Test-Path (Join-Path $JavaHome "bin\javac.exe"))) {
        return $false
    }
    $Major = Get-JavaAiJavacMajor -JavaHome $JavaHome
    return $null -ne $Major -and $Major -ge 21
}

function Get-JavaAiMainJdkCandidates {
    $Candidates = [System.Collections.Generic.List[string]]::new()

    if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
        [void]$Candidates.Add($env:JAVA_HOME)
    }

    $JavacCommand = Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($null -ne $JavacCommand) {
        [void]$Candidates.Add((Split-Path -Parent (Split-Path -Parent $JavacCommand.Source)))
    }

    $Roots = [System.Collections.Generic.List[string]]::new()
    if (-not [string]::IsNullOrWhiteSpace($env:ProgramFiles)) {
        foreach ($Directory in @("Java", "Eclipse Adoptium", "Microsoft", "Amazon Corretto")) {
            [void]$Roots.Add((Join-Path $env:ProgramFiles $Directory))
        }
    }
    if (-not [string]::IsNullOrWhiteSpace($env:USERPROFILE)) {
        [void]$Roots.Add((Join-Path $env:USERPROFILE ".jdks"))
    }

    foreach ($Root in $Roots) {
        if (Test-Path $Root) {
            Get-ChildItem -Path $Root -Directory | ForEach-Object {
                [void]$Candidates.Add($_.FullName)
            }
        }
    }

    return $Candidates | Select-Object -Unique
}

function Enter-JavaAiMainJdk {
    $JavaHome = $null
    if (-not [string]::IsNullOrWhiteSpace($env:JAVA_AI_MAIN_JAVA_HOME)) {
        if (-not (Test-JavaAiMainJdk -JavaHome $env:JAVA_AI_MAIN_JAVA_HOME)) {
            throw "JAVA_AI_MAIN_JAVA_HOME is not a full JDK 21 or newer: $($env:JAVA_AI_MAIN_JAVA_HOME)"
        }
        $JavaHome = $env:JAVA_AI_MAIN_JAVA_HOME
    } else {
        foreach ($Candidate in Get-JavaAiMainJdkCandidates) {
            if (Test-JavaAiMainJdk -JavaHome $Candidate) {
                $JavaHome = $Candidate
                break
            }
        }
    }
    if ([string]::IsNullOrWhiteSpace($JavaHome)) {
        throw "No full JDK 21 or newer was found. Install a JDK and rerun the command."
    }

    $Java = Join-Path $JavaHome "bin\java.exe"
    $Major = Get-JavaAiJavacMajor -JavaHome $JavaHome
    if ($Major -lt 21) {
        throw "The selected JDK must be version 21 or newer."
    }

    $PreviousJavaHome = $env:JAVA_HOME
    $PreviousPath = $env:Path
    $env:JAVA_HOME = $JavaHome
    $env:Path = "$(Join-Path $JavaHome 'bin')$([IO.Path]::PathSeparator)$PreviousPath"

    [pscustomobject]@{
        Java = $Java
        JavaHome = $JavaHome
        PreviousJavaHome = $PreviousJavaHome
        PreviousPath = $PreviousPath
    }
}

function Restore-JavaAiEnvironment {
    param([Parameter(Mandatory = $true)]$Runtime)

    if ($null -eq $Runtime.PreviousJavaHome) {
        Remove-Item Env:JAVA_HOME -ErrorAction SilentlyContinue
    } else {
        $env:JAVA_HOME = $Runtime.PreviousJavaHome
    }
    $env:Path = $Runtime.PreviousPath
}
