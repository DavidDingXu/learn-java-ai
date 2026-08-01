# RAG 评测验证

Status: VERIFIED_LOCAL_EVALUATION_CONTRACT

- Evaluation input: `RetrievalEvalResult`
- Metric output: `RetrievalMetrics`
- Calculator: `RetrievalMetricsCalculator`
- Dataset: `datasets/retrieval/golden-set-v1.jsonl`
- HTTP client: `KnowledgeRetrievalHttpClient`
- Service endpoint: `KnowledgeRetrievalEvaluationController`
- Runner command: `retrieval-eval`

## 已验证

- 每个样例必须包含稳定 case ID、至少一个期望 chunk ID 和有序检索结果。
- 先按原始排名截取 TopK，再对命中集合去重；重复结果会占用排序名额，并通过 `duplicateRateAtK` 单独暴露。
- `Recall@K` 按每条样例先计算召回比例再做宏平均，适用于一题对应多个正确 chunk 的情况。
- `HitRate@K` 统计至少命中一个正确 chunk 的样例比例，`MRR` 使用第一个正确 chunk 的倒数排名。
- 只要样例在 TopK 内没有找全期望 chunk，就进入 `failedCaseIds`，聚合分数不会隐藏坏案例。
- 固定测试集的预期结果为 `Recall@3=2/3`、`HitRate@3=2/3`、`MRR=0.5`，失败样例为 `invoice-change`；重复候选样例验证 `Recall@2=0.5` 和重复率 `0.5`。
- Golden Set 中的期望 Chunk ID 由版本化源文档、文档版本和切分策略重新计算并做一致性校验，避免样例引用漂移。
- 评测接口只接受问题和 TopK，访问范围来自 JWT；安全配置要求独立 `knowledge:eval` scope。
- 评测接口把同步 JDBC 检索调度到 `boundedElastic`，避免阻塞 WebFlux 事件循环。
- Eval Runner 通过 HTTP 调用实际知识服务，记录逐案例召回、Embedding 模型和延迟，输出 JSON、Markdown，并检查 Recall、HitRate、MRR、重复率和 p95 延迟是否达到阈值。
- 同一轮返回多个 Embedding 模型时即使聚合指标达标也判定失败。

## 本地验证

```bash
./mvnw -pl services/knowledge-service,quality/eval-runner \
  -Dtest=RetrievalGoldenDatasetContractTest,KnowledgeRetrievalEvaluationControllerTest,RetrievalMetricsCalculatorTest,RetrievalEvalDatasetLoaderTest,RetrievalEvaluatorTest,RetrievalEvaluationReportWriterTest,KnowledgeRetrievalHttpClientTest \
  test
```

覆盖指标公式、重复位置口径、数据集版本与 Chunk ID 一致性、JWT 访问范围映射、HTTP 请求格式、阈值检查和报告输出。

## 证据边界

本地测试覆盖评测规则、公式和运行编排，不提供共享环境的 Recall@K 结论。计算目标指标前，必须先准备文档、ACL 和向量索引，再按 `docs/runbooks/model-interaction-eval.md` 使用 `java -jar` 与 `--bearer-token-file` 连接目标知识服务。仓库中的 Shell 与 PowerShell 入口用于共享 CI 聚合这些步骤，日常本地回归不依赖它们。

当前指标还没有覆盖 Precision@K、nDCG、禁止召回、ACL 泄漏率、时效命中率、分组基线和回答忠实度，不能用现有聚合结果代替这些判断。
