$ErrorActionPreference = "Stop"

$RootDir = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
. (Join-Path $PSScriptRoot "main-java-runtime.ps1")
$ReportPrefix = if ($args.Count -gt 0) { $args[0] } else { Join-Path $RootDir "docs/reports/lesson-12-live-model-eval" }
$Port = if ($env:JAVA_AI_EVAL_PORT) { $env:JAVA_AI_EVAL_PORT } else { "18081" }
$ConfigFile = Join-Path $RootDir "config\application.yml"

if (-not (Test-Path $ConfigFile)) {
  throw "Missing $ConfigFile. Restore the tracked shared model configuration."
}

$Commit = if ($env:JAVA_AI_EVAL_COMMIT) { $env:JAVA_AI_EVAL_COMMIT } else { git -C $RootDir rev-parse HEAD }
$ServiceJar = Join-Path $RootDir "services/knowledge-service/target/knowledge-service-0.1.0-SNAPSHOT.jar"
$ServiceArgs = @(
  "-Dspring.config.additional-location=file:$ConfigFile",
  "-jar", $ServiceJar,
  "--java-ai.knowledge.mode=classpath",
  "--spring.ai.model.embedding=none",
  "--spring.flyway.enabled=false",
  "--spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
  "--java-ai.security.mode=fixed",
  "--server.address=127.0.0.1",
  "--server.port=$Port"
)
$JavaRuntime = $null
try {
  $JavaRuntime = Enter-JavaAiMainJdk
  & (Join-Path $RootDir "mvnw.cmd") -f (Join-Path $RootDir "pom.xml") `
    -pl services/knowledge-service,quality/eval-runner -am package -DskipTests
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

  $Process = Start-Process -FilePath $JavaRuntime.Java -PassThru -NoNewWindow -ArgumentList $ServiceArgs

  try {
    $Healthy = $false
    for ($Attempt = 0; $Attempt -lt 60; $Attempt++) {
      try {
        Invoke-RestMethod -Uri "http://127.0.0.1:$Port/actuator/health" | Out-Null
        $Healthy = $true
        break
      } catch {
        Start-Sleep -Seconds 1
      }
    }
    if (-not $Healthy) { throw "Knowledge Service did not become healthy." }

    & $JavaRuntime.Java -jar (Join-Path $RootDir "quality/eval-runner/target/eval-runner-0.1.0-SNAPSHOT-all.jar") `
      model-eval `
      --dataset (Join-Path $RootDir "datasets/model-interaction/golden-set-v2.jsonl") `
      --base-url "http://127.0.0.1:$Port" `
      --mode LIVE_MODEL `
      --prompt-version knowledge-answer-v1 `
      --environment-id local-live-model `
      --report $ReportPrefix `
      --commit $Commit
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
  } finally {
    Stop-Process -Id $Process.Id -ErrorAction SilentlyContinue
  }
} finally {
  if ($JavaRuntime) { Restore-JavaAiEnvironment $JavaRuntime }
}
