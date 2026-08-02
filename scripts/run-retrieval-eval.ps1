$ErrorActionPreference = "Stop"

$RootDir = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
. (Join-Path $PSScriptRoot "main-java-runtime.ps1")
$ReportPrefix = if ($args.Count -gt 0) { $args[0] } else { Join-Path $RootDir "docs/reports/lesson-21-retrieval-eval" }

$BaseUrl = if ($env:JAVA_AI_RETRIEVAL_BASE_URL) { $env:JAVA_AI_RETRIEVAL_BASE_URL } else { "http://127.0.0.1:8081" }
$Commit = if ($env:JAVA_AI_EVAL_COMMIT) { $env:JAVA_AI_EVAL_COMMIT } else { git -C $RootDir rev-parse HEAD }
$CredentialDir = $null
$BearerTokenFile = $null
$JavaRuntime = $null
try {
  $JavaRuntime = Enter-JavaAiMainJdk
  & (Join-Path $RootDir "mvnw.cmd") -f (Join-Path $RootDir "pom.xml") `
    -pl quality/eval-runner -am package -DskipTests
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

  $EvalArgs = @(
    "-jar", (Join-Path $RootDir "quality/eval-runner/target/eval-runner-0.1.0-SNAPSHOT-all.jar"),
    "retrieval-eval",
    "--dataset", (Join-Path $RootDir "datasets/retrieval/golden-set-v1.jsonl"),
    "--base-url", $BaseUrl,
    "--top-k", $(if ($env:JAVA_AI_RETRIEVAL_EVAL_TOP_K) { $env:JAVA_AI_RETRIEVAL_EVAL_TOP_K } else { "5" }),
    "--min-recall", $(if ($env:JAVA_AI_RETRIEVAL_MIN_RECALL) { $env:JAVA_AI_RETRIEVAL_MIN_RECALL } else { "0.80" }),
    "--min-hit-rate", $(if ($env:JAVA_AI_RETRIEVAL_MIN_HIT_RATE) { $env:JAVA_AI_RETRIEVAL_MIN_HIT_RATE } else { "0.90" }),
    "--min-mrr", $(if ($env:JAVA_AI_RETRIEVAL_MIN_MRR) { $env:JAVA_AI_RETRIEVAL_MIN_MRR } else { "0.60" }),
    "--max-duplicate-rate", $(if ($env:JAVA_AI_RETRIEVAL_MAX_DUPLICATE_RATE) { $env:JAVA_AI_RETRIEVAL_MAX_DUPLICATE_RATE } else { "0.02" }),
    "--max-p95-ms", $(if ($env:JAVA_AI_RETRIEVAL_MAX_P95_MS) { $env:JAVA_AI_RETRIEVAL_MAX_P95_MS } else { "1500" }),
    "--report", $ReportPrefix,
    "--commit", $Commit
  )
  if ($env:JAVA_AI_RETRIEVAL_EVAL_BEARER_TOKEN) {
    $CredentialDir = Join-Path ([IO.Path]::GetTempPath()) ("java-ai-retrieval-eval-" + [Guid]::NewGuid())
    $BearerTokenFile = Join-Path $CredentialDir "bearer-token"
    New-Item -ItemType Directory -Path $CredentialDir | Out-Null
    $Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [IO.File]::WriteAllText($BearerTokenFile, $env:JAVA_AI_RETRIEVAL_EVAL_BEARER_TOKEN, $Utf8NoBom)
    $EvalArgs += @("--bearer-token-file", $BearerTokenFile)
  }

  & $JavaRuntime.Java @EvalArgs
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally {
  if ($JavaRuntime) { Restore-JavaAiEnvironment $JavaRuntime }
  if ($CredentialDir) { Remove-Item -LiteralPath $CredentialDir -Recurse -Force -ErrorAction SilentlyContinue }
}
