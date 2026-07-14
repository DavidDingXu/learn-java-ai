#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
"$SCRIPT_DIR/verify-unit.sh"
printf 'Local-lite verification passed without requiring Docker or external infrastructure.\n'
