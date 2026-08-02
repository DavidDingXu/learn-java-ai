# 运行配置

## 默认配置可以验证什么

三个服务各自只有一份主 `application.yml`，不需要选择 `demo` 或 `production` Profile。默认配置面向本地学习：

| 服务 | 默认实现 | 可以验证的链路 |
|---|---|---|
| Knowledge Service | 固定身份、classpath 上下文、真实 Chat Provider | Prompt、结构化输出、流式回答、超时与错误映射 |
| Ticket Agent Service | 内存任务与审计、HTTP Knowledge Tool、内存写 Tool、固定身份 | Agent 决策、人工确认、幂等和跨服务调用 |
| Customer BFF | 固定身份、本地委托令牌、真实下游 HTTP | 会话、限流、SSE 转发和工单转交 |

Knowledge Service 和 Ticket Agent Service 会自动读取根目录 `config/application.yml`。第一次运行只需要替换其中的模型 API Key；使用 OpenAI 兼容服务时，再修改 `base-url` 和模型名。

```yaml
spring:
  ai:
    openai:
      api-key: replace-with-your-api-key
      base-url: https://api.openai.com/v1
      chat:
        model: gpt-4.1-mini
      embedding:
        model: text-embedding-3-small
```

把 Key 写在本地 YAML 中是为了减少学习步骤。真实值不能提交到 Git；公司测试、预生产和生产环境必须由 Secret Manager、Vault 或部署平台 Secret 覆盖同一个配置键。

## 启动三个服务

从项目根目录打开三个终端，分别运行：

```bash
./mvnw -pl services/knowledge-service spring-boot:run
./mvnw -pl services/ticket-agent-service spring-boot:run
./mvnw -pl apps/customer-bff spring-boot:run
```

Windows PowerShell 将 `./mvnw` 换为 `.\mvnw.cmd`，Maven 参数保持不变。默认端口是 8081、8082 和 8080。

默认身份为 `tenant-a / local-user / support`，HTTP 请求不需要 JWT。固定身份只用于降低本地联调门槛，不会伪造模型、数据库或下游响应。

## 只验证 Provider 接口

真实模型冒烟测试用于确认 Provider 协议、响应映射和基础业务校验，不要求启动三个服务，也不连接数据库：

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

该测试不验证数据库、ACL、向量检索、身份链路或端到端业务，不能替代完整 RAG 联调。

## 开启完整 RAG

完整 RAG 需要 PostgreSQL、`pgvector`、`pg_trgm`、Embedding Provider 和 Chat Provider。首次联调不需要 Redis、Kafka、MinIO 或身份平台。

修改 Knowledge Service 的 `application.yml`：

```yaml
java-ai:
  knowledge:
    mode: postgres-rag
    postgres:
      jdbc-url: jdbc:postgresql://localhost:5432/java_ai_knowledge
      username: java_ai_knowledge
      password: replace-with-your-database-password
```

然后按普通命令启动 Knowledge Service：

```bash
./mvnw -pl services/knowledge-service spring-boot:run
```

Flyway 会创建项目表结构，并尝试启用 `vector` 与 `pg_trgm` 扩展，因此首次运行的数据库账号需要相应权限。当前向量列是 `vector(1536)`，Embedding 模型必须返回 1536 维向量。上传、发布、索引和问答步骤见[知识文档导入](knowledge-ingestion.md)。

## 按需替换基础设施

本地实现和公司实现共享业务接口，不需要为了部署再维护一套 Profile。接入哪项基础设施，就切换对应配置并补齐所需参数。

### 公司鉴权

Knowledge Service 和 Ticket Agent Service：

```yaml
java-ai:
  security:
    mode: jwt
    jwt:
      issuer: https://identity.example.com
      audience: knowledge-service
      jwk-set-uri: https://identity.example.com/.well-known/jwks.json
```

Customer BFF 也将 `java-ai.security.mode` 改为 `jwt`，并使用自己的 audience。JWT 是仓库提供的一种适配器，不是 RAG 或 Agent 的固定技术前提；公司使用网关身份、Session 或内部权限服务时，可以实现同一身份接口。

### BFF 服务身份委托

本地默认使用短时的进程内委托令牌。接入公司 OAuth2 Token Exchange 后修改：

```yaml
java-ai:
  identity:
    delegation-mode: oauth2
    token-endpoint: https://identity.example.com/oauth2/token
    client-id: customer-bff
    client-secret: replace-with-secret-manager
```

### Ticket Agent 持久化与远程写 Tool

需要验证重启恢复和多实例并发时，将任务、确认和审计切换到独立 PostgreSQL：

```yaml
java-ai:
  persistence:
    mode: jdbc
    jdbc:
      url: jdbc:postgresql://localhost:5432/java_ai_ticket
      username: java_ai_ticket
      password: replace-with-your-database-password
```

有可调用的 Legacy Tool 后，再将 `java-ai.agent.write-tool.mode` 改为 `http`，填写 `legacy-tool-base-url`，并将 `downstream-token-mode` 替换为公司实际的服务令牌实现。没有真实下游时，内存写 Tool 更适合验证确认、幂等和审计规则。

### 多实例组件

Knowledge Service 的上传原文默认保存在本地文件系统，Customer BFF 的会话和限流默认保存在进程内。多实例部署前，必须分别替换为公司对象存储、共享会话和共享限流设施。

## 测试配置

`src/test/resources/application-test.yml` 和 `test` Profile 只供自动化测试使用。测试会装配确定性模型、本地 HTTP Fixture 或内存 Repository，这些实现不会进入本地联调和正式部署路径。

日常回归直接运行：

```bash
./mvnw verify
```

仓库中的 Shell 与 PowerShell 脚本用于聚合多个构建边界、选择本机 JDK 或生成专项报告。启动普通 Java 服务不依赖这些脚本。

## 验证边界

| 检查 | 入口 | 能证明什么 |
|---|---|---|
| 代码回归 | `./mvnw verify` | Java 构建、接口契约和业务规则 |
| Provider API | `LiveModelSmokeIT` | 当前模型连接、协议与响应映射 |
| 完整 RAG | `java-ai.knowledge.mode=postgres-rag` | 文档、Embedding、索引、ACL、检索、引用和回答 |
| 跨服务联调 | 默认启动三个服务 | 会话、身份委托、Knowledge Tool 和工单转交 |
| 目标环境验收 | 公司测试与发布流程 | 身份、数据库、真实 Tool、容量、迁移和回滚 |

这些检查解决的问题不同。单元测试通过不能证明真实模型效果，单次模型调用也不能证明数据库、权限和 Tool 副作用可以上线。
