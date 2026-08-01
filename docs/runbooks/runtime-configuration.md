# 运行配置

## 配置边界

Knowledge Service、Ticket Agent Service 和 Customer BFF 各自只保留一份主 `application.yml`。每份文件都包含通用参数、默认 `demo` 文档和显式 `production` 文档。项目根目录的 `config/application.yml` 只保存公开模型参数和不可用的占位 Key，真实模型测试直接读取它。

配置遵循以下规则：

1. 普通启动使用默认 `demo`，不需要数据库、身份平台、模型或下游服务。
2. 真实模型测试显式读取根目录 `config/application.yml`，使用 OpenAI 时只填写 API Key，运行后不得提交真实值。
3. 数据库、身份、Token Exchange 和下游地址的配置结构直接保留在各服务 `application.yml` 的 `production` 文档中。
4. 普通测试使用 `src/test/resources/application-test.yml`，不访问外部网络。
5. 仓库中只保留不可用的密钥占位值；生产密钥必须由密钥系统或部署平台覆盖。

## 启动本地应用

从项目根目录分别启动：

```bash
./mvnw -pl services/knowledge-service spring-boot:run
./mvnw -pl services/ticket-agent-service spring-boot:run
./mvnw -pl apps/customer-bff spring-boot:run
```

Windows 使用相同的 Maven 参数，把入口换成 `mvnw.cmd`。默认端口为 8081、8082 和 8080。

`demo` 路径只用于启动应用、检查 health 和阅读代码结构。被关闭的外部能力会明确返回不可用，不会返回固定答案或伪造下游成功。

## 运行真实模型测试

在 `config/application.yml` 中填写 `spring.ai.openai.api-key`。OpenAI 地址、Chat 模型和 Embedding 模型已有演示默认值；使用其他 OpenAI 兼容服务时，再修改同一文件中的地址和模型名。

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

`config/application.yml` 只为减少本地演示步骤而存在。仓库版本只包含占位 Key；填入真实值后必须保持在本机并在提交前恢复。

## 生产配置

`production` 只选择生产配置段，不会把占位值变成可用连接。启动前需要准备以下配置：

- Knowledge Service：`spring.ai.openai.*`、`java-ai.knowledge.postgres.*` 和 `java-ai.security.jwt.*`。其中 PostgreSQL 必须安装 pgvector 与 `pg_trgm`；Embedding 模型输出维度必须与当前 `vector(1536)` Schema 一致。
- Ticket Agent Service：`spring.datasource.*`、`spring.ai.openai.*` 和 `java-ai.security.jwt.*`。任务、确认、执行状态与审计使用独立 PostgreSQL。
- Customer BFF：`java-ai.security.customer-jwt.*`、`java-ai.identity.*` 和 `java-ai.downstream.*`。Token Exchange 客户端必须能申请面向 Knowledge 和 Ticket 的最小权限令牌。

文件中的 `replace-with-secret-manager` 和 `*.example.com` 都是不可用占位值。个人隔离环境可以临时修改各服务 `application.yml` 的 `production` 配置段；正式部署应由平台在不改变配置键的前提下覆盖真实值。生产 API Key、数据库密码、JWT 材料和客户端密钥不能保存在 Git、镜像层或测试报告中。

分别启动三个应用：

```bash
./mvnw -pl services/knowledge-service \
  -Dspring-boot.run.profiles=production spring-boot:run

./mvnw -pl services/ticket-agent-service \
  -Dspring-boot.run.profiles=production spring-boot:run

./mvnw -pl apps/customer-bff \
  -Dspring-boot.run.profiles=production spring-boot:run
```

Windows 使用 `mvnw.cmd`，其余参数不变。通常先启动数据库和身份平台，再依次启动 Knowledge Service、Ticket Agent Service 和 Customer BFF。

根目录 `config/application.yml` 只在命令显式传入 `spring.config.additional-location` 时用于真实模型专项测试，不会自动覆盖上述三个服务。完整联调不能只填写这一份文件。

三个应用会在创建业务 Bean 前校验生产必填项。空值、`replace-with-secret-manager`、`*.example.com`、非法 URL、错误数据库协议或冲突的 JWT 验签来源都会阻止启动。错误只包含配置键和要求，不包含密钥值。

Ticket Agent 的远程 Tool 在生产配置中仍然关闭，开启前需要实现公司的短时服务令牌适配器。Knowledge Service 的上传原文仍使用本地文件存储，Customer BFF 的会话和限流仍使用进程内实现；多实例部署需要分别替换为公司对象存储、共享会话和共享限流设施。

## 测试隔离

`src/test/resources/application-test.yml` 关闭模型和外部连接，并为需要状态的测试选择内存适配器。Spring Boot 上下文测试显式使用 `@ActiveProfiles("test")`；模型协议测试使用本地 HTTP Fixture。

日常回归直接运行：

```bash
./mvnw verify
```

仓库中的 Shell 与 PowerShell 脚本只用于聚合多个构建边界、自动选择本机 JDK 或生成专项报告。它们不是启动 Java 服务的必要入口。

## 验证范围

| 检查 | 入口 | 覆盖范围 |
|---|---|---|
| 代码回归 | `./mvnw verify` | Java 构建、接口契约和业务规则 |
| Provider API | `LiveModelSmokeIT` | Provider 协议、模型响应和业务映射 |
| 外部集成 | 目标测试环境 | 数据库、身份、跨服务和下游回执 |
| 生产验收 | 容量、可用性、迁移和回滚报告 | 目标环境是否满足上线条件 |

这些检查使用同一组配置路径，但覆盖范围不同，不能用其中一项代替另一项。
