# Tool Trust Boundaries Verification

Status: VERIFIED_SERVER_OWNED_TOOL_POLICY


## Verified

- User objective, business context and tool output are separated as untrusted prompt regions.
- Model proposals cannot supply tenant, actor, role, risk, endpoint, token or idempotency policy.
- The server normalizes permitted arguments and creates a deterministic SHA-256 tool-call fingerprint.
- Queue codes, refund amount and currency are checked by deterministic Java policy after structured output parsing.
- Tool output remains data; every next-step proposal passes through the same catalog and state machine again.

## Verification

```bash
./mvnw -pl services/ticket-agent-service \
  -Dtest=BusinessToolCatalogTest,SpringAiTicketAgentPlannerPromptTest,HttpLegacyWriteToolExecutorTest test
```

## Production Boundary

Prompt partitioning is not a standalone injection defense. Company rollout must add security datasets for user input, business context and tool-output injection, and must keep authorization in downstream services.
