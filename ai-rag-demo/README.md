# ai-rag-demo

企业 RAG 文档管道、权限过滤、引用和高阶检索 demo。

本模块先不接真实向量库，重点演示 RAG 的工程合同：文档 metadata、Markdown / 纯文本解析、标题感知切分、chunk metadata、embedding 批处理与缓存、内存向量索引、索引任务、混合检索、规则版重排、引用回答、租户/部门过滤、Query Rewrite、多路查询、上下文压缩和无依据兜底。

当前实现不支持真实 PDF / Word 解析。生产环境可以通过 `DocumentParser` SPI 接入 Apache Tika、docx4j、PDFBox 或云端文档解析服务。

当前实现也不调用真实 embedding 模型。`DeterministicEmbeddingProvider` 只用于本地测试和示例，生产环境应该用 `EmbeddingProvider` 接入 Spring AI、Spring AI Alibaba 或 OpenAI-compatible embedding API。

当前实现也不接 pgvector、Elasticsearch 或 Milvus。`InMemoryVectorIndex` 只用于表达向量索引的 Java 边界：upsert、cosine topK、权限过滤和结果排序。

当前实现也不接真实 reranker 模型。`RerankService` 只用规则表达重排边界：保留原始召回分、计算重排分、记录来源和理由，并在重排前做权限兜底。

当前实现也不接真实回答模型。`CitationAnswerComposer` 只表达引用回答的 Java 边界：证据为空时拒答，只使用选中的证据生成回答，并保留 `documentId`、`chunkId` 和原文摘录。

当前实现的 `IndexTaskService` 是同步内存版，只表达索引任务边界：解析、切分、向量化、upsert、任务状态、版本和失败原因。生产环境需要继续接入任务表、消息队列、重试、旧版本下线和真实向量库。

当前实现的 `QueryRewriteService` 是规则版，只表达 query rewrite 的 Java 合同：保留原始问题、生成多路查询、记录改写原因。生产环境可以替换成真实模型改写，但不要丢掉这些字段。

当前实现的 `ContextCompressor` 只做按 chunk 截断，用来表达上下文压缩边界：内容可以压缩，`documentId` 和 `chunkId` 不能丢。

## 核心类

```text
DocumentMetadata
ParsedDocument
DocumentParser
SimpleDocumentParser
DocumentChunker
PolicyDocumentChunker
DocumentChunk
DocumentAccessFilter
DocumentAccessDecision
EmbeddingProvider
DeterministicEmbeddingProvider
EmbeddingBatchService
EmbeddingResult
VectorIndex
VectorIndexEntry
InMemoryVectorIndex
VectorSearchResult
IndexTaskService
IndexTaskResult
IndexTaskStatus
HybridRetriever
HybridRetrievalResult
QueryRewriteService
QueryRewriteResult
MultiQueryRagRetriever
MultiQueryRagRetrievalResult
MultiQueryRetrievalItem
ContextCompressor
ContextCompressionResult
CompressedContextItem
RerankService
RerankedRetrievalResult
CitationAnswerComposer
RagRetrievalService
Citation
RagAnswer
```

## 启动

```bash
mvn -pl ai-rag-demo spring-boot:run
```

打开前端页面：

```text
http://localhost:8086/
```

页面会提交 RAG 问题，并把回答和证据引用分开展示；下方的 RAG 链路实验会继续调用实验接口，观察文档解析、标题切分、权限过滤、混合检索、重排、Query Rewrite、上下文压缩和索引任务状态。

## 验证

```bash
curl -X POST http://localhost:8086/api/rag/answer \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "退款怎么处理",
    "tenantId": "tenant-a",
    "department": "support"
  }'
```

也可以直接观察 RAG 链路中的单点能力：

```bash
curl -X POST http://localhost:8086/api/rag/lab/retrieval \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "客户申请退款但订单已发货怎么办",
    "tenantId": "tenant-a",
    "department": "support",
    "topK": 3
  }'
```

返回结果会同时包含 `hybridResults` 和 `rerankedResults`，便于对比关键词、向量、权限过滤和重排理由。

## 测试

```bash
mvn -pl ai-rag-demo test
```

正常情况下会看到 33 个测试通过，覆盖：

- Markdown 解析并保留文档 metadata。
- 按 Markdown 标题切分。
- metadata 下沉到每个 chunk。
- 长段落稳定生成 chunkId。
- 访问决策按租户和部门过滤，并返回拒绝原因。
- 文档索引任务完成解析、切分、embedding 和向量 upsert，并在空文档时返回失败状态。
- embedding 批量去重、缓存命中和版本更新后重新向量化。
- 内存向量索引 upsert、cosine topK 和权限过滤。
- 关键词检索和向量检索通过 RRF 合并，并在合并前完成权限过滤。
- Query Rewrite 保留原始问题，生成业务 query，并记录改写原因。
- 多路查询按 chunkId 合并结果，并保留命中过的 query。
- 上下文压缩可以裁剪内容，但保留 `documentId` 和 `chunkId`。
- 规则版 rerank 能重新排序相关候选、过滤越权候选，并保留原始召回证据。
- 引用回答只使用选中的证据，无依据时拒答，并限制进入回答层的证据数量。
- 检索前按租户和部门过滤。
- `/api/rag/answer`、`/api/rag/lab/pipeline`、`/api/rag/lab/access`、`/api/rag/lab/retrieval`、`/api/rag/lab/rewrite`、`/api/rag/lab/index` 的 HTTP 合同。
- 前端页面必须暴露问答入口和 RAG 链路实验入口。
