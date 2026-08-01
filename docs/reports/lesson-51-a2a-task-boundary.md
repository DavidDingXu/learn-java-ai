# A2A 任务边界验证

Status: VERIFIED_CLIENT_INTEROPERABILITY_AND_LOCAL_TASK_CONTRACT

## 已验证

- The server computes a normalized, length-prefixed SHA-256 request fingerprint from tenant and business request fields; callers do not supply a trusted hash.
- A focused test keeps adjacent field boundaries unambiguous instead of relying on delimiter-based concatenation.
- Duplicate submissions with the same tenant, idempotency key and request reuse one task; a changed request under the same key is rejected.
- Identical idempotency keys in different tenant namespaces create isolated tasks.
- Status transitions are monotonic; terminal states cannot regress and uncertain delivery can enter UNKNOWN before a confirmed terminal result arrives.
- A repeated terminal callback is idempotent only when status and receipt match; conflicting terminal results are rejected as protocol conflicts.
- Unknown task IDs are rejected without creating local state.
- A2A Java SDK `1.1.0.Final` discovers a standard Agent Card, validates an allowlisted skill and sends a message over JSON-RPC.
- The official client maps the response to a completed A2A Task with an artifact.
- AgentScope `A2aTaskCoordinator` remains the business state boundary; SDK enums are not persisted as the domain contract.

## 验证命令

```bash
./mvnw -f labs/pom.xml -pl protocol-interop-lab,agentscope-lab test
```

## 外部验证边界

The protocol test uses the official A2A client against a local standards-based Agent Card and JSON-RPC service. It covers discovery, message sending and completed-task mapping, not remote status queries. It does not prove production authentication, streaming, push notifications, long-running remote execution, cancellation, callbacks or Inbox/Outbox persistence; those require the target agent and network environment.
