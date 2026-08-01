# 本地构建与运行

## 运行主项目只需要 JDK 21

主项目使用 Java 21。安装包含 `java` 和 `javac` 的完整 JDK 21 后，可以直接运行 Maven Wrapper：

```bash
./mvnw verify
```

Windows PowerShell：

```powershell
.\mvnw.cmd verify
```

Spring Boot 服务默认使用 `demo` Profile。它们会关闭真实模型、数据库、身份平台和远程 Tool，因此启动检查不需要先配密钥或外部地址：

```bash
./mvnw -pl services/knowledge-service spring-boot:run
./mvnw -pl services/ticket-agent-service spring-boot:run
./mvnw -pl apps/customer-bff spring-boot:run
```

三条命令分别在独立终端执行。Windows 将 `./mvnw` 换成 `.\mvnw.cmd`。默认启动只验证应用组装和本地端口，不代表真实模型或跨服务业务链路已经通过。

## 真实模型演示只修改项目级 YAML

需要调用真实模型时，先把 `config/application.example.yml` 复制为 `config/application.yml`，再填写测试用 API Key。如果使用 OpenAI 兼容 Provider，再按实际协议修改 `base-url`、Chat 模型和 Embedding 模型。真实模型命令会显式读取这个本地文件。

本地文件已被 Git 忽略，无需额外配置模型环境变量。生产部署必须由公司密钥系统覆盖同一 Spring 配置键。

## 全仓检查才需要 JDK 8 和 Node.js

JDK 8 只用于编译 `integrations/jdk8-client`，Node.js 24+ 只用于前端与仓库约束检查。当需要验证整个配套项目时，再安装完整 JDK 8 和 Node.js，然后运行：

```bash
scripts/verify-unit.sh
```

Windows PowerShell：

```powershell
.\scripts\verify-unit.ps1
```

聚合脚本会自动寻找完整 JDK 21+ 和 JDK 8，并依次执行：

- Java 21 主 reactor；
- 隔离的框架 labs；
- Java 8 老客户端；
- Customer Web 的类型检查、测试和构建；
- 仓库协议与边界检查。

如果进程继承了失效的 Java 路径，脚本会忽略它并继续查找完整 JDK。macOS 的旧浏览器 JRE 不包含 `javac`，不会被选中。

## 分开运行各构建边界

不需要每次都跑全仓脚本。修改主服务时：

```bash
./mvnw verify
```

修改框架对照实验时：

```bash
./mvnw -f labs/pom.xml verify
```

修改老系统客户端时，在 JDK 8 终端中执行：

```bash
./mvnw -f integrations/jdk8-client/pom.xml verify
```

使用高于 21 的 JDK 时，主模块仍通过 `--release 21` 生成 Java 21 字节码。这只检查编译兼容性，CI 仍需要在真实 JDK 21 JVM 上运行。

## 外部环境另行验证

健康检查可以直接访问目标服务：

```bash
curl --fail https://test.example.com/actuator/health
```

返回 `status=UP` 只能证明该进程当前存活。真实模型、数据库、向量检索、对象存储、身份平台和业务链路要使用各自的集成测试和评测数据。

## 常见问题

### Java 8 构建提示没有编译器

当前机器只安装了 JRE。安装完整 JDK 8，并确认安装目录中存在 `bin/java` 和 `bin/javac`。

### 主项目提示 Java 版本不正确

执行 `java -version` 和 `javac -version`，确认两者都来自完整 JDK 21 或更新版本。如果 IDE 已选择 JDK 21，也可以直接在 IDE 中运行对应的 JUnit 测试或 Spring Boot 启动类。

### 默认启动后为什么不会调用真实模型

`demo` Profile 有意关闭外部依赖，用来检查应用组装和本地代码。需要真实模型结果时，运行 `LiveModelSmokeIT`，并显式加载根目录 `config/application.yml`。
