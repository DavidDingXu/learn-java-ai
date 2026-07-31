$ErrorActionPreference = "Stop"

$RootDir = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
. (Join-Path $PSScriptRoot "main-java-runtime.ps1")
$ReportPrefix = if ($args.Count -gt 0) { $args[0] } else { Join-Path $RootDir "docs/reports/lesson-12-live-model-eval" }
$Port = if ($env:JAVA_AI_EVAL_PORT) { $env:JAVA_AI_EVAL_PORT } else { "18081" }
$ConfigFile = Join-Path $RootDir "config\application.yml"
$ExampleConfigFile = Join-Path $RootDir "config\application.example.yml"

foreach ($Name in @("JAVA_AI_EVAL_BEARER_TOKEN", "JAVA_AI_JWT_ISSUER")) {
  if (-not (Get-Item "Env:$Name" -ErrorAction SilentlyContinue).Value) {
    throw "$Name is required."
  }
}
if (-not (Test-Path $ConfigFile)) {
  Copy-Item -Path $ExampleConfigFile -Destination $ConfigFile
  throw "Created $ConfigFile. Replace spring.ai.openai.api-key, then run this command again."
}

if (-not $env:JAVA_AI_DEV_JWT_HMAC_SECRET -and -not $env:JAVA_AI_JWT_JWK_SET_URI) {
  throw "JAVA_AI_DEV_JWT_HMAC_SECRET or JAVA_AI_JWT_JWK_SET_URI is required."
}

$Commit = if ($env:JAVA_AI_EVAL_COMMIT) { $env:JAVA_AI_EVAL_COMMIT } else { git -C $RootDir rev-parse HEAD }
$SecurityArgs = @(
  "--java-ai.security.jwt.enabled=true",
  "--java-ai.security.jwt.issuer=$($env:JAVA_AI_JWT_ISSUER)",
  "--java-ai.security.jwt.audience=$(if ($env:JAVA_AI_JWT_AUDIENCE) { $env:JAVA_AI_JWT_AUDIENCE } else { 'knowledge-service' })",
  "--java-ai.security.jwt.allowed-actors=$(if ($env:JAVA_AI_JWT_ALLOWED_ACTORS) { $env:JAVA_AI_JWT_ALLOWED_ACTORS } else { 'customer-bff' })"
)
if ($env:JAVA_AI_DEV_JWT_HMAC_SECRET) {
  $SecurityArgs += "--java-ai.security.jwt.hmac-secret=$($env:JAVA_AI_DEV_JWT_HMAC_SECRET)"
} else {
  $SecurityArgs += "--java-ai.security.jwt.jwk-set-uri=$($env:JAVA_AI_JWT_JWK_SET_URI)"
}
$ServiceJar = Join-Path $RootDir "services/knowledge-service/target/knowledge-service-0.1.0-SNAPSHOT.jar"
$ServiceArgs = @(
  "-Dspring.config.additional-location=file:$ConfigFile",
  "-jar", $ServiceJar,
  "--java-ai.knowledge.context-source=classpath",
  "--java-ai.knowledge.ingestion.enabled=false",
  "--spring.ai.model.embedding=none",
  "--spring.flyway.enabled=false",
  "--spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
  "--server.address=127.0.0.1",
  "--server.port=$Port"
)
$ServiceArgs += $SecurityArgs
$CredentialDir = $null
$BearerTokenFile = $null
$JavaRuntime = $null
try {
  $JavaRuntime = Enter-JavaAiMainJdk
  & (Join-Path $RootDir "mvnw.cmd") -f (Join-Path $RootDir "pom.xml") `
    -pl services/knowledge-service,quality/eval-runner -am package -DskipTests
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

  $CredentialDir = Join-Path ([IO.Path]::GetTempPath()) ("java-ai-model-eval-" + [Guid]::NewGuid())
  $BearerTokenFile = Join-Path $CredentialDir "bearer-token"
  New-Item -ItemType Directory -Path $CredentialDir | Out-Null
  $Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
  [IO.File]::WriteAllText($BearerTokenFile, $env:JAVA_AI_EVAL_BEARER_TOKEN, $Utf8NoBom)

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
      --bearer-token-file $BearerTokenFile `
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
  if ($CredentialDir) { Remove-Item -LiteralPath $CredentialDir -Recurse -Force -ErrorAction SilentlyContinue }
}
