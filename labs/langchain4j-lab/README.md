# LangChain4j 业务端口实验

这个模块验证选定用例能否迁移到 LangChain4j，而不重写主应用架构。

- `LangChain4jPolicyAnswerAdapter` 使用 AI Services 实现稳定的回答端口，并将调用失败与非法输出映射为应用错误。
- `TenantScopedRagAdapter` 在内容进入模型前，通过 `KnowledgeSearchPort` 执行带 ACL 的检索。
- `LangChain4jTicketDecisionAdapter` 执行只读 Tool，并将结果解析为业务记录。
- `FrameworkCoexistencePolicy` 按能力路由，不在模块之间传递框架对象。

## 运行入口

1. 使用项目根目录唯一的 `config/application-default.yml`，程序会复用其中的 OpenAI 兼容 API Key、Base URL 和 Chat 模型。
2. 将 `lab.mode` 设为 `answer`、`rag` 或 `tool`。
3. 在 IDEA 运行 `LangChain4jLabApplication`。

`answer` 会打印真实模型回答，`rag` 会打印带来源的租户范围回答，`tool` 会打印结构化决策和 Tool 调用次数。三个模式都走真实 `OpenAiChatModel`，不是固定回答模型。

建议再按 `LangChain4jPolicyAnswerAdapter`、`TenantScopedRagAdapter`、`LangChain4jTicketDecisionAdapter`、`FrameworkCoexistencePolicy` 的顺序阅读。这个实验验证端口迁移和错误边界，不代表已经完成性能、成本和生产运维验收。
