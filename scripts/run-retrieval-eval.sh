#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
REPORT_PREFIX=${1:-"$ROOT_DIR/docs/reports/lesson-21-retrieval-eval"}
MAIN_JAVA_HOME=${JAVA_AI_MAIN_JAVA_HOME:-${JAVA_HOME:-}}

require_env() {
  local name=$1
  [[ -n "${!name:-}" ]] || { printf 'ERROR: %s is required.\n' "$name" >&2; exit 2; }
}

require_env JAVA_AI_RETRIEVAL_BASE_URL
require_env JAVA_AI_RETRIEVAL_EVAL_BEARER_TOKEN
[[ -n "$MAIN_JAVA_HOME" && -x "$MAIN_JAVA_HOME/bin/java" ]] || {
  printf 'ERROR: JAVA_AI_MAIN_JAVA_HOME must point to JDK 21 or newer.\n' >&2
  exit 2
}

COMMIT=${JAVA_AI_EVAL_COMMIT:-$(git -C "$ROOT_DIR" rev-parse HEAD 2>/dev/null || echo unknown)}
env JAVA_HOME="$MAIN_JAVA_HOME" PATH="$MAIN_JAVA_HOME/bin:$PATH" \
  "$ROOT_DIR/mvnw" -f "$ROOT_DIR/pom.xml" \
  -pl quality/eval-runner -am package -DskipTests

"$MAIN_JAVA_HOME/bin/java" \
  -jar "$ROOT_DIR/quality/eval-runner/target/eval-runner-0.1.0-SNAPSHOT.jar" \
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
