# ai-mcp-demo

这个模块演示 MCP 在 Java AI 工程里的边界，不直接绑定某个具体业务 Service。

这个模块提供了薄 Web 层和前端页面，用来观察 MCP Server 能力发现、Host Client 工具调用、资源权限过滤、审计记录和远程调试报告。核心协议对象仍然是 source of truth，Web 层只负责把这些合同展示出来。

示例场景是“企业制度中心 MCP Server”：

- Server 暴露 `tools`、`resources`、`prompts`。
- Host 侧的 Client 初始化后保留工具、资源、Prompt 能力快照。
- Host 侧的 Client 按 `ToolAccessPolicy` 调用工具。
- 资源读取必须带 `OperatorContext`，按租户和部门过滤。
- 工具调用、拒绝原因进入 `McpAuditLedger`，方便后续接全链路追踪。
- 远程接入通过 `McpRemoteEndpoint` 表达 endpoint、transport、timeout 和可用性。
- Host 侧通过 `McpRemoteHostClient#debugReport` 输出连接状态和能力快照，便于排查远程 MCP Server。

## 核心类

```text
PolicyMcpServer      // 模拟内部制度中心 MCP Server
McpHostClient        // Host 侧 MCP Client，负责 allowlist 和审计
McpClientSession     // Client 初始化后的能力快照
McpServerManifest    // Server 暴露的 tools/resources/prompts 清单
ToolAccessPolicy     // Host 侧工具白名单和运行时权限策略
McpAuditLedger       // MCP 工具调用审计
OperatorContext      // 用户身份、租户、部门上下文
McpRemoteEndpoint    // 远程 MCP Server 端点描述
McpRemoteHostClient  // Host 侧远程 Client，负责初始化、调用和调试报告
McpDebugReport       // 连接状态、transport、工具/资源/Prompt 快照
```

## 运行测试

```bash
mvn -pl ai-mcp-demo test
```

启动前端页面：

```bash
mvn -pl ai-mcp-demo spring-boot:run
```

浏览器打开：

```text
http://localhost:8093/
```

页面会调用 `/api/mcp/session`、`/api/mcp/tools/call`、`/api/mcp/resources/read`、`/api/mcp/debug`，分别观察能力快照、工具 allowlist、资源可见性和远程调试报告。

重点测试：

```text
serverPublishesToolsResourcesAndPrompts
hostClientInitializationKeepsDiscoveredCapabilitiesAsSnapshot
hostClientCallsOnlyAllowlistedToolsAndRecordsAudit
toolPolicyDeniesSensitiveToolWhenOperatorDoesNotHaveRequiredPermission
toolPolicyAllowsSensitiveToolWhenOperatorHasRequiredPermission
resourceReadIsFilteredByTenantAndDepartment
promptRenderingKeepsBusinessVariablesExplicit
remoteEndpointCreatesSessionAndDebugReportFromManifestSnapshot
debugReportMarksEndpointDisconnectedWhenServerIsMissing
```

## 重点边界

MCP 能标准化工具和上下文协议，但不能替代企业系统自己的权限、审计和灰度控制。

这个模块里的远程传输是内存模拟版，用来表达 Java 侧的工程合同，不是真实 HTTP Server。生产接入时，`McpRemoteEndpoint` 应替换为正式 MCP transport，例如 stdio 或 Streamable HTTP；Host 侧仍然需要保留 allowlist、身份传递、审计、trace 和连接诊断。

排查远程 MCP Server 时，先看 `McpDebugReport`：

```text
connected       是否连上 Server
transportType   当前 transport 类型
connectTimeout  连接超时配置
tools           初始化后看到的工具快照
resources       初始化后看到的资源快照
prompts         初始化后看到的 Prompt 快照
error           连接失败或初始化失败原因
```

在真实生产里，`McpHostClient` 这一层还要继续接：

- tool allowlist
- tool access policy
- identity propagation
- tenant scope
- audit ledger
- traceId
- rate limit
- secret management
