#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
PROJECT_ROOT="$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)"
. "$SCRIPT_DIR/main-java-runtime.sh"
enter_java_ai_main_jdk
CONFIG_FILE="$PROJECT_ROOT/config/application.yml"
REPORT_PATH="${JAVA_AI_LIVE_REPORT_PATH:-$PROJECT_ROOT/docs/reports/lesson-04-live-model-smoke.md}"

die() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 2
}

[[ -f "$CONFIG_FILE" ]] || die "Missing local demo config: $CONFIG_FILE"

commit="$(git -C "$PROJECT_ROOT" rev-parse HEAD 2>/dev/null || printf 'unknown')"

env JAVA_HOME="$MAIN_JAVA_HOME" PATH="$MAIN_JAVA_HOME/bin:$PATH" \
  "$PROJECT_ROOT/mvnw" -f "$PROJECT_ROOT/pom.xml" \
  -pl services/knowledge-service \
  -Dtest=LiveModelSmokeIT \
  -Dspring.config.additional-location="file:$CONFIG_FILE" \
  -Djava-ai.smoke.report-path="$REPORT_PATH" \
  -Djava-ai.smoke.commit="$commit" \
  test

printf 'LIVE_MODEL report written to %s\n' "$REPORT_PATH"
