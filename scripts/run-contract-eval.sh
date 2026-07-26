#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
. "$ROOT_DIR/scripts/main-java-runtime.sh"
enter_java_ai_main_jdk
REPORT_PREFIX=${1:-"$ROOT_DIR/docs/reports/lesson-12-contract-eval"}
COMMIT=${JAVA_AI_EVAL_COMMIT:-$(git -C "$ROOT_DIR" rev-parse HEAD 2>/dev/null || echo unknown)}

env JAVA_HOME="$MAIN_JAVA_HOME" PATH="$MAIN_JAVA_HOME/bin:$PATH" \
  "$ROOT_DIR/mvnw" -f "$ROOT_DIR/pom.xml" -pl quality/eval-runner -am package -DskipTests
"$MAIN_JAVA_HOME/bin/java" -jar "$ROOT_DIR/quality/eval-runner/target/eval-runner-0.1.0-SNAPSHOT-all.jar" \
  contract-eval \
  --dataset "$ROOT_DIR/datasets/model-interaction/golden-set-v2.jsonl" \
  --report "$REPORT_PREFIX" \
  --commit "$COMMIT"
