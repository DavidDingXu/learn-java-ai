# MCP 与 A2A 互操作实验

这个模块使用 MCP Java SDK 与 A2A Java SDK 验证真实协议交互，同时保留企业应用必须的本地信任决策。

- MCP 实验通过 Streamable HTTP 完成初始化、工具发现和 `tools/call`。
- 远程工具必须通过本地允许目录、只读和非破坏性规则，不会因为 Server 已返回 Schema 就直接交给模型。
- A2A 实验验证任务创建、状态转移、重复请求和不确定交付结果。
- 协议负责互操作；身份、权限、超时、幂等、审计和错误映射仍是应用责任。

## 运行入口

- 在 IDEA 运行 `McpLabApplication`。程序会启动本地 Streamable HTTP MCP Server，官方客户端完成初始化、工具发现和 `query_ticket` 调用，最后打印协议版本、工具清单和返回结果。
- 在 IDEA 运行 `A2aLabApplication`。程序会启动本地 A2A 服务，官方客户端发现 Agent Card、发送风险分析任务，并打印任务状态和 Artifact。

两个入口不需要 API Key，也不需要读者另行安装协议服务。建议随后阅读 `EnterpriseMcpClient` 和 `EnterpriseA2aClient`，看应用层怎样收紧服务器、工具和任务边界。连接公司远程服务时，仍需提供受管地址、身份凭证、超时、审计和业务准入规则。
