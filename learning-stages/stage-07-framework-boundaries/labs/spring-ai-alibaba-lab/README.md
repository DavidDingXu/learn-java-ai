# Spring AI Alibaba 迁移实验

这个模块在保持业务输入和评测规则不变的前提下，验证 Spring AI Alibaba 1.1.2.3 的适配边界。

- `DashScopeProviderAdapter` 映射系统消息、用户消息、Provider Options 和响应，不对外暴露密钥。
- `RetrievalReplacementExperiment` 在同一 Golden Set 上计算 Recall@K、MRR 和 p95，并检查 Embedding 替换是否要求重建索引。
- `ConfirmationGraph` 使用真实 Graph 运行时路由低风险直接执行和高风险人工审批。
- `FrameworkCompatibilityDecision` 防止 Boot 3.5 / Spring AI 1.1 依赖意外进入 Boot 4 / Spring AI 2 主线。

## 运行入口

1. 在项目根目录唯一的 `config/application-default.yml` 追加 `lab.mode`，可选 `provider`、`retrieval`、`graph` 或 `compatibility`。
2. `provider` 模式再在同一文件追加真实的 `lab.dashscope.api-key`、`base-url` 和模型名。
3. 在 IDEA 运行 `SpringAiAlibabaLabApplication`。

```yaml
lab:
  mode: provider
  dashscope:
    api-key: replace-with-your-dashscope-api-key
    base-url: https://dashscope.aliyuncs.com
    chat-model: qwen-plus
```

`provider` 会通过真实 `DashScopeChatModel` 打印回答与 Provider 元数据。`retrieval` 使用同一组 Golden Case 输出 Recall、MRR 和 p95；它用于检查替换决策，不会假装已经调用 Embedding/Rerank 服务。`graph` 运行真实 Graph 状态流转，`compatibility` 输出主线与候选版本应采用的隔离边界。

建议再按 `DashScopeProviderAdapter`、`RetrievalReplacementExperiment`、`ConfirmationGraph`、`FrameworkCompatibilityDecision` 的顺序阅读。接入公司项目时仍要使用自己的语料和账号验证配额、效果与运行边界。
