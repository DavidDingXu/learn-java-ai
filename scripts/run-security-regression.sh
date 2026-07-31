#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
. "$ROOT_DIR/scripts/main-java-runtime.sh"
enter_java_ai_main_jdk
REPORT_PREFIX=${1:-"$ROOT_DIR/docs/reports/lesson-35-security-eval"}

require_env() {
  local name=$1
  [[ -n "${!name:-}" ]] || { printf 'ERROR: %s is required.\n' "$name" >&2; exit 2; }
}

for name in JAVA_AI_AGENT_BASE_URL JAVA_AI_AGENT_CREATE_TOKEN JAVA_AI_AGENT_RUN_TOKEN JAVA_AI_AGENT_READ_TOKEN; do
  require_env "$name"
done
umask 077
CREDENTIAL_DIR=""
CREATE_TOKEN_FILE=""
RUN_TOKEN_FILE=""
READ_TOKEN_FILE=""
cleanup() {
  if [[ -n "$CREDENTIAL_DIR" ]]; then
    rm -f -- "$CREATE_TOKEN_FILE" "$RUN_TOKEN_FILE" "$READ_TOKEN_FILE"
    rmdir -- "$CREDENTIAL_DIR" 2>/dev/null || true
  fi
}
trap cleanup EXIT
COMMIT=${JAVA_AI_EVAL_COMMIT:-$(git -C "$ROOT_DIR" rev-parse HEAD 2>/dev/null || echo unknown)}
env JAVA_HOME="$MAIN_JAVA_HOME" PATH="$MAIN_JAVA_HOME/bin:$PATH" \
  "$ROOT_DIR/mvnw" -f "$ROOT_DIR/pom.xml" \
  -pl services/knowledge-service,services/ticket-agent-service,quality/eval-runner \
  -Dtest=KnowledgeJwtSecurityTest,TicketAgentJwtSecurityTest,BusinessToolCatalogTest,SpringAiTicketAgentPlannerPromptTest,AgentEvaluatorTest \
  -Dsurefire.failIfNoSpecifiedTests=false test package

CREDENTIAL_DIR=$(mktemp -d "${TMPDIR:-/tmp}/java-ai-security-eval.XXXXXX")
CREATE_TOKEN_FILE="$CREDENTIAL_DIR/create-token"
RUN_TOKEN_FILE="$CREDENTIAL_DIR/run-token"
READ_TOKEN_FILE="$CREDENTIAL_DIR/read-token"
printf '%s' "$JAVA_AI_AGENT_CREATE_TOKEN" >"$CREATE_TOKEN_FILE"
printf '%s' "$JAVA_AI_AGENT_RUN_TOKEN" >"$RUN_TOKEN_FILE"
printf '%s' "$JAVA_AI_AGENT_READ_TOKEN" >"$READ_TOKEN_FILE"

"$MAIN_JAVA_HOME/bin/java" \
  -jar "$ROOT_DIR/quality/eval-runner/target/eval-runner-0.1.0-SNAPSHOT-all.jar" \
  security-eval \
  --dataset "$ROOT_DIR/datasets/security/agent-security-v1.jsonl" \
  --base-url "$JAVA_AI_AGENT_BASE_URL" \
  --create-token-file "$CREATE_TOKEN_FILE" \
  --run-token-file "$RUN_TOKEN_FILE" \
  --read-token-file "$READ_TOKEN_FILE" \
  --report "$REPORT_PREFIX" \
  --commit "$COMMIT"
