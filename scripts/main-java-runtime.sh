#!/usr/bin/env bash

java_ai_javac_major() {
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

java_ai_java_major() {
  local java_home="$1"
  local output
  local version

  output="$(env JAVA_HOME="$java_home" PATH="$java_home/bin:${PATH:-}" \
    "$java_home/bin/java" -version 2>&1)" || return 1
  [[ "$output" == *\"* ]] || return 1
  version="${output#*\"}"
  version="${version%%\"*}"
  version="${version%%_*}"
  if [[ "$version" == 1.* ]]; then
    version="${version#1.}"
  fi
  printf '%s\n' "${version%%.*}"
}

java_ai_try_main_jdk() {
  local candidate="$1"
  local java_major
  local javac_major

  [[ -f "$candidate/release" ]] || return 1
  [[ -x "$candidate/bin/java" && -x "$candidate/bin/javac" ]] || return 1
  java_major="$(java_ai_java_major "$candidate")" || return 1
  javac_major="$(java_ai_javac_major "$candidate")" || return 1
  [[ "$java_major" =~ ^[0-9]+$ && "$javac_major" =~ ^[0-9]+$ ]] || return 1
  [[ "$java_major" == "$javac_major" ]] || return 1
  (( javac_major >= 21 )) || return 1
  MAIN_JAVA_HOME="$candidate"
  MAIN_JAVA_MAJOR="$javac_major"
}

enter_java_ai_main_jdk() {
  local candidate
  local javac_path

  MAIN_JAVA_HOME=""
  MAIN_JAVA_MAJOR=""

  if [[ -n "${JAVA_AI_MAIN_JAVA_HOME:-}" ]]; then
    if ! java_ai_try_main_jdk "$JAVA_AI_MAIN_JAVA_HOME"; then
      printf 'ERROR: JAVA_AI_MAIN_JAVA_HOME is not a full JDK 21 or newer: %s\n' \
        "$JAVA_AI_MAIN_JAVA_HOME" >&2
      return 2
    fi
    return
  fi

  if [[ -n "${JAVA_HOME:-}" ]] && java_ai_try_main_jdk "$JAVA_HOME"; then
    return
  fi

  javac_path="$(command -v javac 2>/dev/null || true)"
  if [[ -n "$javac_path" ]]; then
    candidate="$(CDPATH= cd -- "$(dirname -- "$javac_path")/.." 2>/dev/null && pwd || true)"
    if [[ -n "$candidate" ]] && java_ai_try_main_jdk "$candidate"; then
      return
    fi
  fi

  if [[ "$(uname -s)" == "Darwin" && -x /usr/libexec/java_home ]]; then
    candidate="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
    if [[ -n "$candidate" ]] && java_ai_try_main_jdk "$candidate"; then
      return
    fi
  fi

  for candidate in \
    /Library/Java/JavaVirtualMachines/*/Contents/Home \
    "${HOME:-}"/Library/Java/JavaVirtualMachines/*/Contents/Home \
    /opt/homebrew/opt/openjdk*/libexec/openjdk.jdk/Contents/Home \
    /usr/lib/jvm/* \
    "${HOME:-}"/.sdkman/candidates/java/*; do
    [[ -d "$candidate" ]] || continue
    if java_ai_try_main_jdk "$candidate"; then
      return
    fi
  done

  printf '%s\n' 'ERROR: No full JDK 21 or newer was found. Install a JDK and rerun the command.' >&2
  return 2
}
