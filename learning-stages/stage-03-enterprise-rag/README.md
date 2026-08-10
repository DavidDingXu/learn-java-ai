# 阶段 03：企业 RAG

本阶段在模型调用链上加入文档生命周期、切分、PostgreSQL/pgvector、ACL、混合检索、增量索引和检索评测，代码仍然来自当前主线实现。

1. 按 [RAG 本地准备](../../docs/runbooks/rag-prerequisites.md)创建专用 PostgreSQL 数据库，并填写项目根目录唯一的 `config/application-default.yml`。
2. 在 IDEA 中运行 `KnowledgeServiceApplication`。
3. 打开 `rag-learning-journey.http`，从上到下执行请求，依次观察上传、发布、索引、检索、ACL 负例、混合检索和带引用回答。

Chat 与 Embedding 默认共用同一个 OpenAI 兼容 API Key 和 Base URL。只有该接口不提供 Embedding 时，才在同一份本地 YAML 中把 Embedding 切换为 Ollama，业务代码不变。

HTTP 文件使用稳定文档 ID，便于第 21 篇直接回放 Golden Set。中断后从失败的命名请求继续；需要从头重跑时，按准备文档只重建专用学习库。

| 篇目 | 对应操作 | 先看代码 | 页面或响应变化 |
|---|---|---|---|
| 13 | 理解文档状态 | `KnowledgeDocument`、`DocumentVersion` | 上传与发布是两个业务状态 |
| 14 | HTTP `02`、`03` | `KnowledgeDocumentController` | 得到文档 ID、版本和发布结果 |
| 15 | 发布时切分 | `PolicyDocumentChunker` | 同一版本生成可追溯 Chunk |
| 16 | HTTP `08-vector-retrieval` | `PgVectorKnowledgeChunkSearchRepository` | 向量召回返回目标政策 |
| 17 | HTTP `04`、`05` 后再检索 | `PgVectorSearchQuery` | 无权限文档不会进入 TopK |
| 18 | HTTP `09-hybrid-retrieval` | `HybridKnowledgeRetrievalService` | 关键词与向量结果合并排序 |
| 19 | HTTP `10-grounded-answer` | `RetrievalPolicyContextSource` | 回答携带可核对引用 |
| 20 | HTTP `06`、`07`、`11` | `IndexTaskWorker` | 索引任务可重试且重复执行不重复写入 |
| 21 | `EvalRunner` 的 `retrieval-eval` | `RetrievalMetricsCalculator` | Recall、HitRate、MRR、重复率和 p95 报告 |

阶段 03 的评测器只包含模型与检索任务。Agent 轨迹和安全评测从阶段 05、06 才出现，避免读者在 RAG 章节提前看到后续实现。
