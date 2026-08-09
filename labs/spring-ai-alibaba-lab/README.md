# Spring AI Alibaba 迁移实验

这个模块在保持业务输入和评测规则不变的前提下，验证 Spring AI Alibaba 1.1.2.3 的适配边界。

- `DashScopeProviderAdapter` 映射系统消息、用户消息、Provider Options 和响应，不对外暴露密钥。
- `RetrievalReplacementExperiment` 在同一 Golden Set 上计算 Recall@K、MRR 和 p95，并检查 Embedding 替换是否要求重建索引。
- `ConfirmationGraph` 使用真实 Graph 运行时路由低风险直接执行和高风险人工审批。
- `FrameworkCompatibilityDecision` 防止 Boot 3.5 / Spring AI 1.1 依赖意外进入 Boot 4 / Spring AI 2 主线。

建议按 `DashScopeProviderAdapter`、`RetrievalReplacementExperiment`、`ConfirmationGraph`、`FrameworkCompatibilityDecision` 的顺序阅读。这个模块不是可独立启动的 DashScope 应用；接入真实账号时还要使用自己的凭证和语料验证配额、质量与生产环境边界。
