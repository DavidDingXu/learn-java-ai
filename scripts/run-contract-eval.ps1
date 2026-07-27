$ErrorActionPreference = "Stop"

$RootDir = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
. (Join-Path $PSScriptRoot "main-java-runtime.ps1")
$ReportPrefix = if ($args.Count -gt 0) { $args[0] } else { Join-Path $RootDir "docs/reports/lesson-12-contract-eval" }
$Commit = if ($env:JAVA_AI_EVAL_COMMIT) { $env:JAVA_AI_EVAL_COMMIT } else { git -C $RootDir rev-parse HEAD }

$JavaRuntime = Enter-JavaAiMainJdk
try {
  & (Join-Path $RootDir "mvnw.cmd") -f (Join-Path $RootDir "pom.xml") -pl quality/eval-runner -am package -DskipTests
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

  & $JavaRuntime.Java -jar (Join-Path $RootDir "quality/eval-runner/target/eval-runner-0.1.0-SNAPSHOT-all.jar") `
    contract-eval `
    --dataset (Join-Path $RootDir "datasets/model-interaction/golden-set-v2.jsonl") `
    --prompt-version knowledge-answer-v1 `
    --environment-id local-contract-fixture `
    --report $ReportPrefix `
    --commit $Commit
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally {
  Restore-JavaAiEnvironment $JavaRuntime
}
