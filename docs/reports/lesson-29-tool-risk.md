# Tool Risk Verification

Status: VERIFIED_SERVER_SIDE_RISK_CLASSIFICATION


## Verified

- Tool effect, risk and required role are catalog policy, never model-supplied fields.
- `QUERY_KNOWLEDGE` is `READ_ONLY`; `ADD_INTERNAL_NOTE` and `REQUEST_MANUAL_REVIEW` are `LOW`; `ASSIGN_QUEUE` is `MEDIUM`; `ISSUE_REFUND` is `HIGH`.
- All write tools stop before execution and produce a structured confirmation request.
- Refund and queue arguments have deterministic business validation before confirmation.

## Verification

```bash
./mvnw -pl services/ticket-agent-service \
  -Dtest=BusinessToolCatalogTest,TicketAgentOrchestratorTest,AgentTaskStateTest test
```

## Production Boundary

The sample static matrix must be replaced or extended with company action ownership, amount bands, data sensitivity, reversibility and approval rules. Model confidence must not lower the resulting risk.
