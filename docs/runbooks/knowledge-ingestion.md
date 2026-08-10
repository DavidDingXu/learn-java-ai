# 知识文档导入

Knowledge Service 的 RAG 写入链路包含上传原文、保存文档与版本、发布 ACL 和索引任务，以及 Worker 切分原文、生成 Embedding 并写入 pgvector。业务版本和检索版本分别管理；新版本索引完成前，已有文档继续使用上一版索引。

## 完整 RAG 需要准备什么

Knowledge Service 默认使用 classpath 上下文。本 Runbook 把上下文切换为 PostgreSQL 检索，需要 PostgreSQL 和一个同时支持 Chat、Embedding 的 OpenAI 兼容 Provider；固定本地身份与文件对象存储由项目提供。业务代码统一依赖 Spring AI 的 `EmbeddingModel`，不会感知具体 Provider。

先按 [RAG 本地准备](rag-prerequisites.md)创建允许使用 `vector` 与 `pg_trgm` 扩展的专用 PostgreSQL 数据库。应用启动时由 Flyway 按顺序执行 V1-V4：V1 创建文档、版本、ACL、任务和向量分块，V2 创建 trigram 索引，V3 增加发布审计字段，V4 创建检索版本指针。

Redis、Kafka、MinIO、Query Rewrite 和 Rerank 都不是首次联调的前置条件。原文默认写入本地文件目录；多实例部署时再替换为公司对象存储。

## 所有读者参数都在一份本地 YAML

模型、数据库与 RAG 参数集中在项目根目录唯一的 `config/application-default.yml`。Chat 和 Embedding 默认共用 API Key 与 Base URL：

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

java-ai:
  knowledge:
    mode: postgres-rag
    embedding:
      mode: provider
    retrieval:
      mode: hybrid
      lexical-search: true
      rewrite-query: false
      rerank: false
    indexing:
      scheduler-enabled: false
    object-store:
      local-root: ./var/knowledge-objects
    postgres:
      jdbc-url: jdbc:postgresql://localhost:5432/java_ai_knowledge
      username: java_ai_knowledge
      password: replace-with-your-database-password
```

该本地文件已被 Git 忽略。生产环境必须由公司密钥系统覆盖这些值；多实例部署也不应继续使用本地文件对象存储，需要替换为 S3 兼容实现。

`local-hash` 仍保留为故障排查选项。它不理解同义词或上下文，报告会显示 `deterministic-hash-v1-1536`，并将“可作为语义质量证据”标记为 `false`。

`java-ai.knowledge.embedding.mode` 保持 `provider`。当前 Flyway Schema 将向量列定义为 `vector(1536)`。如果统一远程接口不提供 Embedding，再按准备文档在同一份 YAML 中只把 Embedding 切换为 Ollama；如果更换为其他维度，应新建向量列或索引版本，完成回填和检索评测后再切换。

数据库账号需要对 Knowledge Schema 具备 Flyway 迁移和业务读写权限。正式环境通常把迁移账号与运行账号拆开，示例为了便于首次运行使用同一个连接。

## 本地联调使用固定身份

默认身份适配器固定返回 `tenant-a / local-user / support`，下面所有 HTTP 请求都不需要 Token。请求头或请求体即使夹带其他租户和用户，也不会覆盖这组身份。

发布 ACL 时必须包含 `support` 部门或 `local-user` 用户，否则后续检索会按权限规则返回空结果。这正好可以验证 ACL 是否真的在 TopK 之前生效。

## 直接运行完整 RAG 链路

在 IDEA 中打开阶段 03 的根 `pom.xml`，把 Working directory 设为阶段目录，先运行 `KnowledgeServiceApplication`。看到 Health 为 `UP` 后，打开 `rag-learning-journey.http`，按文件顺序执行带名称的真实 HTTP 请求：

- 上传并发布固定退款政策；
- 上传一份 `support` 无权读取的财务文档；
- 执行两条索引任务；
- 并列运行向量检索和混合检索；
- 检查越权文档没有进入 TopK；
- 生成带引用的回答；
- 回放 Golden Set 并写出 Markdown 报告。

最后运行 `EvalRunner` 的检索评测入口，报告位于 `var/reports`。模型和数据库配置都来自唯一的 `application-default.yml`。先看报告中的 Embedding 名称和“可作为语义质量证据”，再解释 Recall、HitRate 和 MRR。

`.http` 文件使用稳定文档 ID，默认面向干净的专用数据库。若这些 ID 已存在，接口会返回冲突，不会覆盖旧数据或偷偷清库。

下面的 HTTP 与 SQL 用来逐层解释 `.http` 文件中的协议和状态；连续实验与正文使用的是同一组真实接口，不存在另一套教学包装逻辑。

确认 `/actuator/health` 返回 `UP` 后再上传文档。Health 不会替你验证 Embedding 质量或数据库容量。

## 上传草稿版本

`metadata` 是 JSON Part，`file` 是 Markdown 或纯文本文件，单文件上限 5 MiB：

```bash
curl --fail-with-body \
  -F 'metadata={"title":"退款政策","expectedRevision":0};type=application/json' \
  -F 'file=@datasets/knowledge/refund-policy-chunking-v1.md;type=text/markdown' \
  http://localhost:8081/api/v1/knowledge/documents/refund-policy/versions
```

Windows PowerShell 使用 `curl.exe`，避免把 `curl` 解析成旧版 PowerShell 的别名：

```powershell
curl.exe --fail-with-body `
  -F 'metadata={"title":"退款政策","expectedRevision":0};type=application/json' `
  -F 'file=@datasets/knowledge/refund-policy-chunking-v1.md;type=text/markdown' `
  http://localhost:8081/api/v1/knowledge/documents/refund-policy/versions
```

首次上传返回 `201`、版本号 `1` 和 revision `1`。同一文档再次上传时，`expectedRevision` 必须使用上一次写操作返回的 revision；内容哈希重复或 revision 过期返回 `409`。

## 发布并创建索引任务

把 `effectiveFrom` 设置为当前时间或过去时间。当前状态模型不提前激活未来版本；如果业务需要定时发布，由调度系统在生效时间调用此接口。

```bash
curl --fail-with-body \
  -H 'Content-Type: application/json' \
  -d '{
    "expectedRevision": 1,
    "effectiveFrom": "2026-07-17T02:00:00Z",
    "effectiveUntil": null,
    "acl": [
      {"subjectType": "DEPARTMENT", "subjectId": "support"},
      {"subjectType": "USER", "subjectId": "editor-42"}
    ]
  }' \
  http://localhost:8081/api/v1/knowledge/documents/refund-policy/versions/1/publish
```

Windows PowerShell：

```powershell
$publishBody = @{
  expectedRevision = 1
  effectiveFrom = "2026-07-17T02:00:00Z"
  effectiveUntil = $null
  acl = @(
    @{ subjectType = "DEPARTMENT"; subjectId = "support" }
    @{ subjectType = "USER"; subjectId = "editor-42" }
  )
} | ConvertTo-Json -Depth 4

Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8081/api/v1/knowledge/documents/refund-policy/versions/1/publish" `
  -ContentType "application/json" `
  -Body $publishBody
```

发布返回 `202` 和 `indexTaskId`。版本状态、整份文档的 READ ACL 与 `PENDING` 索引任务在同一个数据库事务中提交；其中任何一步失败都会回滚。

发布成功表示业务版本已经切换，不表示新版本索引已经可查询。如果这是一份已有索引的文档，读取请求会继续命中上一版索引；如果是首次发布，因为没有旧索引可用，新分块写入并激活前不会返回这份文档的内容。

## 运行索引 Worker

手动接口从本地身份组件读取 `tenant-a`，每次至多领取该租户的一个到期任务：

```bash
curl --fail-with-body \
  -X POST \
  http://localhost:8081/internal/v1/knowledge/index-tasks/run-once
```

Windows PowerShell：

```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8081/internal/v1/knowledge/index-tasks/run-once"
```

返回值为 `SUCCEEDED`、`FAILED`、`IDLE` 或 `LOST_LEASE`。`LOST_LEASE` 表示当前 Worker 已不再拥有该任务，它不会再使用旧租约写入失败状态。普通失败任务按 `java-ai.knowledge.indexing.retry-delay` 重试，达到 `maximum-attempts` 后进入 `DEAD`。

Worker 先在数据库事务外读取原文、切分并调用 Embedding 服务，期间每隔约一个租期的三分之一续租。续租、完成和失败写入都会校验任务 ID、Worker、`leaseAttempt` 和当前租约。进入短写事务后，sink 还会校验租户、文档、版本和任务类型；只有租约仍有效才会写入目标版本。新分块和 `document_search_version` 指针在同一个事务中提交。租约已丢失、分块写入失败、目标版本已被替代或已过期时，事务回滚，原检索版本不变。

## 验证写入结果

```sql
select task_id, status, attempts, error_code
from index_task
order by created_at desc;

select document_id, version_number, chunk_id, ordinal, embedding_model
from document_chunk
where tenant_id = 'tenant-a' and document_id = 'refund-policy'
order by ordinal;

select document_id, version_number, activated_at
from document_search_version
where tenant_id = 'tenant-a' and document_id = 'refund-policy';
```

读取链路先确认文档存在当前有效的 `PUBLISHED` 业务版本，再从 `document_search_version` 指向的已完成索引中取分块，并在 TopK 前应用租户、用户和部门 ACL。ACL 属于整份文档，本次发布写入的新 ACL 会立即约束上一版索引。替换索引期间，返回结果里的 `documentVersion` 可能暂时仍是上一版。

## 用真实检索结果生成回答

索引任务成功后，再调用完整回答接口：

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

响应必须同时检查回答和 `citations`。只有回答有内容，却没有引用到刚才发布的文档版本，不能算完整 RAG 跑通。接着把发布 ACL 改成不包含 `support` 和 `local-user` 的主体，再用同一问题验证空召回或拒答，才能确认权限过滤没有被最终回答掩盖。

## 生产环境替换项

默认 `LocalFileDocumentObjectStore` 便于单机运行。多实例部署必须换成 S3 兼容对象存储，并保留相同的租户隔离 key、不可变原文和读取接口。上线前还要验证 PostgreSQL/pgvector 扩展版本、Embedding 维度、索引构建时间、任务积压告警、对象与元数据孤儿清理、备份恢复及容量上限。

正式部署还要把固定身份改为公司现有鉴权适配器。JWT 只是仓库提供的一种实现，不是 RAG 的固定技术前提。

监控中应同时展示当前业务版本和 `document_search_version`。两者长时间不一致通常意味着索引重试、Embedding 服务异常或任务已经进入 `DEAD`。只要当前业务版本仍在有效期内且已有上一版索引，读取仍可用，但内容还是上一版；首次发布和业务版本过期不具备这个回退条件。
