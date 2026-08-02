# MCP 工具调用边界验证

Status: VERIFIED_PROTOCOL_INTEROPERABILITY

## 已验证

- MCP Java SDK `2.0.0` 按 `2025-11-25` 协议基线运行。
- 官方同步客户端与服务端通过 Streamable HTTP 完成初始化、`tools/list` 和 `tools/call`。
- `EnterpriseMcpClient` exposes only locally allowlisted tools and rejects tools without the approved read-only contract.
- 发现回执记录协商后的协议、服务端身份和已注册 Tool。
- `EnterpriseMcpRegistry` resolves endpoints from a managed `serverId -> HTTPS endpoint` map; callers cannot pair an approved ID with an arbitrary URL.
- 一批发现结果会在注册前完成整体校验，后续无效 Tool 不会导致前面的 Tool 被部分注册。
- AgentScope 适配器使用同一个 `Toolkit` 注册外部 Tool，并由 `PermissionEngine` 决策；缺少本地权限规则的已注册 Tool 仍会被拒绝。
- AgentScope Tool 注册保持为独立的业务运行时适配器，协议 SDK 依赖不会泄漏到 AgentScope 实验内部。

## 验证命令

```bash
./mvnw -f labs/pom.xml -pl agentscope-lab -Dtest=McpEnterpriseRegistryTest test
./mvnw -f labs/pom.xml -pl protocol-interop-lab -Dtest=McpInteroperabilityTest test
```

## 外部验证边界

本地测试验证 Java SDK 的协议行为、受管地址选择，以及目录注册与调用权限的分离，不代表已建立生产信任。OAuth、mTLS、DNS 与网络策略、重定向检查、对象级授权、Schema 审批、远程写 Tool 和失败恢复，仍需要在目标 MCP Server 和公司环境验证。
