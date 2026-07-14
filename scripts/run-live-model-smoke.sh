#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
PROJECT_ROOT="$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)"
REPORT_PATH="${JAVA_AI_LIVE_REPORT_PATH:-$PROJECT_ROOT/docs/reports/lesson-04-live-model-smoke.md}"
MAIN_JAVA_HOME="${JAVA_AI_MAIN_JAVA_HOME:-${JAVA_HOME:-}}"

die() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 2
}

require_env() {
  local name="$1"
  [[ -n "${!name:-}" ]] || die "$name is required for the live model smoke test."
}

require_env JAVA_AI_CHAT_API_KEY
require_env JAVA_AI_CHAT_BASE_URL
require_env JAVA_AI_CHAT_MODEL

[[ -n "$MAIN_JAVA_HOME" ]] || die "Set JAVA_AI_MAIN_JAVA_HOME to a full JDK 21 or newer."
[[ -x "$MAIN_JAVA_HOME/bin/java" && -x "$MAIN_JAVA_HOME/bin/javac" ]] ||
  die "JAVA_AI_MAIN_JAVA_HOME must contain bin/java and bin/javac: $MAIN_JAVA_HOME"

javac_output="$($MAIN_JAVA_HOME/bin/javac -version 2>&1)" || die "Unable to run javac from $MAIN_JAVA_HOME"
javac_version="${javac_output#javac }"
javac_major="${javac_version%%.*}"
if [[ "$javac_major" == "1" ]]; then
  javac_major="${javac_version#1.}"
  javac_major="${javac_major%%.*}"
fi
[[ "$javac_major" =~ ^[0-9]+$ ]] || die "Unable to parse javac version: $javac_output"
(( javac_major >= 21 )) || die "The live model smoke test requires JDK 21 or newer."

commit="$(git -C "$PROJECT_ROOT" rev-parse HEAD 2>/dev/null || printf 'unknown')"

env JAVA_HOME="$MAIN_JAVA_HOME" PATH="$MAIN_JAVA_HOME/bin:$PATH" \
  "$PROJECT_ROOT/mvnw" -f "$PROJECT_ROOT/pom.xml" \
  -pl services/knowledge-service \
  -Dtest=LiveModelSmokeIT \
  -Djava-ai.smoke.report-path="$REPORT_PATH" \
  -Djava-ai.smoke.commit="$commit" \
  test

printf 'LIVE_MODEL report written to %s\n' "$REPORT_PATH"
