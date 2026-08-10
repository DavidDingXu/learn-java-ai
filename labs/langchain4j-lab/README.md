# LangChain4j 业务端口实验

这个模块验证选定用例能否迁移到 LangChain4j，而不重写主应用架构。

- `LangChain4jPolicyAnswerAdapter` 使用 AI Services 实现稳定的回答端口，并将调用失败与非法输出映射为应用错误。
- `TenantScopedRagAdapter` 在内容进入模型前，通过 `KnowledgeSearchPort` 执行带 ACL 的检索。
- `LangChain4jTicketDecisionAdapter` 执行只读 Tool，并将结果解析为业务记录。
- `FrameworkCoexistencePolicy` 按能力路由，不在模块之间传递框架对象。

## 运行入口

1. 使用项目根目录唯一的 `config/application-default.yml`，程序会复用其中的 OpenAI 兼容 API Key、Base URL 和 Chat 模型。
2. 读第 44 篇时运行 `LangChain4jLabApplication`，查看 AI Services 适配后的真实回答。
3. 读第 45 篇时运行 `LangChain4jRagLabApplication`，查看带来源的租户范围回答。
4. 读第 46 篇时运行 `LangChain4jToolLabApplication`，查看结构化决策和 Tool 调用次数。

三个入口都走真实 `OpenAiChatModel`。RAG 入口使用受控内存语料突出 ACL 和上下文边界，Tool 入口只注册只读查询；它们不是固定回答模型，也不会让读者先理解一套模式开关。

运行后再按 `LangChain4jPolicyAnswerAdapter`、`TenantScopedRagAdapter`、`LangChain4jTicketDecisionAdapter`、`FrameworkCoexistencePolicy` 的顺序阅读。这里回答的是业务端口能否平移；性能、成本和运维指标留给目标环境评估。
