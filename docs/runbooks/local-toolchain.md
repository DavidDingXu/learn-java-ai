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

安装完整 JDK 21+ 和 JDK 8 后，直接运行：

```bash
scripts/verify-unit.sh
```

脚本会检查当前 `JAVA_HOME`、`PATH` 和常见 JDK 安装目录，只接受同时包含 `java` 与 `javac` 的完整 JDK。macOS 的 `/usr/libexec/java_home -v 1.8` 可能返回不含 `javac` 的旧浏览器 JRE，因此脚本不会把它作为 JDK 8 自动发现来源。

## Windows PowerShell

安装完整 JDK 21+ 和 JDK 8 后，直接运行：

```powershell
.\scripts\verify-unit.ps1
```

脚本会从 `JAVA_HOME`、`PATH`、`Program Files` 和用户 `.jdks` 目录寻找 JDK，校验 `java.exe`、`javac.exe` 与主版本，再执行三个 Maven 构建边界和 Node 检查。

PowerShell files are statically checked on macOS. A release that promises Windows support still needs a real Windows run with the command output retained.

## Run One Build

主 reactor 和框架实验使用当前 JDK 21+：

```bash
./mvnw verify
./mvnw -f labs/pom.xml verify
```

Java 8 客户端需要切换到 JDK 8。正常情况下直接使用统一的 `verify-unit` 脚本即可，它会自动选择并隔离两个 JDK。

## External Health Smoke

The external script requires an explicit deployed base URL:

```bash
JAVA_AI_EXTERNAL_BASE_URL=https://test.example.com \
scripts/verify-integration.sh
```

Missing configuration exits with code 2. A successful run proves only that `/actuator/health` returned HTTP 200 with `status=UP`. It does not validate model calls, databases, vector search, object storage or business workflows.

## Development And External Infrastructure

日常代码回归使用 `src/test` 下的确定性配置、单元测试以及接口和规则测试，不需要模型密钥或 `.env`。真实模型演示读取 `config/application.yml`。只有连接数据库、对象存储、身份平台或外部业务系统时，才需要根目录 `.env` 或部署系统参数：

- 统一管理的测试环境；
- 允许启动容器的 CI Runner；
- 远程开发环境。

报告必须记录实际使用的环境和验证边界，不能用本地替代实现推导生产结论。

## Common Failures

### Java 8 build reports no compiler

自动发现的 Java 8 目录只有 JRE。安装完整 JDK 8，并确认安装目录中存在 `bin/javac`。

### Main reactor reports the wrong Java version

确认已安装完整 JDK 21 或更新版本，并且 `javac -version` 可以正常执行。脚本会跳过无效的旧 `JAVA_HOME`，继续检查 `PATH` 和常见安装目录。

只有机器同时安装了多套同版本 JDK、自动发现结果又不是预期版本时，才需要用 `JAVA_AI_MAIN_JAVA_HOME` 或 `JAVA_AI_JDK8_HOME` 临时覆盖。它们是排障选项，不是正常运行前置步骤。

### External verification exits with code 2

Set an absolute `http` or `https` `JAVA_AI_EXTERNAL_BASE_URL`. The script intentionally refuses to infer an environment.
