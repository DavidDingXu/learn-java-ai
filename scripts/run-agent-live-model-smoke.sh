#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
PROJECT_ROOT="$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)"
REPORT_PATH="${JAVA_AI_AGENT_LIVE_REPORT_PATH:-$PROJECT_ROOT/docs/reports/lesson-34-agent-live-model-smoke.md}"
MAIN_JAVA_HOME="${JAVA_AI_MAIN_JAVA_HOME:-${JAVA_HOME:-}}"

die() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 2
}

for name in JAVA_AI_CHAT_API_KEY JAVA_AI_CHAT_BASE_URL JAVA_AI_CHAT_MODEL; do
  [[ -n "${!name:-}" ]] || die "$name is required for the agent live model smoke test."
done

[[ -n "$MAIN_JAVA_HOME" ]] || die "Set JAVA_AI_MAIN_JAVA_HOME to a full JDK 21 or newer."
[[ -x "$MAIN_JAVA_HOME/bin/java" && -x "$MAIN_JAVA_HOME/bin/javac" ]] ||
  die "JAVA_AI_MAIN_JAVA_HOME must contain bin/java and bin/javac: $MAIN_JAVA_HOME"

commit="$(git -C "$PROJECT_ROOT" rev-parse HEAD 2>/dev/null || printf 'unknown')"

env JAVA_HOME="$MAIN_JAVA_HOME" PATH="$MAIN_JAVA_HOME/bin:$PATH" \
  "$PROJECT_ROOT/mvnw" -f "$PROJECT_ROOT/pom.xml" \
  -pl services/ticket-agent-service \
  -Dtest=TicketAgentLiveModelSmokeIT \
  -Djava-ai.agent-smoke.report-path="$REPORT_PATH" \
  -Djava-ai.agent-smoke.commit="$commit" \
  test

printf 'Agent LIVE_MODEL report written to %s\n' "$REPORT_PATH"
