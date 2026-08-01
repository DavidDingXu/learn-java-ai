# 远程写操作一致性验证

Status: VERIFIED_REMOTE_OUTCOME_CLASSIFICATION


## 已验证

- Approved tasks enter `EXECUTING` before the remote write begins.
- The legacy request uses a versioned SHA-256 key over length-prefixed tenant and action fields, while the original action ID remains in the request and audit records.
- Valid 2xx receipts complete the task; explicit 4xx rejection fails it without automatic retry.
- Timeout, 5xx, empty response, malformed response and action ID mismatch enter `EXECUTION_UNCERTAIN`.
- Unexpected local runtime errors move the task out of `EXECUTING` to `FAILED` and remain visible to error handling.
- Audit events link Agent action ID to the downstream business audit ID without storing raw response bodies.

## 验证命令

```bash
./mvnw -pl services/ticket-agent-service \
  -Dtest=ToolConfirmationServiceTest,HttpLegacyWriteToolExecutorTest,AgentTaskStateTest test
```

## 生产接入边界

The final business system must durably store action ID, request fingerprint and result in the same local transaction as its write. Company deployment also needs result query, stuck-execution scanning, uncertain-outcome reconciliation and alerting.
