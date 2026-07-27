# Lesson 37 Production Resilience Evidence

Status: VERIFIED_OPERATION_SPECIFIC_RESILIENCE

Implementation commit: `b8fd7c46e2329e48cdbdbedfcc58e8097afe306d`

## Verified

- Synchronous Knowledge model resilience remains scoped to the business model port and does not apply write retry semantics.
- The Knowledge model keeps one explicit Resilience4j retry policy while both Knowledge and Ticket disable OpenAI SDK retries, so retry ownership is not duplicated.
- A real Spring context and local Provider fixture verify that the Knowledge adapter is proxied, one transient 503 is retried, and a slow response is timed out.
- Customer consultation keeps its channel-level fixed-window limiter.
- The BFF streaming client applies an idle timeout and preserves cancellation; the Knowledge service propagates subscriber cancellation to its model stream.
- Ticket Agent Run uses a fair semaphore admission boundary with a configurable limit.
- Capacity exhaustion returns HTTP 429 and stable `AGENT_RUN_CAPACITY_EXCEEDED` code.
- Permits are released after success and exception; Tool writes still classify explicit rejection and uncertain remote outcomes separately.

## Verification

```bash
./mvnw -pl services/knowledge-service,services/ticket-agent-service,apps/customer-bff \
  -Dtest=ModelResilienceContractTest,ProviderProtocolFixtureTest,TicketAgentModelConfigurationTest,SemaphoreAgentRunAdmissionTest,AgentTaskExceptionHandlerTest,InMemoryFixedWindowConsultationRateLimiterTest,WebClientKnowledgeAnswerStreamClientTest,StreamingKnowledgeAnswerServiceTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

## Production Boundary

The semaphore is per instance and the BFF limiter is in-memory. The current stream has no end-to-end deadline, and the streaming model adapter does not use the synchronous model's Resilience4j annotations. The fixture does not exercise the circuit breaker's complete closed-open-half-open recovery sequence. Company rollout must size from load tests and add shared tenant/global quotas only where required; write Tool retry remains tied to business idempotency and result query.
