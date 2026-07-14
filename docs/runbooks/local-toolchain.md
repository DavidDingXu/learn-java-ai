# Local Toolchain Runbook

## Supported Build Boundaries

| Build | Required runtime | Entry point |
|---|---|---|
| Main reactor | full JDK 21 or newer; CI uses real JDK 21 | `pom.xml` |
| Framework labs | full JDK 21 or newer | `labs/pom.xml` |
| Legacy client | full JDK 8 exactly | `integrations/jdk8-client/pom.xml` |
| Contract checks | Node.js | `scripts/*.test.mjs` |

The author workstation currently has JDK 26 and JDK 8. Local JDK 26 runs compile the main modules with `--release 21`; they do not prove runtime behavior on a JDK 21 JVM. CI must include a real JDK 21 build.

## macOS/Linux

Set explicit homes before running the full verification:

```bash
export JAVA_AI_MAIN_JAVA_HOME=/path/to/full-jdk-21-or-newer
export JAVA_AI_JDK8_HOME=/path/to/full-jdk8

test -x "$JAVA_AI_MAIN_JAVA_HOME/bin/java"
test -x "$JAVA_AI_MAIN_JAVA_HOME/bin/javac"
test -x "$JAVA_AI_JDK8_HOME/bin/java"
test -x "$JAVA_AI_JDK8_HOME/bin/javac"

scripts/verify-unit.sh
```

On managed macOS machines, `/usr/libexec/java_home -v 1.8` may return a browser JRE without `javac`. Do not use that result unless both binaries exist and `javac -version` reports Java 8.

To require the surrounding paid-column checks from the combined workspace:

```bash
JAVA_AI_REQUIRE_COLUMN_TESTS=1 scripts/verify-unit.sh
```

An independent project clone may omit that flag. The script then states that column checks were not run.

## Windows PowerShell

Use JDK installation roots, not paths to individual executables:

```powershell
$env:JAVA_AI_MAIN_JAVA_HOME = "C:\\Java\\jdk-21"
$env:JAVA_AI_JDK8_HOME = "C:\\Java\\jdk8"
.\scripts\verify-unit.ps1
```

The script verifies `java.exe` and `javac.exe`, checks the major versions, runs Node contracts and invokes `mvnw.cmd` for all three Maven boundaries.

PowerShell files are statically checked on macOS. A release that promises Windows support still needs a real Windows run with the command output retained.

## Run One Build

Main reactor:

```bash
JAVA_HOME="$JAVA_AI_MAIN_JAVA_HOME" \
PATH="$JAVA_HOME/bin:$PATH" \
./mvnw verify
```

Labs:

```bash
JAVA_HOME="$JAVA_AI_MAIN_JAVA_HOME" \
PATH="$JAVA_HOME/bin:$PATH" \
./mvnw -f labs/pom.xml verify
```

Java 8 client:

```bash
JAVA_HOME="$JAVA_AI_JDK8_HOME" \
PATH="$JAVA_HOME/bin:$PATH" \
./mvnw -f integrations/jdk8-client/pom.xml verify
```

## External Health Smoke

The external script requires an explicit deployed base URL:

```bash
JAVA_AI_EXTERNAL_BASE_URL=https://test.example.com \
scripts/verify-integration.sh
```

Missing configuration exits with code 2. A successful run proves only that `/actuator/health` returned HTTP 200 with `status=UP`. It does not validate model calls, databases, vector search, object storage or business workflows.

## No-Docker Development

Daily verification must remain usable when Docker is blocked by company policy. Use deterministic unit tests, contract fixtures and `local-lite` for normal work. When real infrastructure is introduced, choose one explicit path:

- an externally managed test environment;
- CI runners allowed to start containers;
- a company-provided remote development environment.

Record which path produced each report. Do not translate a local substitute into a production claim.

## Common Failures

### Java 8 build reports no compiler

`JAVA_AI_JDK8_HOME` points to a JRE. Select a full JDK 8 and confirm `bin/javac` exists.

### Main reactor reports the wrong Java version

Set `JAVA_AI_MAIN_JAVA_HOME` explicitly. The script requires javac major 21 or newer.

### Standalone clone cannot find column tests

Leave `JAVA_AI_REQUIRE_COLUMN_TESTS=0`. Set it to `1` only in the combined workspace release gate.

### External verification exits with code 2

Set an absolute `http` or `https` `JAVA_AI_EXTERNAL_BASE_URL`. The script intentionally refuses to infer an environment.
