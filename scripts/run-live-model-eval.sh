#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
. "$ROOT_DIR/scripts/main-java-runtime.sh"
enter_java_ai_main_jdk
REPORT_PREFIX=${1:-"$ROOT_DIR/docs/reports/lesson-12-live-model-eval"}
PORT=${JAVA_AI_EVAL_PORT:-18081}
CONFIG_FILE="$ROOT_DIR/config/application.yml"

require_env() {
  local name=$1
  [[ -n "${!name:-}" ]] || { printf 'ERROR: %s is required.\n' "$name" >&2; exit 2; }
}

require_env JAVA_AI_EVAL_BEARER_TOKEN
require_env JAVA_AI_JWT_ISSUER
[[ -f "$CONFIG_FILE" ]] || { printf 'ERROR: Missing local demo config: %s\n' "$CONFIG_FILE" >&2; exit 2; }
[[ -n "${JAVA_AI_DEV_JWT_HMAC_SECRET:-}" || -n "${JAVA_AI_JWT_JWK_SET_URI:-}" ]] || {
  printf 'ERROR: JAVA_AI_DEV_JWT_HMAC_SECRET or JAVA_AI_JWT_JWK_SET_URI is required.\n' >&2
  exit 2
}

SECURITY_ARGS=(
  "--java-ai.security.jwt.enabled=true"
  "--java-ai.security.jwt.issuer=$JAVA_AI_JWT_ISSUER"
  "--java-ai.security.jwt.audience=${JAVA_AI_JWT_AUDIENCE:-knowledge-service}"
  "--java-ai.security.jwt.allowed-actors=${JAVA_AI_JWT_ALLOWED_ACTORS:-customer-bff}"
)
if [[ -n "${JAVA_AI_DEV_JWT_HMAC_SECRET:-}" ]]; then
  SECURITY_ARGS+=("--java-ai.security.jwt.hmac-secret=$JAVA_AI_DEV_JWT_HMAC_SECRET")
else
  SECURITY_ARGS+=("--java-ai.security.jwt.jwk-set-uri=$JAVA_AI_JWT_JWK_SET_URI")
fi

COMMIT=${JAVA_AI_EVAL_COMMIT:-$(git -C "$ROOT_DIR" rev-parse HEAD 2>/dev/null || echo unknown)}
env JAVA_HOME="$MAIN_JAVA_HOME" PATH="$MAIN_JAVA_HOME/bin:$PATH" \
  "$ROOT_DIR/mvnw" -f "$ROOT_DIR/pom.xml" \
  -pl services/knowledge-service,quality/eval-runner -am package -DskipTests

env JAVA_HOME="$MAIN_JAVA_HOME" PATH="$MAIN_JAVA_HOME/bin:$PATH" \
  "$MAIN_JAVA_HOME/bin/java" \
  -Dspring.config.additional-location="file:$CONFIG_FILE" \
  -jar "$ROOT_DIR/services/knowledge-service/target/knowledge-service-0.1.0-SNAPSHOT.jar" \
  --java-ai.knowledge.context-source=classpath \
  --java-ai.knowledge.ingestion.enabled=false \
  --spring.ai.model.embedding=none \
  --spring.flyway.enabled=false \
  --spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration \
  --server.address=127.0.0.1 \
  --server.port="$PORT" \
  "${SECURITY_ARGS[@]}" \
  >"${TMPDIR:-/tmp}/java-ai-live-eval-service.log" 2>&1 &
SERVICE_PID=$!
trap 'kill "$SERVICE_PID" 2>/dev/null || true; wait "$SERVICE_PID" 2>/dev/null || true' EXIT

for _ in $(seq 1 60); do
  if curl --fail --silent "http://127.0.0.1:$PORT/actuator/health" >/dev/null; then
    break
  fi
  sleep 1
done
curl --fail --silent "http://127.0.0.1:$PORT/actuator/health" >/dev/null || {
  printf 'ERROR: Knowledge Service did not become healthy. See %s\n' "${TMPDIR:-/tmp}/java-ai-live-eval-service.log" >&2
  exit 1
}

"$MAIN_JAVA_HOME/bin/java" \
  -jar "$ROOT_DIR/quality/eval-runner/target/eval-runner-0.1.0-SNAPSHOT-all.jar" \
  model-eval \
  --dataset "$ROOT_DIR/datasets/model-interaction/golden-set-v2.jsonl" \
  --base-url "http://127.0.0.1:$PORT" \
  --mode LIVE_MODEL \
  --bearer-token "$JAVA_AI_EVAL_BEARER_TOKEN" \
  --report "$REPORT_PREFIX" \
  --commit "$COMMIT"
