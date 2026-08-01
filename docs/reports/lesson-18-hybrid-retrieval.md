# 混合检索验证

Status: VERIFIED_LOCAL_RETRIEVAL_COMPOSITION

- Orchestrator: `HybridKnowledgeRetrievalService`
- Strategy contract: `RetrievalPlan` / `RetrievalPlanProvider`
- Rank fusion: `ReciprocalRankFusion`
- Extension ports: `KnowledgeQueryRewriter`, `KnowledgeLexicalSearchRepository`, `KnowledgeReranker`
- Vector ports: `KnowledgeEmbeddingModel`, `KnowledgeChunkSearchRepository`

## 已验证

- Query Rewrite、关键词检索和 Rerank 都由显式 `RetrievalPlan` 控制；向量检索仍可独立运行，不需要空实现适配器。
- Rewrite 开启时，改写结果同时进入 Embedding 与关键词检索；Rerank 仍接收原始用户问题，避免只围绕改写后的短查询判断相关性。
- `candidateK` 必须不小于最终 `topK`，并作为向量、关键词两路的候选预算传入。
- RRF 按排名位置融合不同量纲的分数，以 `chunkId` 去重；相同融合分数使用稳定的 `chunkId` 顺序，保证回归结果可重复。
- Rerank 关闭时直接截取融合结果；开启时 Reranker 只返回 ID，应用层拒绝空 ID、重复 ID、未知 ID 和不符合约定数量的结果，不能丢失或注入候选。
- 纯向量计划不会调用 Rewrite、关键词检索或 Rerank 端口。
- `KnowledgeRetrievalConfiguration` 在 `hybrid` 模式下接入 pgvector、PostgreSQL `pg_trgm` 关键词检索、服务端计划和 RRF；Rewrite 与 Rerank 默认关闭，未配置适配器时不会静默伪造结果。

## 本地验证

```bash
./mvnw -pl services/knowledge-service \
  -Dtest=HybridKnowledgeRetrievalServiceTest,ReciprocalRankFusionTest,KnowledgeRetrievalConfigurationTest,PostgresLexicalSearchQueryTest \
  test
```

覆盖完整混合流程、纯向量流程、Rerank 候选约束、跨通道重复候选、单通道重复去重、同分稳定排序、Spring Bean 组装和关键词 SQL 结构。

## 证据边界

当前测试覆盖应用层组合算法和 PostgreSQL `pg_trgm` 基础适配。项目没有提供中文分词、Query Rewrite 或 Reranker 的公司级适配器，也没有使用目标语料测量相关性、延迟与成本。RRF 的 `rankConstant=60` 和候选预算只是默认参数；是否启用混合检索与 Rerank 应由检索评测数据决定。
