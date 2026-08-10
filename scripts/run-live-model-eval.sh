#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
. "$ROOT_DIR/scripts/main-java-runtime.sh"
enter_java_ai_main_jdk
REPORT_PREFIX=${1:-"$ROOT_DIR/docs/reports/lesson-12-live-model-eval"}
PORT=${JAVA_AI_EVAL_PORT:-18081}
CONFIG_FILE="$ROOT_DIR/config/application-base.yml"

if [[ ! -f "$CONFIG_FILE" ]]; then
  printf 'ERROR: Missing %s. Restore the tracked shared model configuration.\n' "$CONFIG_FILE" >&2
  exit 2
fi

SERVICE_PID=""
cleanup() {
  if [[ -n "$SERVICE_PID" ]]; then
    kill "$SERVICE_PID" 2>/dev/null || true
    wait "$SERVICE_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

COMMIT=${JAVA_AI_EVAL_COMMIT:-$(git -C "$ROOT_DIR" rev-parse HEAD 2>/dev/null || echo unknown)}
env JAVA_HOME="$MAIN_JAVA_HOME" PATH="$MAIN_JAVA_HOME/bin:$PATH" \
  "$ROOT_DIR/mvnw" -f "$ROOT_DIR/pom.xml" \
  -pl services/knowledge-service,quality/eval-runner -am package -DskipTests

env JAVA_HOME="$MAIN_JAVA_HOME" PATH="$MAIN_JAVA_HOME/bin:$PATH" \
  "$MAIN_JAVA_HOME/bin/java" \
  -Dspring.config.additional-location="file:$CONFIG_FILE" \
  -jar "$ROOT_DIR/services/knowledge-service/target/knowledge-service-0.1.0-SNAPSHOT.jar" \
  --java-ai.knowledge.mode=classpath \
  --spring.ai.model.embedding=none \
  --spring.flyway.enabled=false \
  --spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration \
  --java-ai.security.mode=fixed \
  --server.address=127.0.0.1 \
  --server.port="$PORT" \
  >"${TMPDIR:-/tmp}/java-ai-live-eval-service.log" 2>&1 &
SERVICE_PID=$!

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
  --prompt-version knowledge-answer-v1 \
  --environment-id local-live-model \
  --report "$REPORT_PREFIX" \
  --commit "$COMMIT"
