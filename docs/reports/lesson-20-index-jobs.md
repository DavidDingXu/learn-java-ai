# 索引任务恢复验证

Status: IMPLEMENTED_WITH_LOCAL_TESTS_AND_EXTERNAL_PROFILE

- State machine: `IndexTask`
- Worker use case: `IndexTaskWorker`
- Queue port: `IndexTaskQueue`
- PostgreSQL queue: `PostgresIndexTaskQueue`
- Indexer port: `DocumentVersionIndexer`
- Indexer: `DocumentVersionIndexingService`
- Object text source: `StoredDocumentVersionTextSource`
- pgvector sink: `PgVectorKnowledgeChunkIndexSink`
- Search version: `document_search_version`
- Runtime assembly: `KnowledgeIngestionConfiguration`
- PostgreSQL claim SQL: `PostgresIndexTaskClaimQuery`
- Schema source: Flyway V1-V4

## 已验证

- 任务状态为 `PENDING`、`RUNNING`、`RETRY_WAIT`、`SUCCEEDED` 和 `DEAD`，尝试次数在成功 claim 时递增。
- Claim 写入 worker、有限租约和更新时间；非租约持有者或租约已过期的 worker 不能完成、失败任务。
- 失败使用稳定错误码，未耗尽次数时进入带 `nextAttemptAt` 的等待状态，达到最大次数后进入 `DEAD`。
- 已过期的 `RUNNING` 任务可回收给其他 worker，未到期的重试任务不能提前领取。
- Worker 明确区分无任务、成功、失败和租约丢失；租约丢失后不会再用旧代次写入失败状态。
- PostgreSQL Claim SQL 使用 `FOR UPDATE SKIP LOCKED`、一次只选一条到期任务，并在同一语句中更新租约和返回任务。
- PostgreSQL Queue 执行 claim、renew、complete 和 fail SQL；续租与终态操作同时校验 worker、租约尝试号和租约有效期，更新数不是 1 时拒绝提交。
- Worker 在长索引执行期间每隔约一个租期的三分之一续租；续租线程随 Spring Bean 销毁。
- pgvector sink 的短写事务首先用任务 ID、租户、文档、版本、任务类型、`leaseAttempt` 和 `lease_until` 执行 `SELECT ... FOR UPDATE`。失去租约的旧 Worker 在删除分块前就会被拒绝。
- 原文读取、切分和 Embedding 调用在进入 sink 前完成，不会在外部调用期间持有索引任务行锁。
- 崩溃后过期的 `RUNNING` 任务在尝试预算耗尽时原子转为 `DEAD`，不会无限重领。
- `DocumentVersionIndexingService` 对指定版本执行确定性切分，按配置上限分批 Embedding，并通过 `replaceVersion` 端口整体替换索引，重复执行不会生成新的 chunk ID。
- Spring AI Embedding 适配器校验批量结果数量、Provider 索引、模型名和向量维度；跨批次模型名不一致时拒绝写入。
- Flyway 对同一租户、文档版本和任务类型设置唯一约束，避免重复创建等价索引任务。
- 发布事务同时写入版本状态、发布人、ACL 和 `PENDING` 索引任务，任何一步失败都会回滚。
- Worker 读取当前 `PUBLISHED` 版本；pgvector sink 在提交前再次校验目标仍是当前有效发布版本，版本已被替代时会回滚，迟到任务不能切换检索版本。
- pgvector sink 在一个 JDBC 事务中删除当前版本旧分块、批量写入新分块并切换 `document_search_version`，其中任何一步失败都会回滚。
- 发布新业务版本不会立即改变检索指针。新分块写入并激活前继续查询上一版，任务重试不会造成已有文档暂时无结果。
- 向量与关键词查询都按 `document_search_version` 读取分块，同时要求文档存在当前有效的 `PUBLISHED` 业务版本；业务有效期和索引就绪状态没有混成同一个字段。
- Spring 装配提供全局定时轮询和受 `knowledge:index` scope 保护的租户级手动触发，两者复用同一个 Worker。

## 本地验证

```bash
./mvnw -pl services/knowledge-service \
  -Dtest=KnowledgeSchemaMigrationContractTest,IndexTaskTest,IndexTaskWorkerTest,IndexTaskControllerTest,PostgresIndexTaskClaimQueryTest,PostgresIndexTaskMutationQueryTest,PostgresIndexTaskQueueTest,DocumentVersionIndexingServiceTest,RagIngestionVerticalSliceTest,PgVectorKnowledgeChunkIndexSinkTest,PgVectorSearchQueryTest,PostgresLexicalSearchQueryTest,StoredDocumentVersionTextSourceTest,KnowledgeIngestionConfigurationTest \
  test
```

覆盖状态迁移、租约所有权、失败重试、死信状态、Worker 编排、JDBC Queue、发布事务、对象回读、检索指针、pgvector 参数与回滚、Spring 装配和完整写入链。

## 证据边界

`external-integration` profile 会在指定 PostgreSQL/pgvector 测试库执行 Flyway、sink 批量写入、上一版继续服务、新索引切换和 ACL 检索。本地测试不依赖容器，但公司对象存储、多 Worker 抢占、宕机恢复、任务积压和容量仍需在目标环境测试。首次发布没有上一版可回退；索引任务长期失败时，只要当前业务版本仍有效，读取会继续返回旧版，因此必须监控业务版本与检索版本不一致的持续时间。
