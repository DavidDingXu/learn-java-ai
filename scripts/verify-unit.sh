#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
PROJECT_ROOT="$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)"
WORKSPACE_ROOT="$(CDPATH= cd -- "$PROJECT_ROOT/../.." && pwd)"
MVNW="$PROJECT_ROOT/mvnw"

SELECTED_JDK=""
SELECTED_MAJOR=""

die() {
  local exit_code="$1"
  shift
  printf 'ERROR: %s\n' "$*" >&2
  exit "$exit_code"
}

javac_major() {
  local java_home="$1"
  local output
  local version

  output="$("$java_home/bin/javac" -version 2>&1)" || return 1
  version="${output#javac }"
  version="${version%%_*}"
  if [[ "$version" == 1.* ]]; then
    version="${version#1.}"
  fi
  printf '%s\n' "${version%%.*}"
}

try_main_jdk() {
  local candidate="$1"
  local major

  [[ -x "$candidate/bin/java" && -x "$candidate/bin/javac" ]] || return 1
  major="$(javac_major "$candidate")" || return 1
  [[ "$major" =~ ^[0-9]+$ ]] || return 1
  (( major >= 21 )) || return 1
  SELECTED_JDK="$candidate"
  SELECTED_MAJOR="$major"
}

try_jdk8() {
  local candidate="$1"
  local major

  [[ -x "$candidate/bin/java" && -x "$candidate/bin/javac" ]] || return 1
  major="$(javac_major "$candidate")" || return 1
  [[ "$major" == "8" ]] || return 1
  SELECTED_JDK="$candidate"
  SELECTED_MAJOR="$major"
}

scan_main_jdk() {
  local candidate

  if [[ -n "${JAVA_AI_MAIN_JAVA_HOME:-}" ]]; then
    try_main_jdk "$JAVA_AI_MAIN_JAVA_HOME" ||
      die 2 "JAVA_AI_MAIN_JAVA_HOME is not a full JDK with javac major >= 21: $JAVA_AI_MAIN_JAVA_HOME"
    return
  fi

  if [[ -n "${JAVA_HOME:-}" ]] && try_main_jdk "$JAVA_HOME"; then
    return
  fi

  if [[ "$(uname -s)" == "Darwin" && -x /usr/libexec/java_home ]]; then
    candidate="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
    if [[ -n "$candidate" ]] && try_main_jdk "$candidate"; then
      return
    fi
  fi

  for candidate in \
    /Library/Java/JavaVirtualMachines/*/Contents/Home \
    "$HOME"/Library/Java/JavaVirtualMachines/*/Contents/Home \
    /opt/homebrew/opt/openjdk*/libexec/openjdk.jdk/Contents/Home \
    /usr/lib/jvm/* \
    "$HOME"/.sdkman/candidates/java/*; do
    [[ -d "$candidate" ]] || continue
    if try_main_jdk "$candidate"; then
      return
    fi
  done

  die 2 "No full JDK >= 21 found. Set JAVA_AI_MAIN_JAVA_HOME to a JDK home containing bin/java and bin/javac."
}

scan_jdk8() {
  local candidate

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

  die 2 "No full JDK 8 found. Set JAVA_AI_JDK8_HOME to a JDK 8 home containing bin/java and bin/javac."
}

run_maven() {
  local java_home="$1"
  shift
  env JAVA_HOME="$java_home" PATH="$java_home/bin:$PATH" "$MVNW" "$@"
}

[[ -x "$MVNW" ]] || die 2 "Maven wrapper is missing or not executable: $MVNW"
command -v node >/dev/null 2>&1 || die 2 "Node.js is required for repository contract tests."

scan_main_jdk
MAIN_JAVA_HOME="$SELECTED_JDK"
MAIN_JAVA_MAJOR="$SELECTED_MAJOR"

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

column_test_count=0
if [[ -d "$WORKSPACE_ROOT/column/scripts" ]]; then
  for test_file in "$WORKSPACE_ROOT"/column/scripts/*.test.mjs; do
    [[ -e "$test_file" ]] || continue
    node_tests+=("$test_file")
    ((column_test_count += 1))
  done
fi

if (( column_test_count == 0 )); then
  if [[ "${JAVA_AI_REQUIRE_COLUMN_TESTS:-0}" == "1" ]]; then
    die 2 "Column tests are required but were not found under $WORKSPACE_ROOT/column/scripts."
  fi
  printf 'NOTE: standalone project verification; column tests were not found and were not verified.\n'
else
  printf 'Column contract tests included: %s file(s).\n' "$column_test_count"
fi

node --test "${node_tests[@]}"

run_maven "$MAIN_JAVA_HOME" -f "$PROJECT_ROOT/pom.xml" verify
run_maven "$MAIN_JAVA_HOME" -f "$PROJECT_ROOT/labs/pom.xml" verify
run_maven "$JDK8_HOME" -f "$PROJECT_ROOT/integrations/jdk8-client/pom.xml" verify

printf 'Project unit verification passed for root, labs, Java 8 client, and project contracts.\n'
if (( column_test_count > 0 )); then
  printf 'Column contract verification also passed.\n'
else
  printf 'Column contract verification was not run.\n'
fi
