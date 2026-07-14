# Milestone 34 Controlled Ticket Agent

Status: VERIFIED_LOCAL_CONTROLLED_AGENT_LIVE_MODEL_SHARED_ENVIRONMENT_REQUIRED

## Implementation

Commit: `44713c1a26c1e9a4d47354032db8c3e32d5e0b49`

The milestone contains a bounded Agent task state machine, Spring AI 2.0 structured planning, server-owned Tool Catalog, delegated Knowledge read tool, risk-classified write tools, version-bound human confirmation, remote result classification, Java 8 task client, auditable OpenAPI and independent Agent evaluation.

## Verification

- Real model planner smoke passed with a structured `USE_TOOL / QUERY_KNOWLEDGE` decision and model usage metadata.
- Main reactor passed: Knowledge Service 85 tests, Ticket Agent Service 45 tests, Customer BFF 19 tests and Eval Runner 23 tests; 172 tests total.
- Isolated framework labs reactor built successfully.
- Independent Temurin 8 client verification passed 6 tests.
- Column and project contract verification passed 111 checks.
- Secret scan found no API key or supplied provider URL in tracked project content.

## External Boundary

Local verification does not prove production IdP, persistent Agent task/confirmation/audit storage, prepared external Knowledge index, durable Legacy Tool idempotency, uncertain-result reconciliation or end-to-end capacity. These must be verified in the company shared environment before enabling write tools.

## Tag Rule

`milestone-34-controlled-ticket-agent` must point to implementation commit `44713c1a26c1e9a4d47354032db8c3e32d5e0b49`. Documentation-only commits do not replace this implementation evidence point.
