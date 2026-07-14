# Lesson 16 pgvector Search Evidence

Status: VERIFIED_LOCAL_VECTOR_CONTRACT_WITH_EXTERNAL_PROFILE

- Application service: `KnowledgeRetrievalService`
- Embedding port and adapter: `KnowledgeEmbeddingModel` / `SpringAiKnowledgeEmbeddingModel`
- Search adapter: `PgVectorKnowledgeChunkSearchRepository`
- SQL builder: `PgVectorSearchQuery`
- Schema source: `db/migration/V1__knowledge_platform.sql`
- External test: `PgVectorExternalIT`

## Verified Locally

- 应用层只依赖 Embedding 与检索端口；Spring AI 的 `EmbeddingModel` 和 JDBC 类型没有进入应用合同。
- `SpringAiKnowledgeEmbeddingModel` 映射向量及 Provider 返回的模型名；缺失向量或模型元数据会被拒绝。
- Flyway 创建 `VECTOR(1536)` 字段和基于 cosine distance 的 HNSW 索引，迁移文件是当前数据库结构的唯一来源。
- pgvector 查询使用参数绑定传入向量、Embedding 模型、租户、有效时间和 TopK，并按 `<=>` 距离排序后限制结果数量。
- 查询只允许 `ACTIVE` 文档、当前 `PUBLISHED` 版本和与查询向量相同的 Embedding 模型，避免模型迁移期间混排不兼容向量。
- JDBC 适配器把 `heading_path`、条款、正文、文档版本和相似度映射为 `RetrievedKnowledgeChunk`。
- `DeterministicHashEmbeddingModel` 只提供无外部模型时的稳定本地向量，测试验证其确定性和单位范数，不把它当作语义 Embedding。

## Local Verification

以下命令只运行 JVM 内的单元测试和 SQL/迁移合同测试，不连接真实 PostgreSQL，也不调用真实 Embedding API：

```bash
./mvnw -pl services/knowledge-service \
  -Dtest=KnowledgeSchemaMigrationContractTest,KnowledgeRetrievalServiceTest,SpringAiKnowledgeEmbeddingModelTest,DeterministicHashEmbeddingModelTest,PgVectorSearchQueryTest,PgVectorKnowledgeChunkSearchRepositoryTest \
  test
```

在项目支持的 JDK 21 环境中，预期 7 个测试全部通过。这里证明的是端口编排、SQL 结构、参数绑定和结果映射。

## External pgvector Verification

外部验证必须使用安装了 `vector` 扩展的专用测试库。测试会执行 `Flyway.clean()` 并重建结构，禁止指向共享或生产数据库。

```bash
export JAVA_AI_POSTGRES_URL='jdbc:postgresql://127.0.0.1:5432/java_ai_test'
export JAVA_AI_POSTGRES_USER='java_ai'
export JAVA_AI_POSTGRES_PASSWORD='replace-with-test-password'

./mvnw -pl services/knowledge-service verify -Pexternal-integration
```

预期 `PgVectorExternalIT` 执行一条 Flyway 迁移，并从三份跨租户、跨权限测试数据中只返回 `allowed-chunk`。CI 中的 `.github/workflows/pgvector-integration.yml` 使用同一 Maven profile；没有该运行结果时，不能声称真实 pgvector 已验证。

## Evidence Boundary

本地测试不证明 PostgreSQL/pgvector 可连接，也不证明真实 Embedding 质量。外部测试只验证一次迁移、向量查询和权限过滤，数据量很小且向量人为固定；它不覆盖 HNSW 参数调优、索引构建耗时、连接池容量、真实语义召回、备份恢复或生产高可用。
