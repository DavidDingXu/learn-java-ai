# Runtime Configuration

## Configuration Boundaries

Knowledge Service、Ticket Agent Service 和 Customer BFF 各自只保留一份主 `application.yml`。每份文件都包含通用参数、默认 `demo` 文档和显式 `production` 文档。项目根目录还有可提交的 `config/application.example.yml`；真实模型测试读取由它复制出来的本地 `config/application.yml`。

配置遵循以下规则：

1. 普通启动使用默认 `demo`，不需要数据库、身份平台、模型或下游服务。
2. 真实模型测试显式读取 Git 忽略的 `config/application.yml`，使用 OpenAI 时只填写 API Key。
3. 数据库、身份、Token Exchange 和下游地址的配置结构直接保留在各服务 `application.yml` 的 `production` 文档中。
4. 普通测试使用 `src/test/resources/application-test.yml`，不访问外部网络。
5. 仓库中只保留不可用的密钥占位值；生产密钥必须由密钥系统或部署平台覆盖。

## Start The Demo Applications

从项目根目录分别启动：

```bash
./mvnw -pl services/knowledge-service spring-boot:run
./mvnw -pl services/ticket-agent-service spring-boot:run
./mvnw -pl apps/customer-bff spring-boot:run
```

Windows 使用相同的 Maven 参数，把入口换成 `mvnw.cmd`。默认端口为 8081、8082 和 8080。

`demo` 路径只用于启动应用、检查 health 和阅读代码结构。被关闭的外部能力会明确返回不可用，不会返回固定答案或伪造下游成功。

## Run A Real Model Test

先复制演示配置，再填写 `spring.ai.openai.api-key`：

```bash
cp config/application.example.yml config/application.yml
```

Windows PowerShell 使用 `Copy-Item config/application.example.yml config/application.yml`。OpenAI 地址、Chat 模型和 Embedding 模型已有演示默认值；使用其他 OpenAI 兼容服务时，再修改同一文件中的地址和模型名。

直接运行指定的 Java 集成测试：

```bash
./mvnw \
  -pl services/knowledge-service \
  -Dtest=LiveModelSmokeIT \
  -Dspring.config.additional-location=file:../../config/application.yml \
  -Djava-ai.smoke.report-path=target/live-model-smoke.md \
  test
```

Windows PowerShell：

```powershell
.\mvnw.cmd `
  -pl services/knowledge-service `
  -Dtest=LiveModelSmokeIT `
  -Dspring.config.additional-location=file:../../config/application.yml `
  -Djava-ai.smoke.report-path=target/live-model-smoke.md `
  test
```

该测试只检查模型协议、响应映射和业务校验，并把脱敏结果写入 `target/live-model-smoke.md`。它不验证数据库、身份链路、RAG 质量或端到端业务。

`config/application.yml` 只为减少本地演示步骤而存在，并已被 Git 忽略。

## Production Configuration

各服务 `application.yml` 的 `production` 文档列出真实适配器需要的完整 Spring 配置路径：

- Knowledge Service：PostgreSQL/pgvector、Flyway、Chat/Embedding 模型、JWT 验签和索引任务。
- Ticket Agent Service：PostgreSQL/Flyway、Chat 模型、JWT 验签、Knowledge 和 Legacy Tool 地址。
- Customer BFF：客户 JWT、Token Exchange、Knowledge 和 Ticket 地址。

文件中的 `replace-with-secret-manager` 和 `*.example.com` 都是不可用占位值。部署时，平台在不改变配置键的前提下覆盖真实值。生产 API Key、数据库密码、JWT 材料和客户端密钥不能保存在 Git、镜像层或测试报告中。

Ticket Agent 的远程 Tool 在生产示例中仍然默认关闭。接入公司的短时服务令牌适配器之前，不应为了跑通演示而把开发 HMAC 签发器带进生产。

## Test Isolation

`src/test/resources/application-test.yml` 关闭模型和外部连接，并为需要状态的测试选择内存适配器。Spring Boot 上下文测试显式使用 `@ActiveProfiles("test")`；模型协议测试使用本地 HTTP Fixture。

日常回归直接运行：

```bash
./mvnw verify
```

仓库中的 Shell 与 PowerShell 脚本只用于聚合多个构建边界、自动选择本机 JDK 或生成专项报告。它们不是启动 Java 服务的必要入口。

## Verification Scope

| Check | Entry point | Covered scope |
|---|---|---|
| Code regression | `./mvnw verify` | Java 构建、接口契约和业务规则 |
| Provider API | `LiveModelSmokeIT` | Provider 协议、模型响应和业务映射 |
| Integration | 目标测试环境 | 数据库、身份、跨服务和下游回执 |
| Production acceptance | 容量、可用性、迁移和回滚报告 | 目标环境是否满足上线条件 |

这些检查使用同一组配置路径，但覆盖范围不同，不能用其中一项代替另一项。
