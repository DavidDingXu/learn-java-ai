# Lesson 51 A2A Task Boundary Evidence

Status: VERIFIED_CLIENT_INTEROPERABILITY_AND_LOCAL_TASK_CONTRACT

Implementation baseline: `release-hardening-2026-07-14`

## Verified

- Duplicate submissions with the same idempotency key and fingerprint reuse one task.
- A changed fingerprint under the same key is rejected.
- Status transitions are monotonic; terminal states cannot regress and uncertain delivery can enter UNKNOWN.
- A2A Java SDK `1.1.0.Final` discovers a standard Agent Card, validates an allowlisted skill and sends a message over JSON-RPC.
- The official client maps the response to a completed A2A Task with an artifact.
- AgentScope `A2aTaskCoordinator` remains the business state boundary; SDK enums are not persisted as the domain contract.

## Verification

```bash
./mvnw -f labs/pom.xml -pl protocol-interop-lab,agentscope-lab test
```

## External Boundary

The protocol test uses the official A2A client against a local standards-based Agent Card and JSON-RPC service. It does not prove production authentication, streaming, push notifications, long-running remote execution, callbacks or Inbox/Outbox persistence; those require the target agent and network environment.
