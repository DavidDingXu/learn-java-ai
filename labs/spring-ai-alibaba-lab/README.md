# Spring AI Alibaba 迁移实验

这个模块在保持业务输入和评测规则不变的前提下，验证 Spring AI Alibaba 1.1.2.3 的适配边界。

- `DashScopeProviderAdapter` 映射系统消息、用户消息、Provider Options 和响应，不对外暴露密钥。
- `RetrievalReplacementExperiment` 在同一 Golden Set 上计算 Recall@K、MRR 和 p95，并检查 Embedding 替换是否要求重建索引。
- `ConfirmationGraph` 使用真实 Graph 运行时路由低风险直接执行和高风险人工审批。
- `FrameworkCompatibilityDecision` 防止 Boot 3.5 / Spring AI 1.1 依赖意外进入 Boot 4 / Spring AI 2 主线。

在项目根目录执行：

```bash
./mvnw -f labs/pom.xml -pl spring-ai-alibaba-lab test
```

默认测试使用确定性输入，用于检查映射、决策和路由逻辑，不代表 DashScope 账号、配额或生产环境已经验收。
