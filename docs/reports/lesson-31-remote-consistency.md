# Lesson 31 Remote Consistency Evidence

Status: VERIFIED_REMOTE_OUTCOME_CLASSIFICATION

Implementation commit: `44713c1a26c1e9a4d47354032db8c3e32d5e0b49`

## Verified

- Approved tasks enter `EXECUTING` before the remote write begins.
- The legacy request uses `tool:{tenant}:{actionId}` as a stable action idempotency key.
- Valid 2xx receipts complete the task; explicit 4xx rejection fails it without automatic retry.
- Timeout, 5xx, empty response, malformed response and action ID mismatch enter `EXECUTION_UNCERTAIN`.
- Unexpected local runtime errors move the task out of `EXECUTING` to `FAILED` and remain visible to error handling.
- Audit events link Agent action ID to the downstream business audit ID without storing raw response bodies.

## Verification

```bash
./mvnw -pl services/ticket-agent-service \
  -Dtest=ToolConfirmationServiceTest,HttpLegacyWriteToolExecutorTest,AgentTaskStateTest test
```

## Production Boundary

The final business system must durably store action ID, request fingerprint and result in the same local transaction as its write. Company deployment also needs result query, stuck-execution scanning, uncertain-outcome reconciliation and alerting.
