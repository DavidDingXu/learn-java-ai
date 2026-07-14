#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
REQUIRE_EXTERNAL=${JAVA_AI_RELEASE_REQUIRE_EXTERNAL:-0}

"$ROOT_DIR/scripts/verify-unit.sh"

secret_pattern='sk-[A-Za-z0-9_-]{20,}|memberai\.tech'
secret_hits=""
while IFS= read -r -d '' file; do
  case "$file" in
    scripts/release-gate.sh|scripts/release-gate.ps1) continue ;;
  esac
  if [[ -f "$ROOT_DIR/$file" ]]; then
    match=$(rg -n --no-heading "$secret_pattern" "$ROOT_DIR/$file" || true)
    if [[ -n "$match" ]]; then
      secret_hits+="$match"$'\n'
    fi
  fi
done < <(git -C "$ROOT_DIR" ls-files -co --exclude-standard -z)
if [[ -n "$secret_hits" ]]; then
  printf 'ERROR: possible secret or private provider endpoint found:\n%s' "$secret_hits" >&2
  exit 1
fi

if [[ "$REQUIRE_EXTERNAL" == "1" ]]; then
  [[ -n "${JAVA_AI_EXTERNAL_BASE_URL:-}" ]] || {
    printf 'ERROR: JAVA_AI_EXTERNAL_BASE_URL is required when JAVA_AI_RELEASE_REQUIRE_EXTERNAL=1.\n' >&2
    exit 2
  }
  "$ROOT_DIR/scripts/verify-integration.sh"
elif [[ "$REQUIRE_EXTERNAL" != "0" ]]; then
  printf 'ERROR: JAVA_AI_RELEASE_REQUIRE_EXTERNAL must be 0 or 1.\n' >&2
  exit 2
fi

printf 'Release gate passed. External evidence required: %s.\n' "$REQUIRE_EXTERNAL"
