#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
REPORT_PREFIX=${1:-"$ROOT_DIR/docs/reports/lesson-12-contract-eval"}
COMMIT=${JAVA_AI_EVAL_COMMIT:-$(git -C "$ROOT_DIR" rev-parse HEAD 2>/dev/null || echo unknown)}

"$ROOT_DIR/mvnw" -f "$ROOT_DIR/pom.xml" -pl quality/eval-runner -am package -DskipTests
java -jar "$ROOT_DIR/quality/eval-runner/target/eval-runner-0.1.0-SNAPSHOT.jar" \
  contract-eval \
  --dataset "$ROOT_DIR/datasets/model-interaction/golden-set-v2.jsonl" \
  --report "$REPORT_PREFIX" \
  --commit "$COMMIT"
