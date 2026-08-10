# Knowledge Service

企业知识文档、检索与 RAG 回答服务。该模块拥有文档业务版本、ACL、索引任务、分块、检索版本指针和引用映射，不拥有客户会话或工单业务状态。

## 主要边界

- `/api/v1/knowledge/documents`：文档上传、版本发布与 ACL。
- `/api/v1/knowledge/answers`：完整回答和 SSE 流式回答。
- `/internal/v1/knowledge/index-tasks`：受 `knowledge:index` 权限保护的索引任务入口。
- `/internal/v1/knowledge/retrieval/evaluations`：受 `knowledge:eval` 权限保护的检索评测入口。

公开契约以 [`contracts/openapi/knowledge-service-v1.yaml`](../../contracts/openapi/knowledge-service-v1.yaml) 为准。租户、主体和部门范围来自服务端身份适配器，不从业务请求体读取。默认使用固定本地身份；接入公司项目时，可以切换到 JWT 或替换为现有鉴权系统。

## 直接启动

在项目根目录唯一的 `config/application-default.yml` 填好 Provider 和数据库配置，用 IDE 运行 `KnowledgeServiceApplication`。启动后打开阶段根目录的 `rag-learning-journey.http`。

默认使用固定的 `tenant-a / local-user / support` 身份，不要求生成 Token。

本地完整 RAG 保持 `java-ai.knowledge.embedding.mode: provider`，默认让 Chat 与 Embedding 共用同一个 OpenAI 兼容 API。若远程接口没有 Embedding，再在同一份本地 YAML 中只切换 Ollama。安装、手动启停和建库见 [RAG 本地准备](../../../../docs/runbooks/rag-prerequisites.md)。

`local-hash` 只用于排查上传、索引、ACL、pgvector、引用和回答流程，不提供语义质量证据。远程 Embedding 则使用 `provider` 模式。

生产部署还需接入对象存储，并验证迁移、容量、备份恢复和检索质量。
