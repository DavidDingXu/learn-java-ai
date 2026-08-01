# Observability Verification

Status: VERIFIED_LOW_CARDINALITY_AGENT_METRICS


## Verified

- Ticket Agent records plan count, total-token distribution and Tool duration/outcome through an application telemetry port.
- Meter tags are limited to decision, finish reason, tool and outcome; task, prompt, question, tenant and model are not metric tags.
- Read and write Tool success, rejection, uncertain result and local failure are recorded at the owning application boundary.
- Telemetry failures do not change an Agent plan or an already persisted Tool result.
- Runtime configuration exposes health and Prometheus; test configuration exposes health only.
- Knowledge operations use Micrometer Observation and can return the current trace ID; Agent business IDs remain in the audit model rather than metric tags.

## Verification

```bash
./mvnw -pl services/ticket-agent-service \
  -Dtest=MicrometerAgentTelemetryTest,TicketAgentOrchestratorTest,ToolConfirmationServiceTest,TicketAgentServiceApplicationTest test
```

## Production Boundary

No current test proves an end-to-end BFF-to-Knowledge-to-Agent-to-JDK8 Trace or automatic Trace-to-audit correlation. Company deployment must provide an OTel exporter or agent, protected management network, dashboards, SLOs, alert routes and a versioned model-price source. Local metrics do not establish capacity or cost targets.
