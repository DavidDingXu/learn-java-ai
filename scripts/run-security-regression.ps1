$ErrorActionPreference = "Stop"

$RootDir = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
. (Join-Path $PSScriptRoot "main-java-runtime.ps1")
$ReportPrefix = if ($args.Count -gt 0) { $args[0] } else { Join-Path $RootDir "docs/reports/lesson-35-security-eval" }

foreach ($Name in @(
  "JAVA_AI_AGENT_BASE_URL",
  "JAVA_AI_AGENT_CREATE_TOKEN",
  "JAVA_AI_AGENT_RUN_TOKEN",
  "JAVA_AI_AGENT_READ_TOKEN"
)) {
  $Item = Get-Item "Env:$Name" -ErrorAction SilentlyContinue
  if (-not $Item -or -not $Item.Value) { throw "$Name is required." }
}

$Commit = if ($env:JAVA_AI_EVAL_COMMIT) { $env:JAVA_AI_EVAL_COMMIT } else { git -C $RootDir rev-parse HEAD }
$JavaRuntime = Enter-JavaAiMainJdk
try {
  & (Join-Path $RootDir "mvnw.cmd") -f (Join-Path $RootDir "pom.xml") `
    -pl services/knowledge-service,services/ticket-agent-service,quality/eval-runner `
    -Dtest=KnowledgeJwtSecurityTest,TicketAgentJwtSecurityTest,BusinessToolCatalogTest,SpringAiTicketAgentPlannerPromptTest,AgentEvaluatorTest `
    -Dsurefire.failIfNoSpecifiedTests=false test package
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

  & $JavaRuntime.Java -jar (Join-Path $RootDir "quality/eval-runner/target/eval-runner-0.1.0-SNAPSHOT-all.jar") `
    security-eval `
    --dataset (Join-Path $RootDir "datasets/security/agent-security-v1.jsonl") `
    --base-url $env:JAVA_AI_AGENT_BASE_URL `
    --create-token $env:JAVA_AI_AGENT_CREATE_TOKEN `
    --run-token $env:JAVA_AI_AGENT_RUN_TOKEN `
    --read-token $env:JAVA_AI_AGENT_READ_TOKEN `
    --report $ReportPrefix `
    --commit $Commit
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally {
  Restore-JavaAiEnvironment $JavaRuntime
}
