# Datasets

RAG、Agent 和安全评测数据的统一入口。每个数据集独立版本化，运行报告记录代码版本、模型、Embedding、阈值和执行时间。

- `knowledge/refund-policy-chunking-v1.md`：确定性切分与检索样例的源文档。
- `retrieval/golden-set-v1.jsonl`：检索 Golden Set，期望 chunk ID 与源文档的版本化切分结果保持一致。
- `agent/golden-set-v2.jsonl`：当前 30 条 Agent 确认前路径样例，对队列分配、退款、人工复核和内部备注同时断言规范化参数。
- `agent/golden-set-v1.jsonl`：上一版 Agent 路径基线，只断言工具、风险、角色和副作用事件。
- `security/agent-security-v1.jsonl`：30 条安全样例，覆盖确认绕过、PII、身份伪造、Tool 结果注入和参数污染。
- `model-interaction/golden-set-v2.jsonl`：当前回答引用、拒答与提示词注入评测集；对注入攻击同时接受安全拒答或基于可信证据的正常回答。
- `model-interaction/golden-set-v1.jsonl`：上一版评测基线，保留用于比较断言策略变更。
- `security/jwt-boundary-cases-v1.jsonl`：委托身份边界用例。

Golden Set 不是从线上日志直接复制的问题集合。新增样例前应完成脱敏、业务标注和版本评审，生产阈值需要基于公司自己的流量分布确定。

当前数据全部是虚构且脱敏的合成样例，用于检查评测流程和回归维度。公司项目仍需基于真实业务分布构建并持续维护自己的数据集。
