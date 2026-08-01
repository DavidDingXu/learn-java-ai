# pgvector 检索验证

Status: IMPLEMENTED_WITH_LOCAL_TESTS_AND_EXTERNAL_PROFILE

- Application service: `KnowledgeRetrievalService`
- Embedding port and adapter: `KnowledgeEmbeddingModel` / `SpringAiKnowledgeEmbeddingModel`
- Search adapter: `PgVectorKnowledgeChunkSearchRepository`
- SQL builder: `PgVectorSearchQuery`
- Schema source: `db/migration/V1__knowledge_platform.sql` and `db/migration/V4__document_search_version.sql`
- External test: `PgVectorExternalIT`

## 本地已验证

- 应用层只依赖 Embedding 与检索端口；Spring AI 的 `EmbeddingModel` 和 JDBC 类型没有进入应用接口。
- `SpringAiKnowledgeEmbeddingModel` 映射向量及 Provider 返回的模型名；缺失向量或模型元数据会被拒绝。
- Flyway 创建 `VECTOR(1536)` 字段和基于 cosine distance 的 HNSW 索引，迁移文件是当前数据库结构的唯一来源。
- pgvector 查询使用参数绑定传入向量、Embedding 模型、租户、有效时间和 TopK，并按 `<=>` 距离排序后限制结果数量。
- 查询只允许 `ACTIVE` 文档、存在当前有效 `PUBLISHED` 业务版本的文档，以及 `document_search_version` 指向且 Embedding 模型一致的分块。发布替换版本后，新索引完成前继续读取上一版分块。
- JDBC 适配器把 `heading_path`、条款、正文、文档版本和相似度映射为 `RetrievedKnowledgeChunk`。
- 测试源码中的 `DeterministicHashEmbeddingModel` 只提供稳定本地向量，用来检查确定性和单位范数，不把它当作语义 Embedding。

## 本地验证

以下命令只运行 JVM 内的单元测试、SQL 结构和迁移文件检查，不连接 PostgreSQL，也不调用 Provider Embedding API：

```bash
./mvnw -pl services/knowledge-service \
  -Dtest=KnowledgeSchemaMigrationContractTest,KnowledgeRetrievalServiceTest,SpringAiKnowledgeEmbeddingModelTest,DeterministicHashEmbeddingModelTest,PgVectorSearchQueryTest,PgVectorKnowledgeChunkSearchRepositoryTest \
  test
```

这些测试覆盖端口编排、SQL 结构、参数绑定、检索版本选择和结果映射。

## 外部 pgvector 验证

外部测试必须使用安装了 `vector` 扩展的专用数据库。`PgVectorExternalIT` 会执行 `Flyway.clean()` 并重建结构，禁止指向共享或生产数据库。这项破坏性验证不属于默认演示步骤。

该测试执行全部 Flyway 迁移，从跨租户、跨权限测试数据中只返回 `allowed-chunk`，并检查替换索引完成前读取上一版、完成后切换到新版。Knowledge Service 的生产连接项位于 `application.yml` 的 `production` 段；仓库只保留占位值，真实密码必须由部署平台的密钥系统覆盖。具体环境是否通过应以该次测试报告为准。

## 证据边界

本地测试不连接 PostgreSQL/pgvector，也不检查 Embedding 质量。外部测试的数据量很小，向量也是固定输入，只覆盖迁移、向量查询、权限过滤和检索版本切换；HNSW 参数、索引构建耗时、连接池容量、公司语料召回、备份恢复和高可用仍需在目标环境测试。
