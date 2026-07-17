# Local Toolchain Runbook

## Supported Build Boundaries

| Build | Required runtime | Entry point |
|---|---|---|
| Main reactor | full JDK 21 or newer; CI runs on JDK 21 | `pom.xml` |
| Framework labs | full JDK 21 or newer | `labs/pom.xml` |
| Legacy client | full JDK 8 exactly | `integrations/jdk8-client/pom.xml` |
| Repository checks | Node.js 24 or newer | `scripts/*.test.mjs` |

使用高于 21 的 JDK 时，主模块仍通过 `--release 21` 生成 Java 21 字节码；这只能检查编译兼容性，不能替代 JDK 21 CI 运行。

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

## Development And External Infrastructure

日常代码回归使用 `src/test` 下的确定性配置、单元测试以及接口和规则测试。需要连接数据库、模型、对象存储或外部业务系统时，通过根目录 `.env` 或部署系统显式注入参数：

- 统一管理的测试环境；
- 允许启动容器的 CI Runner；
- 远程开发环境。

报告必须记录实际使用的环境和验证边界，不能用本地替代实现推导生产结论。

## Common Failures

### Java 8 build reports no compiler

`JAVA_AI_JDK8_HOME` points to a JRE. Select a full JDK 8 and confirm `bin/javac` exists.

### Main reactor reports the wrong Java version

Set `JAVA_AI_MAIN_JAVA_HOME` explicitly. The script requires javac major 21 or newer.

### External verification exits with code 2

Set an absolute `http` or `https` `JAVA_AI_EXTERNAL_BASE_URL`. The script intentionally refuses to infer an environment.
