# Lesson 37 Production Resilience Evidence

Status: VERIFIED_OPERATION_SPECIFIC_RESILIENCE

Implementation commit: `b8fd7c46e2329e48cdbdbedfcc58e8097afe306d`

## Verified

- Knowledge model resilience remains scoped to the business model port and does not apply write retry semantics.
- Customer consultation keeps its channel-level fixed-window limiter.
- Ticket Agent Run uses a fair semaphore admission boundary with a configurable limit.
- Capacity exhaustion returns HTTP 429 and stable `AGENT_RUN_CAPACITY_EXCEEDED` code.
- Permits are released after success and exception; Tool writes still classify explicit rejection and uncertain remote outcomes separately.

## Verification

```bash
./mvnw -pl services/knowledge-service,services/ticket-agent-service,apps/customer-bff \
  -Dtest=ModelResilienceContractTest,SemaphoreAgentRunAdmissionTest,AgentTaskExceptionHandlerTest,InMemoryFixedWindowConsultationRateLimiterTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

## Production Boundary

The semaphore is per instance and the BFF limiter is in-memory. Company rollout must size from load tests and add shared tenant/global quotas only where required; write Tool retry remains tied to business idempotency and result query.
