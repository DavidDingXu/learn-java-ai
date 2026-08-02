#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
. "$ROOT_DIR/scripts/main-java-runtime.sh"
enter_java_ai_main_jdk
REPORT_PREFIX=${1:-"$ROOT_DIR/docs/reports/lesson-21-retrieval-eval"}

BASE_URL=${JAVA_AI_RETRIEVAL_BASE_URL:-http://127.0.0.1:8081}
umask 077
CREDENTIAL_DIR=""
BEARER_TOKEN_FILE=""
cleanup() {
  if [[ -n "$CREDENTIAL_DIR" ]]; then
    rm -f -- "$BEARER_TOKEN_FILE"
    rmdir -- "$CREDENTIAL_DIR" 2>/dev/null || true
  fi
}
trap cleanup EXIT
COMMIT=${JAVA_AI_EVAL_COMMIT:-$(git -C "$ROOT_DIR" rev-parse HEAD 2>/dev/null || echo unknown)}
env JAVA_HOME="$MAIN_JAVA_HOME" PATH="$MAIN_JAVA_HOME/bin:$PATH" \
  "$ROOT_DIR/mvnw" -f "$ROOT_DIR/pom.xml" \
  -pl quality/eval-runner -am package -DskipTests

AUTH_ARGS=()
if [[ -n "${JAVA_AI_RETRIEVAL_EVAL_BEARER_TOKEN:-}" ]]; then
  CREDENTIAL_DIR=$(mktemp -d "${TMPDIR:-/tmp}/java-ai-retrieval-eval.XXXXXX")
  BEARER_TOKEN_FILE="$CREDENTIAL_DIR/bearer-token"
  printf '%s' "$JAVA_AI_RETRIEVAL_EVAL_BEARER_TOKEN" >"$BEARER_TOKEN_FILE"
  AUTH_ARGS=(--bearer-token-file "$BEARER_TOKEN_FILE")
fi

"$MAIN_JAVA_HOME/bin/java" \
  -jar "$ROOT_DIR/quality/eval-runner/target/eval-runner-0.1.0-SNAPSHOT-all.jar" \
  retrieval-eval \
  --dataset "$ROOT_DIR/datasets/retrieval/golden-set-v1.jsonl" \
  --base-url "$BASE_URL" \
  "${AUTH_ARGS[@]}" \
  --top-k "${JAVA_AI_RETRIEVAL_EVAL_TOP_K:-5}" \
  --min-recall "${JAVA_AI_RETRIEVAL_MIN_RECALL:-0.80}" \
  --min-hit-rate "${JAVA_AI_RETRIEVAL_MIN_HIT_RATE:-0.90}" \
  --min-mrr "${JAVA_AI_RETRIEVAL_MIN_MRR:-0.60}" \
  --max-duplicate-rate "${JAVA_AI_RETRIEVAL_MAX_DUPLICATE_RATE:-0.02}" \
  --max-p95-ms "${JAVA_AI_RETRIEVAL_MAX_P95_MS:-1500}" \
  --report "$REPORT_PREFIX" \
  --commit "$COMMIT"
