function Enter-JavaAiMainJdk {
    $JavaHome = if (-not [string]::IsNullOrWhiteSpace($env:JAVA_AI_MAIN_JAVA_HOME)) {
        $env:JAVA_AI_MAIN_JAVA_HOME
    } else {
        $env:JAVA_HOME
    }
    if ([string]::IsNullOrWhiteSpace($JavaHome)) {
        throw "Set JAVA_AI_MAIN_JAVA_HOME to a full JDK 21 or newer."
    }

    $Java = Join-Path $JavaHome "bin\java.exe"
    $Javac = Join-Path $JavaHome "bin\javac.exe"
    if (-not (Test-Path $Java) -or -not (Test-Path $Javac)) {
        throw "JAVA_AI_MAIN_JAVA_HOME must contain bin\java.exe and bin\javac.exe: $JavaHome"
    }

    $JavacOutput = (& $Javac -version 2>&1 | Out-String).Trim()
    if ($LASTEXITCODE -ne 0 -or $JavacOutput -notmatch '^javac\s+(.+)$') {
        throw "Unable to parse javac version: $JavacOutput"
    }
    $Version = $Matches[1]
    $Major = if ($Version.StartsWith("1.")) {
        [int](($Version.Substring(2) -split '[._]')[0])
    } else {
        [int](($Version -split '[._]')[0])
    }
    if ($Major -lt 21) {
        throw "JAVA_AI_MAIN_JAVA_HOME must point to JDK 21 or newer."
    }

    $PreviousJavaHome = $env:JAVA_HOME
    $PreviousPath = $env:Path
    $env:JAVA_HOME = $JavaHome
    $env:Path = "$(Join-Path $JavaHome 'bin')$([IO.Path]::PathSeparator)$PreviousPath"

    [pscustomobject]@{
        Java = $Java
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
