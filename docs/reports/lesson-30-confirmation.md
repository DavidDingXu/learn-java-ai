# Confirmation Verification

Status: VERIFIED_VERSION_BOUND_IDEMPOTENT_CONFIRMATION


## Verified

- Confirmation binds confirmation ID, action ID, tool, risk, role, normalized arguments, fingerprint, task version and expiry.
- The confirmation actor is derived from trusted JWT claims and must match tenant, `jdk8-crm` actor and required role.
- Stale task versions, expired confirmation, wrong tenant, wrong actor and missing role are rejected before tool execution.
- Decision idempotency is scoped by tenant, actor and subject and compares a request fingerprint.
- Duplicate identical decisions return the original receipt; same key with different content conflicts.

## Verification

```bash
./mvnw -pl services/ticket-agent-service \
  -Dtest=ToolConfirmationServiceTest,ConfirmationActorFactoryTest,AgentTaskWorkflowControllerTest test
```

## Production Boundary

Confirmation and idempotency state are in-memory only in tests. The runtime configuration uses JDBC/Flyway, durable unique constraints and optimistic task updates; an expired pending write is not automatically reclaimed without downstream reconciliation. High-risk domains may still require multi-party approval rather than a single confirmation.
