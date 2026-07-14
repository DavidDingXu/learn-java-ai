# Milestone 52 Framework Migration And Column Completion

Status: VERIFIED_LOCAL_RELEASE_BASELINE

Implementation baseline: `release-hardening-2026-07-14`

## Scope

- Spring AI 2.0 remains the production mainline.
- Spring AI Alibaba, LangChain4j, AgentScope and protocol interoperability stay in an isolated labs reactor.
- The same business contracts are used to compare provider, retrieval, graph, AI Services, Tool, runtime, MCP and A2A boundaries.
- MCP Java SDK `2.0.0` and A2A Java SDK `1.1.0.Final` exercise real local protocol interoperability instead of only local schema objects.
- Ticket Agent runtime persists tasks, audit events and confirmation decisions with PostgreSQL/Flyway; in-memory adapters remain test-only.
- Dataset gates require 50 retrieval, 30 Agent and 30 security teaching cases.
- ADR 0003 defines promotion and rollback conditions.

## Verification

```bash
./mvnw -f labs/pom.xml verify
bash scripts/release-gate.sh
```

## Evidence Boundary

Local tests prove framework API usage, dependency isolation, MCP/A2A client interoperability and business boundary behavior. Live DashScope, production MCP/A2A authentication, target PostgreSQL capacity and company infrastructure require credentials and target-environment evidence before production promotion.
