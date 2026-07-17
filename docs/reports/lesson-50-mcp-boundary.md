# Lesson 50 MCP Boundary Evidence

Status: VERIFIED_PROTOCOL_INTEROPERABILITY

Implementation baseline: `release-hardening-2026-07-14`

## Verified

- MCP Java SDK `2.0.0` runs against the `2025-11-25` protocol baseline.
- The official synchronous client and server complete initialize, `tools/list` and `tools/call` over Streamable HTTP.
- `EnterpriseMcpClient` exposes only locally allowlisted tools and rejects tools without the approved read-only contract.
- The discovery receipt records the negotiated protocol, server identity and registered tools.
- The AgentScope adapter uses one `Toolkit` for external-tool registration and `PermissionEngine` decisions; a registered tool without a local permission rule is denied.
- AgentScope tool registration remains a separate business-runtime adapter so the protocol SDK dependency does not leak into the AgentScope lab.

## Verification

```bash
./mvnw -f labs/pom.xml -pl agentscope-lab -Dtest=McpEnterpriseRegistryTest test
./mvnw -f labs/pom.xml -pl protocol-interop-lab -Dtest=McpInteroperabilityTest test
```

## External Boundary

The local tests prove Java SDK protocol behavior and the separation between catalog registration and invocation permission, not production trust. OAuth, mTLS, DNS and network policy, object-level authorization, schema approval, remote write tools and failure recovery still require the target MCP server and company environment.
