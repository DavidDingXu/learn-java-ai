#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"

"$SCRIPT_DIR/verify-unit.sh"

printf 'Phase B build and runtime contracts passed without external model or infrastructure calls.\n'
