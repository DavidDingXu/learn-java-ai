# 知识文档导入

Knowledge Service 的 RAG 写入链路包含上传原文、保存文档与版本、发布 ACL 和索引任务，以及 Worker 切分原文、生成 Embedding 并写入 pgvector。业务版本和检索版本分别管理；新版本索引完成前，已有文档继续使用上一版索引。

## 这条链路不属于默认演示

默认 `demo` Profile 会关闭数据库、文档写入、真实模型和 JWT，用于直接启动应用与运行确定性测试。本 Runbook 说明的是目标环境写入链路，需要真实 PostgreSQL/pgvector、对象存储、Embedding Provider 和身份平台。

准备一个已安装 `vector` 与 `pg_trgm` 扩展的 PostgreSQL 数据库，以及支持 Embedding 的 OpenAI 兼容接口。若同时验证知识问答，该接口还需支持 Chat。应用启动时由 Flyway 按顺序执行 V1-V4：V1 创建文档、版本、ACL、任务和向量分块，V2 创建 trigram 索引，V3 增加发布审计字段，V4 创建检索版本指针。

## 所有运行参数都在 `application.yml`

Knowledge Service 的 `application.yml` 已经给出 `production` 配置结构。在隔离的测试环境中，把占位值替换为实际地址与账号：

```yaml
spring:
  ai:
    openai:
      api-key: replace-with-secret-manager
      base-url: https://api.openai.com/v1
      embedding:
        model: text-embedding-3-small

java-ai:
  security:
    jwt:
      issuer: https://identity.example.com
      audience: knowledge-service
      jwk-set-uri: https://identity.example.com/.well-known/jwks.json
      allowed-actors: customer-bff,ticket-agent-service,knowledge-admin-service
  knowledge:
    object-store:
      local-root: ./var/knowledge-objects
    postgres:
      jdbc-url: jdbc:postgresql://database.example.com:5432/java_ai_knowledge
      username: java_ai_knowledge
      password: replace-with-secret-manager
```

为了演示配置位置，YAML 中保留了占位密码和 API Key。真实密钥不能提交到 Git；生产环境必须由公司密钥系统覆盖这些值。多实例部署也不应继续使用本地文件对象存储，需要替换为 S3 兼容实现。

当前 Flyway Schema 将向量列定义为 `vector(1536)`，所选 Embedding 模型必须输出 1536 维向量。如果更换为其他维度，应新建向量列或索引版本，完成回填和检索评测后再切换，不是只修改模型名称。

数据库账号需要对 Knowledge Schema 具备 Flyway 迁移和业务读写权限。正式环境通常把迁移账号与运行账号拆开，示例为了便于首次运行使用同一个连接。

## 写入令牌的最小 Claim

上传和发布使用 `knowledge:write` scope，手动执行一次 Worker 使用 `knowledge:index` scope。JWT 还必须包含：

- `aud`：包含 `knowledge-service`；
- `tenantId`：文档所属租户；
- `sub`：执行上传或发布的编辑人；
- `act.sub`：必须命中服务端 `allowed-actors` 白名单。

接口不会接收请求头或请求体中的租户、编辑人字段。

从 IdP 或 Token Exchange 获取两枚短时令牌：一枚只带 `knowledge:write`，另一枚只带 `knowledge:index`。后续命令中的 `<WRITE_TOKEN>` 和 `<INDEX_TOKEN>` 分别替换为这两枚令牌。不要把令牌写进脚本、命令历史或仓库文件。

## 启动 Knowledge Service

下面的步骤使用手动接口执行一条索引任务，因此先在 `production` 配置段关闭自动调度：

```yaml
java-ai:
  knowledge:
    indexing:
      scheduler-enabled: false
```

如果保留自动调度，请跳过后面的手动 `run-once`，否则任务可能已经被调度器处理。

```bash
./mvnw -pl services/knowledge-service \
  -Dspring-boot.run.profiles=production \
  spring-boot:run
```

确认 `/actuator/health` 返回 `UP` 后再上传文档。Health 不会替你验证 Embedding 质量或数据库容量。

## 上传草稿版本

`metadata` 是 JSON Part，`file` 是 Markdown 或纯文本文件，单文件上限 5 MiB：

```bash
curl --fail-with-body \
  -H "Authorization: Bearer <WRITE_TOKEN>" \
  -F 'metadata={"title":"退款政策","expectedRevision":0};type=application/json' \
  -F 'file=@datasets/knowledge/refund-policy-chunking-v1.md;type=text/markdown' \
  http://localhost:8081/api/v1/knowledge/documents/refund-policy/versions
```

Windows PowerShell 使用 `curl.exe`，避免把 `curl` 解析成旧版 PowerShell 的别名：

```powershell
curl.exe --fail-with-body `
  -H "Authorization: Bearer <WRITE_TOKEN>" `
  -F 'metadata={"title":"退款政策","expectedRevision":0};type=application/json' `
  -F 'file=@datasets/knowledge/refund-policy-chunking-v1.md;type=text/markdown' `
  http://localhost:8081/api/v1/knowledge/documents/refund-policy/versions
```

首次上传返回 `201`、版本号 `1` 和 revision `1`。同一文档再次上传时，`expectedRevision` 必须使用上一次写操作返回的 revision；内容哈希重复或 revision 过期返回 `409`。

## 发布并创建索引任务

把 `effectiveFrom` 设置为当前时间或过去时间。当前状态模型不提前激活未来版本；如果业务需要定时发布，由调度系统在生效时间调用此接口。

```bash
curl --fail-with-body \
  -H "Authorization: Bearer <WRITE_TOKEN>" \
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
  -Headers @{ Authorization = "Bearer <WRITE_TOKEN>" } `
  -ContentType "application/json" `
  -Body $publishBody
```

发布返回 `202` 和 `indexTaskId`。版本状态、整份文档的 READ ACL 与 `PENDING` 索引任务在同一个数据库事务中提交；其中任何一步失败都会回滚。

发布成功表示业务版本已经切换，不表示新版本索引已经可查询。如果这是一份已有索引的文档，读取请求会继续命中上一版索引；如果是首次发布，因为没有旧索引可用，新分块写入并激活前不会返回这份文档的内容。

## 运行索引 Worker

手动接口从已验证 JWT 中读取 `tenantId`，每次至多领取该租户的一个到期任务：

```bash
curl --fail-with-body \
  -X POST \
  -H "Authorization: Bearer <INDEX_TOKEN>" \
  http://localhost:8081/internal/v1/knowledge/index-tasks/run-once
```

Windows PowerShell：

```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8081/internal/v1/knowledge/index-tasks/run-once" `
  -Headers @{ Authorization = "Bearer <INDEX_TOKEN>" }
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

读取链路先确认文档存在当前有效的 `PUBLISHED` 业务版本，再从 `document_search_version` 指向的已完成索引中取分块，并在 TopK 前应用 JWT 租户、用户和部门 ACL。ACL 属于整份文档，本次发布写入的新 ACL 会立即约束上一版索引。替换索引期间，返回结果里的 `documentVersion` 可能暂时仍是上一版。

## 生产环境替换项

默认 `LocalFileDocumentObjectStore` 便于单机运行。多实例部署必须换成 S3 兼容对象存储，并保留相同的租户隔离 key、不可变原文和读取接口。上线前还要验证 PostgreSQL/pgvector 扩展版本、Embedding 维度、索引构建时间、任务积压告警、对象与元数据孤儿清理、备份恢复及容量上限。

监控中应同时展示当前业务版本和 `document_search_version`。两者长时间不一致通常意味着索引重试、Embedding 服务异常或任务已经进入 `DEAD`。只要当前业务版本仍在有效期内且已有上一版索引，读取仍可用，但内容还是上一版；首次发布和业务版本过期不具备这个回退条件。
