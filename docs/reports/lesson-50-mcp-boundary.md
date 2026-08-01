# MCP 工具调用边界验证

Status: VERIFIED_PROTOCOL_INTEROPERABILITY

## 已验证

- MCP Java SDK `2.0.0` runs against the `2025-11-25` protocol baseline.
- The official synchronous client and server complete initialize, `tools/list` and `tools/call` over Streamable HTTP.
- `EnterpriseMcpClient` exposes only locally allowlisted tools and rejects tools without the approved read-only contract.
- The discovery receipt records the negotiated protocol, server identity and registered tools.
- `EnterpriseMcpRegistry` resolves endpoints from a managed `serverId -> HTTPS endpoint` map; callers cannot pair an approved ID with an arbitrary URL.
- A discovery batch is fully validated before registration, so a later invalid tool cannot leave an earlier tool registered.
- The AgentScope adapter uses one `Toolkit` for external-tool registration and `PermissionEngine` decisions; a registered tool without a local permission rule is denied.
- AgentScope tool registration remains a separate business-runtime adapter so the protocol SDK dependency does not leak into the AgentScope lab.

## 验证命令

```bash
./mvnw -f labs/pom.xml -pl agentscope-lab -Dtest=McpEnterpriseRegistryTest test
./mvnw -f labs/pom.xml -pl protocol-interop-lab -Dtest=McpInteroperabilityTest test
```

## 外部验证边界

The local tests prove Java SDK protocol behavior, managed endpoint selection and the separation between catalog registration and invocation permission, not production trust. OAuth, mTLS, DNS and network policy, redirect enforcement, object-level authorization, schema approval, remote write tools and failure recovery still require the target MCP server and company environment.
