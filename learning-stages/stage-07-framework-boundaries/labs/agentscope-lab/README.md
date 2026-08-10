# AgentScope 运行时实验

这个模块使用 AgentScope Java 2.0 验证 Agent 运行时与协议边界，身份、Tool 策略、人工介入和幂等仍由应用层掌握。

- `AgentScopeTicketRuntime` 把可信身份和 Tool 策略映射为 `PermissionEngine` 决策。
- `CollaborationPolicy` 根据任务依赖和副作用事实，选择单 Agent、多 Agent 或必须人工处理。
- `EnterpriseMcpRegistry` 只导入通过 HTTPS、允许列表和只读策略验证的 MCP 工具。
- `A2aTaskCoordinator` 管理幂等、请求指纹、单调状态和 `UNKNOWN` 交付结果。

## 运行入口

1. 使用项目根目录唯一的 `config/application-default.yml`，程序会复用其中的 OpenAI 兼容 API Key、Base URL 和 Chat 模型。
2. 读第 48 篇时运行 `AgentScopeLabApplication`，观察模型先调用 `query_ticket`，再根据 Tool 返回的工单事实回答。
3. 读第 49 篇时运行 `MultiAgentCollaborationApplication`，观察政策 Agent 与工单 Agent 并行取证，再由汇总 Agent 只根据两份结果作答。
4. 再运行 `MultiAgentFailureApplication`，观察任一专家失败后跳过汇总并进入 `HUMAN_REQUIRED`。

在线入口使用 AgentScope 的 `OpenAIChatModel`、`ReActAgent`、`Toolkit` 和 `PermissionContextState`。政策 Agent 与工单 Agent 拥有彼此隔离的只读 Tool 和权限；协调器负责并发、超时、失败短路和人工接管，模型不掌握这些控制权。

再按 `AgentScopeTicketRuntime`、`CollaborationPolicy`、`MultiAgentCoordinator`、`EnterpriseMcpRegistry`、`A2aTaskCoordinator` 阅读应用层边界。两个在线 Agent 读取的是本地演示事实；接入实际系统时，只需要替换各自的 Tool 适配器，不改变协调与失败语义。
