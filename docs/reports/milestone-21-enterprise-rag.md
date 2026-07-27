# Milestone 21 Enterprise RAG

Status: IMPLEMENTED_WITH_LOCAL_TESTS_AND_EXTERNAL_PROFILE

## Included Capabilities

- 受 `knowledge:write` 保护的文档上传与发布接口，租户和操作人来自 JWT。
- `JdbcKnowledgeDocumentRepository` 保存文档、不可变版本和 revision；Flyway V1-V4 管理文档、ACL、索引任务、分块、发布审计与检索版本指针。
- 原文通过 `DocumentObjectStore` 隔离存储实现，默认适配器写入本地文件目录。
- 发布事务同时更新业务版本、文档级 READ ACL 和 `PENDING` 索引任务。
- Worker 使用租约领取任务，长索引期间按租约代次续租，读取原文后完成确定性切分和批量 Embedding，再由 pgvector sink 写入分块。
- sink 在写事务开始时校验 `leaseAttempt` 和租约有效期，写入完成后原子切换 `document_search_version`。
- 新业务版本发布后，已有文档继续读取上一版索引；首次发布要等索引任务完成后才可检索。
- 向量和 trigram 检索在 TopK 前应用租户、业务有效期、检索版本和 ACL 条件，并支持 RRF 融合。
- 检索评测通过受 `knowledge:eval` 保护的 HTTP 接口运行，输出 Recall@K、HitRate@K、MRR、重复率和 p95 延迟。

## Verification Commands

日常代码回归：

```bash
./mvnw -pl services/knowledge-service,quality/eval-runner test
```

外部 PostgreSQL/pgvector 验证由目标测试流水线执行，流水线提供一次性数据库连接信息后运行 `./mvnw -pl services/knowledge-service verify -Pexternal-integration`。该 profile 会执行 `Flyway.clean()`，只能连接一次性或专用测试库，不能连接共享库或生产库。这项验证不是本地 demo 的前置条件。

## Remaining Environment Work

本仓库的本地测试不提供公司语料的召回率、HNSW 参数、并发 Worker、任务积压、数据库容量或备份恢复结论。默认本地文件对象存储也不适合多实例部署。上线前需要接入公司的对象存储和 IdP，在目标 PostgreSQL/pgvector 环境完成迁移、并发、故障恢复、容量及检索质量测试。
