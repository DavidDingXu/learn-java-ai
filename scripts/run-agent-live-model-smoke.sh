#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
PROJECT_ROOT="$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)"
. "$SCRIPT_DIR/main-java-runtime.sh"
enter_java_ai_main_jdk
CONFIG_FILE="$PROJECT_ROOT/config/application-base.yml"
REPORT_PATH="${JAVA_AI_AGENT_LIVE_REPORT_PATH:-$PROJECT_ROOT/docs/reports/lesson-34-agent-live-model-smoke.md}"

die() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 2
}

if [[ ! -f "$CONFIG_FILE" ]]; then
  die "Missing $CONFIG_FILE. Restore the tracked shared model configuration."
fi

commit="$(git -C "$PROJECT_ROOT" rev-parse HEAD 2>/dev/null || printf 'unknown')"

env JAVA_HOME="$MAIN_JAVA_HOME" PATH="$MAIN_JAVA_HOME/bin:$PATH" \
  "$PROJECT_ROOT/mvnw" -f "$PROJECT_ROOT/pom.xml" \
  -pl services/ticket-agent-service \
  -Dtest=TicketAgentLiveModelSmokeIT \
  -Dspring.config.additional-location="file:$CONFIG_FILE" \
  -Djava-ai.agent-smoke.report-path="$REPORT_PATH" \
  -Djava-ai.agent-smoke.commit="$commit" \
  test

printf 'Agent LIVE_MODEL report written to %s\n' "$REPORT_PATH"
