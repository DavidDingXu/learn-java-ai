#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
. "$ROOT_DIR/scripts/main-java-runtime.sh"
enter_java_ai_main_jdk
REPORT_PREFIX=${1:-"$ROOT_DIR/docs/reports/lesson-34-agent-eval"}

require_env() {
  local name=$1
  [[ -n "${!name:-}" ]] || { printf 'ERROR: %s is required.\n' "$name" >&2; exit 2; }
}

for name in JAVA_AI_AGENT_BASE_URL JAVA_AI_AGENT_CREATE_TOKEN JAVA_AI_AGENT_RUN_TOKEN JAVA_AI_AGENT_READ_TOKEN; do
  require_env "$name"
done
COMMIT=${JAVA_AI_EVAL_COMMIT:-$(git -C "$ROOT_DIR" rev-parse HEAD 2>/dev/null || echo unknown)}
env JAVA_HOME="$MAIN_JAVA_HOME" PATH="$MAIN_JAVA_HOME/bin:$PATH" \
  "$ROOT_DIR/mvnw" -f "$ROOT_DIR/pom.xml" \
  -pl quality/eval-runner -am package -DskipTests

"$MAIN_JAVA_HOME/bin/java" \
  -jar "$ROOT_DIR/quality/eval-runner/target/eval-runner-0.1.0-SNAPSHOT-all.jar" \
  agent-eval \
  --dataset "$ROOT_DIR/datasets/agent/golden-set-v2.jsonl" \
  --base-url "$JAVA_AI_AGENT_BASE_URL" \
  --create-token "$JAVA_AI_AGENT_CREATE_TOKEN" \
  --run-token "$JAVA_AI_AGENT_RUN_TOKEN" \
  --read-token "$JAVA_AI_AGENT_READ_TOKEN" \
  --report "$REPORT_PREFIX" \
  --commit "$COMMIT"
