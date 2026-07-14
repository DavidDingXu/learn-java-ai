#!/usr/bin/env bash
set -euo pipefail

die() {
  local exit_code="$1"
  shift
  printf 'ERROR: %s\n' "$*" >&2
  exit "$exit_code"
}

BASE_URL="${JAVA_AI_EXTERNAL_BASE_URL:-}"
[[ -n "$BASE_URL" ]] ||
  die 2 "JAVA_AI_EXTERNAL_BASE_URL is required; refusing to claim external verification without an explicit environment."

command -v curl >/dev/null 2>&1 || die 2 "curl is required for the external health smoke."
command -v node >/dev/null 2>&1 || die 2 "Node.js is required to validate the health response as JSON."

HEALTH_URL="$(node -e '
  const base = new URL(process.argv[1]);
  base.pathname = `${base.pathname.replace(/\/$/, "")}/actuator/health`;
  base.search = "";
  base.hash = "";
  process.stdout.write(base.href);
' "$BASE_URL" 2>/dev/null)" || die 2 "JAVA_AI_EXTERNAL_BASE_URL must be an absolute http(s) URL: $BASE_URL"

case "$HEALTH_URL" in
  http://*|https://*) ;;
  *) die 2 "JAVA_AI_EXTERNAL_BASE_URL must use http or https: $BASE_URL" ;;
esac

RESPONSE_FILE="$(mktemp)"
trap 'rm -f "$RESPONSE_FILE"' EXIT

HTTP_STATUS="$(curl --silent --show-error \
  --connect-timeout 5 \
  --max-time 15 \
  --output "$RESPONSE_FILE" \
  --write-out '%{http_code}' \
  "$HEALTH_URL")" || die 1 "External health request failed: $HEALTH_URL"

[[ "$HTTP_STATUS" == "200" ]] ||
  die 1 "External health returned HTTP $HTTP_STATUS: $HEALTH_URL"

node - "$RESPONSE_FILE" <<'NODE' || die 1 "External health response is not JSON with status=UP."
const fs = require("node:fs");
const body = JSON.parse(fs.readFileSync(process.argv[2], "utf8"));
if (body.status !== "UP") {
  process.exit(1);
}
NODE

printf 'External health smoke passed: %s\n' "$HEALTH_URL"
printf 'Scope: one HTTP health endpoint only; no database, vector, object-storage, or end-to-end evidence was produced.\n'
