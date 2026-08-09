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
    model:
      chat: openai
      embedding: openai
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

在 IDE 中按顺序运行 `KnowledgeServiceApplication`、`TicketAgentServiceApplication` 和 `CustomerBffApplication`。默认端口是 8081、8082 和 8080，macOS 与 Windows 的启动方式相同。

默认身份为 `tenant-a / local-user / support`，HTTP 请求不需要 JWT。固定身份只用于降低本地联调门槛，不会伪造模型、数据库或下游响应。

## 先确认 Provider 接口

只运行 `KnowledgeServiceApplication`，然后直接调用回答接口：

```bash
curl --fail-with-body \
  -H 'Content-Type: application/json' \
  -d '{"question":"退款通常多久到账？"}' \
  http://localhost:8081/api/v1/knowledge/answers
```

Windows PowerShell：

```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8081/api/v1/knowledge/answers" `
  -ContentType "application/json" `
  -Body '{"question":"退款通常多久到账？"}'
```

这一步确认 Chat Provider、响应映射和基础业务校验已经进入真实运行链路，但不验证数据库、ACL、向量检索或完整 RAG。

## 开启完整 RAG

`java-ai.knowledge.mode=postgres-rag` 开启完整 RAG。它需要 PostgreSQL、`pgvector`、`pg_trgm` 和 Chat Provider。

业务层有两种模式。`provider` 使用 Spring AI 当前选择的 Embedding Provider；`local-hash` 只用来排查上传、索引、ACL、pgvector、引用和回答流程，不提供语义质量证据。`provider` 下面既可以选择免费的本地 Ollama API，也可以选择远程 OpenAI 兼容 API。

使用 `ollama` 前，在 macOS 或 Windows 安装 Ollama 应用，并在应用中下载 `qwen3-embedding:4b`。模型约 2.5 GB，支持中文，应用会把本地接口提供在 `http://localhost:11434`。需要运行 RAG 时再打开 Ollama 即可，不必设置登录自启动。

修改根目录 `config/application.yml`：

```yaml
spring:
  ai:
    model:
      embedding: ollama
    ollama:
      base-url: http://localhost:11434
      embedding:
        model: qwen3-embedding:4b
        truncate: true
```

Knowledge Service 的 `application.yml` 中保持 `java-ai.knowledge.embedding.mode: provider`，再把 `java-ai.knowledge.mode` 改成 `postgres-rag` 并填写 PostgreSQL。Chat 仍使用原来的 OpenAI 兼容配置。修改后直接重新运行 `KnowledgeServiceApplication`。通用适配器会要求当前 Provider 返回 1536 维向量；模型、数量或维度不匹配时，索引任务会明确失败。

Flyway 会创建项目表结构，并尝试启用 `vector` 与 `pg_trgm` 扩展，因此首次运行的数据库账号需要相应权限。上传、发布、索引和问答步骤见[知识文档导入](knowledge-ingestion.md)。

切换到远程 Embedding 时，只需把根目录 `config/application.yml` 中的 `spring.ai.model.embedding` 改回 `openai` 并填写远程模型配置。业务层仍保持 `provider`，不需要修改 Java 代码。

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

## 运行边界

| 运行方式 | 能观察什么 |
|---|---|
| 只启动 Knowledge Service | 当前模型连接、回答映射与流式接口 |
| `postgres-rag` + `ollama` | 免费本地语义 Embedding、pgvector、ACL、引用和回答 |
| `postgres-rag` + `local-hash` | 文档、索引、ACL、pgvector、引用和回答流程，不证明语义质量 |
| `postgres-rag` + `provider` | 远程 Embedding 下的检索与质量评测 |
| 启动三个服务 | 会话、身份委托、Knowledge Tool 和工单转交 |
| 接入目标环境 | 身份、数据库、真实 Tool、容量、迁移和回滚 |

这些运行方式解决的问题不同。单次模型调用不能证明数据库、权限和 Tool 副作用可以上线。
