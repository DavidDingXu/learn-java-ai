# AgentScope 运行时实验

这个模块使用 AgentScope Java 2.0 验证 Agent 运行时与协议边界，身份、Tool 策略、人工介入和幂等仍由应用层掌握。

- `AgentScopeTicketRuntime` 把可信身份和 Tool 策略映射为 `PermissionEngine` 决策。
- `CollaborationPolicy` 根据任务依赖和副作用事实，选择单 Agent、多 Agent 或必须人工处理。
- `EnterpriseMcpRegistry` 只导入通过 HTTPS、允许列表和只读策略验证的 MCP 工具。
- `A2aTaskCoordinator` 管理幂等、请求指纹、单调状态和 `UNKNOWN` 交付结果。

## 运行入口

1. 在 `src/main/resources/application.properties` 填写 OpenAI 兼容 API 的 Key、Base URL 和模型名。
2. 在 IDEA 运行 `AgentScopeLabApplication`。
3. 观察模型先调用 `query_ticket`，再根据 Tool 返回的真实工单事实回答。

这个入口使用 AgentScope 的 `OpenAIChatModel`、`ReActAgent`、`Toolkit` 和 `PermissionContextState`。`query_ticket` 被允许，写 Tool 没有授权；权限不是通过提示词假设出来的。

再按 `AgentScopeTicketRuntime`、`CollaborationPolicy`、`EnterpriseMcpRegistry`、`A2aTaskCoordinator` 阅读应用层边界。当前入口证明单 Agent 的真实模型与 Tool 链路，`CollaborationPolicy` 只负责判断何时值得拆成多 Agent，并不冒充已经执行多个远程 Agent。
