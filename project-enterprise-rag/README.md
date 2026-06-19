# project-enterprise-rag

这是企业制度 RAG 知识库主项目。

它围绕企业制度问答展开，把 RAG 系统里最容易被忽略的生产边界放进代码：

- 文档上传
- 文档版本
- 索引任务
- 文档切分
- 权限过滤
- 混合检索
- 引用回答
- 证据冲突和时效治理
- RAG trace
- 检索和引用质量评测

当前第一版使用内存仓储，方便读者直接运行测试。它已经把文档版本、权限、引用、索引任务、证据治理、RAG Eval 和 trace 这些边界落到代码里。MySQL、pgvector、Elasticsearch、MinIO、真实 embedding 和 reranker 还没有接入，后续应该通过仓储和 provider 替换，不改应用层边界。

## 核心类

```text
EnterpriseRagApplicationService // 应用编排入口
PolicyDocumentUpload            // 文档上传请求
PolicyDocument                   // 文档元数据和版本
PolicyDocumentChunker            // 文档切分
DocumentChunk                    // 检索最小单元
HybridPolicyRetriever            // 混合检索入口
EvidenceGovernanceService        // 证据冲突、时效和优先级治理
RagAnswer / Citation             // 回答和引用
RagTrace / RagTraceStep          // RAG 链路追踪
EvalCase / EvalReport            // 质量评测
```

## 运行测试

```bash
mvn -pl project-enterprise-rag test
```

正常情况下会看到 9 个测试通过，覆盖领域编排、文档上传索引、权限过滤、引用回答、证据冲突治理、RAG Eval 和 REST 接口。

## 启动服务

```bash
mvn -pl project-enterprise-rag spring-boot:run
```

默认端口是 `8092`。

## 上传并索引制度文档

```bash
curl -X POST http://localhost:8092/api/enterprise-rag/documents \
  -H 'Content-Type: application/json' \
  -d '{
    "documentId": "refund-policy-2026",
    "tenantId": "tenant-a",
    "departments": ["support"],
    "type": "POLICY",
    "content": "退款制度\n发货后退款需先核对物流状态。\n高金额退款必须转人工复核。"
  }'
```

重点看返回里的：

```text
status = COMPLETED
documentId = refund-policy-2026
documentVersion = 1
chunkCount = 2
```

## 发起知识库问答

```bash
curl -X POST http://localhost:8092/api/enterprise-rag/answers \
  -H 'Content-Type: application/json' \
  -d '{
    "question": "客户申请退款但订单已发货怎么办",
    "tenantId": "tenant-a",
    "department": "support"
  }'
```

返回应该包含制度原文片段、引用和 RAG trace：

```text
citations[0].documentId = refund-policy-2026
trace.steps = query-rewrite, access-filter, hybrid-retrieve, evidence-governance, citation-compose
```

如果把 `department` 改成 `hr`，当前用户就查不到 support 部门制度，回答会返回没有可访问依据。

## 验证知识冲突治理

先上传一份 FAQ，再上传一份制度。两份文档使用同一个 `#topic=refund-shipped`，但结论相互冲突：

```bash
curl -X POST http://localhost:8092/api/enterprise-rag/documents \
  -H 'Content-Type: application/json' \
  -d '{
    "documentId": "refund-faq",
    "tenantId": "tenant-a",
    "departments": ["support"],
    "type": "FAQ",
    "content": "退款 FAQ\n#topic=refund-shipped priority=10 发货后退款可以直接退款。"
  }'

curl -X POST http://localhost:8092/api/enterprise-rag/documents \
  -H 'Content-Type: application/json' \
  -d '{
    "documentId": "refund-policy-2026",
    "tenantId": "tenant-a",
    "departments": ["support"],
    "type": "POLICY",
    "content": "退款制度\n#topic=refund-shipped priority=20 发货后退款必须转人工复核，不能直接执行退款。"
  }'
```

再提问：

```bash
curl -X POST http://localhost:8092/api/enterprise-rag/answers \
  -H 'Content-Type: application/json' \
  -d '{
    "question": "发货后退款怎么处理",
    "tenantId": "tenant-a",
    "department": "support"
  }'
```

此时回答不会拼接两条冲突依据，而是返回需要人工确认。重点看 trace：

```text
trace.steps[*].name 包含 evidence-governance
trace.steps[*].detail 包含 status=CONFLICTED
```

## 跑一次 RAG Eval

```bash
curl -X POST http://localhost:8092/api/enterprise-rag/eval \
  -H 'Content-Type: application/json' \
  -d '[
    {
      "caseId": "E-001",
      "question": "发货后退款怎么处理",
      "tenantId": "tenant-a",
      "department": "support",
      "expectedDocumentId": "refund-policy-2026"
    }
  ]'
```

重点看：

```text
retrievalHitRate = 1.00
citationHitRate = 1.00
```

## 已覆盖的生产边界

- 同一文档重复上传会生成新版本。
- 新版本索引会替换旧 chunk，避免新旧内容混查。
- 检索前会按租户和部门过滤。
- 证据进入回答前会检查 topic、priority、effectiveFrom、effectiveTo。
- 同一 topic 出现互斥结论时，默认不生成答案，并在 trace 里记录冲突原因。
- 回答必须携带引用。
- Eval 同时计算 retrieval hit 和 citation hit。

## 分阶段演进

这个项目按阶段演进。先固定业务边界，再替换基础设施。

### 阶段 1：内存版边界

当前已经完成：

- 文档上传和版本递增。
- 新版本索引替换旧 chunk。
- 租户和部门权限过滤。
- 混合检索。
- 引用回答。
- 证据冲突、优先级和时效治理。
- RAG Eval。
- RAG Trace。

### 阶段 2：基础设施替换

推荐顺序：

1. `PolicyDocument` 元数据进入 MySQL。
2. 原始文件进入 MinIO。
3. `IndexTask` 状态持久化。
4. `DocumentChunk` 索引进入 pgvector 或 Elasticsearch。
5. 增加 Markdown / PDF / Word parser。

替换时保留 `EnterpriseRagApplicationService` 的主流程。仓储和索引实现可以变，文档版本、权限、引用和 trace 边界不变。

### 阶段 3：真实模型能力

推荐顺序：

1. embedding provider 接真实模型服务。
2. reranker provider 接真实重排服务。
3. 回答生成通过模型网关调用模型。
4. 输出仍然必须带 citation。

模型只接收权限过滤后的 chunk。

### 阶段 4：治理和上线

后续要接入：

- `ai-observability-demo` 的 trace 模型。
- `ai-eval-demo` 的 golden set 和回归思路。
- 坏 case 复盘。
- 召回质量、引用准确率、索引失败率监控。
- 发布验收和回滚计划。
