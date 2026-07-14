# Lesson 36 Observability Evidence

Status: VERIFIED_LOW_CARDINALITY_AGENT_METRICS

Implementation commit: `b8fd7c46e2329e48cdbdbedfcc58e8097afe306d`

## Verified

- Ticket Agent records plan count, total-token distribution and Tool duration/outcome through an application telemetry port.
- Meter tags are limited to decision, finish reason, tool and outcome; task, prompt, question, tenant and model are not metric tags.
- Read and write Tool success, rejection, uncertain result and local failure are recorded at the owning application boundary.
- Prometheus is exposed only by the shared-dev profile; local-lite continues to expose only health.
- Business audit IDs remain separate from sampled Trace and aggregated metrics.

## Verification

```bash
./mvnw -pl services/ticket-agent-service \
  -Dtest=MicrometerAgentTelemetryTest,TicketAgentServiceApplicationTest test
```

## Production Boundary

Company deployment must provide an OTel exporter or agent, protected management network, dashboards, SLOs, alert routes and a versioned model-price source. Local metrics do not establish capacity or cost targets.
