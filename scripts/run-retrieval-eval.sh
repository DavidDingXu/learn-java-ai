#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
. "$ROOT_DIR/scripts/main-java-runtime.sh"
enter_java_ai_main_jdk
REPORT_PREFIX=${1:-"$ROOT_DIR/docs/reports/lesson-21-retrieval-eval"}

require_env() {
  local name=$1
  [[ -n "${!name:-}" ]] || { printf 'ERROR: %s is required.\n' "$name" >&2; exit 2; }
}

require_env JAVA_AI_RETRIEVAL_BASE_URL
require_env JAVA_AI_RETRIEVAL_EVAL_BEARER_TOKEN
COMMIT=${JAVA_AI_EVAL_COMMIT:-$(git -C "$ROOT_DIR" rev-parse HEAD 2>/dev/null || echo unknown)}
env JAVA_HOME="$MAIN_JAVA_HOME" PATH="$MAIN_JAVA_HOME/bin:$PATH" \
  "$ROOT_DIR/mvnw" -f "$ROOT_DIR/pom.xml" \
  -pl quality/eval-runner -am package -DskipTests

"$MAIN_JAVA_HOME/bin/java" \
  -jar "$ROOT_DIR/quality/eval-runner/target/eval-runner-0.1.0-SNAPSHOT-all.jar" \
  retrieval-eval \
  --dataset "$ROOT_DIR/datasets/retrieval/golden-set-v1.jsonl" \
  --base-url "$JAVA_AI_RETRIEVAL_BASE_URL" \
  --bearer-token "$JAVA_AI_RETRIEVAL_EVAL_BEARER_TOKEN" \
  --top-k "${JAVA_AI_RETRIEVAL_EVAL_TOP_K:-5}" \
  --min-recall "${JAVA_AI_RETRIEVAL_MIN_RECALL:-0.80}" \
  --min-hit-rate "${JAVA_AI_RETRIEVAL_MIN_HIT_RATE:-0.90}" \
  --min-mrr "${JAVA_AI_RETRIEVAL_MIN_MRR:-0.60}" \
  --max-duplicate-rate "${JAVA_AI_RETRIEVAL_MAX_DUPLICATE_RATE:-0.02}" \
  --max-p95-ms "${JAVA_AI_RETRIEVAL_MAX_P95_MS:-1500}" \
  --report "$REPORT_PREFIX" \
  --commit "$COMMIT"
