# Spring AI Alibaba 迁移实验

这个模块在保持业务输入和评测规则不变的前提下，验证 Spring AI Alibaba 1.1.2.3 的适配边界。

- `DashScopeProviderAdapter` 映射系统消息、用户消息、Provider Options 和响应，不对外暴露密钥。
- `RetrievalReplacementExperiment` 在同一 Golden Set 上计算 Recall@K、MRR 和 p95，并检查 Embedding 替换是否要求重建索引。
- `ConfirmationGraph` 使用真实 Graph 运行时路由低风险直接执行和高风险人工审批。
- `FrameworkCompatibilityDecision` 防止 Boot 3.5 / Spring AI 1.1 依赖意外进入 Boot 4 / Spring AI 2 主线。

## 运行入口

所有入口都从项目根目录唯一的 `config/application-default.yml` 读取配置。第 40、41 篇需要在线模型时，填写模板中已经预留的 `lab.dashscope` 区块即可。

读到哪一篇，就在 IDEA 直接运行对应主类：

| 篇目 | 主类 | 运行后会看到 |
|---|---|---|
| 40 | `SpringAiAlibabaLabApplication` | DashScope 的真实回答、模型名、Token 用量、耗时和响应 ID |
| 41 | `DashScopeRetrievalLabApplication` | 在线 Embedding 排名、Rerank 排名、Recall、MRR 和两段耗时 |
| 42 | `ConfirmationGraphLabApplication` | 低风险直达、高风险暂停确认后的状态流转 |
| 43 | `FrameworkCompatibilityLabApplication` | 主线与候选框架版本是否应隔离 |

前两个入口会调用 DashScope；后两个入口不需要模型服务。`RetrievalReplacementExperiment` 用于不访问外部接口的边界检查，在线检索则从 `DashScopeRetrievalLabApplication` 进入。

运行后再按 `DashScopeProviderAdapter`、`OnlineRetrievalReplacementExperiment`、`ConfirmationGraph`、`FrameworkCompatibilityDecision` 的顺序阅读。迁移评估时换成自己的语料和账号，重新观察质量、配额和耗时。
