#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
PROJECT_ROOT="$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)"
MVNW="$PROJECT_ROOT/mvnw"
CUSTOMER_WEB_DIR="$PROJECT_ROOT/apps/customer-web"
. "$SCRIPT_DIR/main-java-runtime.sh"

SELECTED_JDK=""
SELECTED_MAJOR=""

die() {
  local exit_code="$1"
  shift
  printf 'ERROR: %s\n' "$*" >&2
  exit "$exit_code"
}

try_jdk8() {
  local candidate="$1"
  local java_major
  local javac_major

  [[ -f "$candidate/release" ]] || return 1
  [[ -x "$candidate/bin/java" && -x "$candidate/bin/javac" ]] || return 1
  java_major="$(java_ai_java_major "$candidate")" || return 1
  javac_major="$(java_ai_javac_major "$candidate")" || return 1
  [[ "$java_major" == "8" && "$javac_major" == "8" ]] || return 1
  SELECTED_JDK="$candidate"
  SELECTED_MAJOR="$javac_major"
}

scan_jdk8() {
  local candidate
  local javac_path

  SELECTED_JDK=""
  SELECTED_MAJOR=""

  if [[ -n "${JAVA_AI_JDK8_HOME:-}" ]]; then
    try_jdk8 "$JAVA_AI_JDK8_HOME" ||
      die 2 "JAVA_AI_JDK8_HOME is not a full JDK 8: $JAVA_AI_JDK8_HOME"
    return
  fi

  if [[ -n "${JAVA_HOME:-}" ]] && try_jdk8 "$JAVA_HOME"; then
    return
  fi

  javac_path="$(command -v javac 2>/dev/null || true)"
  if [[ -n "$javac_path" ]]; then
    candidate="$(CDPATH= cd -- "$(dirname -- "$javac_path")/.." 2>/dev/null && pwd || true)"
    if [[ -n "$candidate" ]] && try_jdk8 "$candidate"; then
      return
    fi
  fi

  for candidate in \
    /Library/Java/JavaVirtualMachines/*/Contents/Home \
    "$HOME"/Library/Java/JavaVirtualMachines/*/Contents/Home \
    /usr/lib/jvm/* \
    "$HOME"/.sdkman/candidates/java/*; do
    [[ -d "$candidate" ]] || continue
    if try_jdk8 "$candidate"; then
      return
    fi
  done

  die 2 "No full JDK 8 was found. Install a JDK 8 containing bin/java and bin/javac, then rerun the command."
}

run_maven() {
  local java_home="$1"
  shift
  env JAVA_HOME="$java_home" PATH="$java_home/bin:$PATH" "$MVNW" "$@"
}

[[ -x "$MVNW" ]] || die 2 "Maven wrapper is missing or not executable: $MVNW"
command -v node >/dev/null 2>&1 || die 2 "Node.js is required for repository contract tests."
command -v npm >/dev/null 2>&1 || die 2 "npm is required to verify Customer Web."

NODE_MAJOR="$(node -p 'process.versions.node.split(".")[0]')"
[[ "$NODE_MAJOR" =~ ^[0-9]+$ ]] || die 2 "Unable to determine the Node.js major version."
(( NODE_MAJOR >= 24 )) || die 2 "Node.js 24 or newer is required; found major $NODE_MAJOR."
[[ -f "$CUSTOMER_WEB_DIR/package.json" ]] || die 2 "Customer Web package.json is missing."
[[ -f "$CUSTOMER_WEB_DIR/package-lock.json" ]] || die 2 "Customer Web package-lock.json is missing."

enter_java_ai_main_jdk || exit $?

scan_jdk8
JDK8_HOME="$SELECTED_JDK"

printf 'Main reactor JDK: %s (javac major %s)\n' "$MAIN_JAVA_HOME" "$MAIN_JAVA_MAJOR"
if (( MAIN_JAVA_MAJOR > 21 )); then
  printf 'NOTE: this proves --release 21 compilation on JDK %s, not execution on a JDK 21 JVM.\n' "$MAIN_JAVA_MAJOR"
fi
printf 'Java 8 client JDK: %s (javac major 8)\n' "$JDK8_HOME"

node_tests=()
for test_file in "$PROJECT_ROOT"/scripts/*.test.mjs; do
  [[ -e "$test_file" ]] || continue
  node_tests+=("$test_file")
done
(( ${#node_tests[@]} > 0 )) || die 2 "No project contract tests were found under $PROJECT_ROOT/scripts."

node --test "${node_tests[@]}"

npm --prefix "$CUSTOMER_WEB_DIR" ci --no-audit --no-fund
npm --prefix "$CUSTOMER_WEB_DIR" run typecheck
npm --prefix "$CUSTOMER_WEB_DIR" test
npm --prefix "$CUSTOMER_WEB_DIR" run build

run_maven "$MAIN_JAVA_HOME" -f "$PROJECT_ROOT/pom.xml" verify
run_maven "$MAIN_JAVA_HOME" -f "$PROJECT_ROOT/labs/pom.xml" verify
run_maven "$JDK8_HOME" -f "$PROJECT_ROOT/integrations/jdk8-client/pom.xml" verify

printf 'Project verification passed for Customer Web, root, labs, Java 8 client, and project contracts.\n'
