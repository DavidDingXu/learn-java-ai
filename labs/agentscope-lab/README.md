# AgentScope 运行时实验

这个模块使用 AgentScope Java 2.0 验证 Agent 运行时与协议边界，身份、Tool 策略、人工介入和幂等仍由应用层掌握。

- `AgentScopeTicketRuntime` 把可信身份和 Tool 策略映射为 `PermissionEngine` 决策。
- `CollaborationPolicy` 根据任务依赖和副作用事实，选择单 Agent、多 Agent 或必须人工处理。
- `EnterpriseMcpRegistry` 只导入通过 HTTPS、允许列表和只读策略验证的 MCP 工具。
- `A2aTaskCoordinator` 管理幂等、请求指纹、单调状态和 `UNKNOWN` 交付结果。

建议按 `AgentScopeTicketRuntime`、`CollaborationPolicy`、`EnterpriseMcpRegistry`、`A2aTaskCoordinator` 的顺序阅读。它们分别对应 Tool 裁决、协作选择、远程工具准入和任务状态，不组成一个可独立启动的 Agent 应用，也不证明远程 Agent 的可用性、性能或信任等级。
