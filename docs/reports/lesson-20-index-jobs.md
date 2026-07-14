# Lesson 20 Index Job Recovery Evidence

Status: VERIFIED_LOCAL_RECOVERABLE_TASK_CONTRACT

- State machine: `IndexTask`
- Worker use case: `IndexTaskWorker`
- Queue port: `IndexTaskQueue`
- PostgreSQL queue: `PostgresIndexTaskQueue`
- Indexer port: `DocumentVersionIndexer`
- Deterministic indexer: `DocumentVersionIndexingService`
- PostgreSQL claim SQL: `PostgresIndexTaskClaimQuery`
- Schema source: `db/migration/V1__knowledge_platform.sql`

## Verified

- 任务状态为 `PENDING`、`RUNNING`、`RETRY_WAIT`、`SUCCEEDED` 和 `DEAD`，尝试次数在成功 claim 时递增。
- Claim 写入 worker、有限租约和更新时间；非租约持有者或租约已过期的 worker 不能完成、失败任务。
- 失败使用稳定错误码，未耗尽次数时进入带 `nextAttemptAt` 的等待状态，达到最大次数后进入 `DEAD`。
- 已过期的 `RUNNING` 任务可回收给其他 worker，未到期的重试任务不能提前领取。
- Worker 明确区分无任务、成功和失败；只有索引完成后才确认任务，索引异常会交给队列执行重试或终止策略。
- PostgreSQL Claim SQL 使用 `FOR UPDATE SKIP LOCKED`、一次只选一条到期任务，并在同一语句中更新租约和返回任务。
- PostgreSQL Queue 执行 claim、complete 和 fail SQL；完成与失败操作同时校验 worker、租约尝试号和租约有效期，更新数不是 1 时拒绝提交。
- 崩溃后过期的 `RUNNING` 任务在尝试预算耗尽时原子转为 `DEAD`，不会无限重领。
- `DocumentVersionIndexingService` 对指定版本执行确定性切分，按配置上限分批 Embedding，并通过 `replaceVersion` 端口整体替换索引，重复执行不会生成新的 chunk ID。
- Spring AI Embedding 适配器校验批量结果数量、Provider 索引、模型名和向量维度；跨批次模型名不一致时拒绝写入。
- Flyway 对同一租户、文档版本和任务类型设置唯一约束，避免重复创建等价索引任务。

## Local Verification

```bash
./mvnw -pl services/knowledge-service \
  -Dtest=KnowledgeSchemaMigrationContractTest,IndexTaskTest,IndexTaskWorkerTest,PostgresIndexTaskClaimQueryTest,PostgresIndexTaskMutationQueryTest,PostgresIndexTaskQueueTest,DocumentVersionIndexingServiceTest \
  test
```

覆盖状态迁移、租约所有权、失败重试、死信状态、Worker 编排、JDBC Queue、PostgreSQL SQL 合同和确定性索引替换。

## Evidence Boundary

当前证据覆盖领域状态机、Worker、JDBC Queue、租约 SQL 和确定性索引应用服务。文档内容源与 chunk 批量写入通过端口隔离，生产对象存储和公司数据库适配器仍需按环境实现；项目也没有在多进程下制造抢占、宕机和租约过期。因此不能声称真实多 Worker 并发、对象存储读取或容量已经验收。
