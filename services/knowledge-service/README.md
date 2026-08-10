# Knowledge Service

企业知识文档、检索与 RAG 回答服务。该模块拥有文档业务版本、ACL、索引任务、分块、检索版本指针和引用映射，不拥有客户会话或工单业务状态。

## 主要边界

- `/api/v1/knowledge/documents`：文档上传、版本发布与 ACL。
- `/api/v1/knowledge/answers`：完整回答和 SSE 流式回答。
- `/internal/v1/knowledge/index-tasks`：受 `knowledge:index` 权限保护的索引任务入口。
- `/internal/v1/knowledge/retrieval/evaluations`：受 `knowledge:eval` 权限保护的检索评测入口。

公开契约以 [`contracts/openapi/knowledge-service-v1.yaml`](../../contracts/openapi/knowledge-service-v1.yaml) 为准。租户、主体和部门范围来自服务端身份适配器，不从业务请求体读取。默认使用固定本地身份；接入公司项目时，可以切换到 JWT 或替换为现有鉴权系统。

## 直接启动

在根目录 `config/application-default.yml` 填好模型 API Key，用 IDE 运行 `KnowledgeServiceApplication`。启动后访问 `http://localhost:8081/actuator/health`，再调用 `POST /api/v1/knowledge/answers`。

默认使用固定的 `tenant-a / local-user / support` 身份和 classpath 上下文，并调用根目录共享配置中的真实 Chat Provider，不要求生成 Token。完整写入链路见 [Knowledge Ingestion](../../docs/runbooks/knowledge-ingestion.md)。

本地完整 RAG 将 `java-ai.knowledge.mode` 改为 `postgres-rag`，填写 PostgreSQL 连接，并保持业务层 Embedding 模式为 `provider`。在根目录 YAML 中把 `spring.ai.model.embedding` 设为 `ollama`，即可使用本机免费的 `qwen3-embedding:4b`；改为 `openai` 则调用远程兼容 API。两种方式复用同一套业务代码。HTTP 请求仍使用固定身份。完整清单见[运行配置](../../docs/runbooks/runtime-configuration.md)。

`local-hash` 只用于排查上传、索引、ACL、pgvector、引用和回答流程，不提供语义质量证据。远程 Embedding 则使用 `provider` 模式。

生产部署还需接入对象存储，并验证迁移、容量、备份恢复和检索质量。
