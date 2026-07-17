# ADR 0003: Keep Framework Migration Experiments Isolated

Status: Accepted

Date: 2026-07-14

## Context

The main services use Spring Boot 4.1 and Spring AI 2.0. The Spring AI Alibaba 1.1.2.x stable line is based on the Spring AI 1.1 and Spring Boot 3.5 generation. LangChain4j and AgentScope own separate model, tool, memory and protocol abstractions. Importing these dependencies into the main reactor would turn a business migration experiment into an application-wide dependency migration.

A migration decision needs executable comparisons, not API screenshots. Each experiment must therefore keep a business interface stable while replacing only the framework-facing adapter.

## Decision

Keep three independent labs under the separate labs reactor.

- Spring AI Alibaba lab owns DashScope options, domestic retrieval replacement metrics and one confirmation graph.
- LangChain4j lab owns AI Services, a tenant-scoped RAG adapter, a read-only tool loop, structured output and capability routing.
- AgentScope lab owns Tool permission mapping, human intervention policy, MCP external tool registration and the A2A task boundary.

The labs do not depend on production service implementation modules. They reproduce only the narrow business ports needed to demonstrate migration. Framework objects do not cross those ports.

Provider credentials and remote endpoints remain external configuration. Unit tests use deterministic models or local state and do not claim remote Provider, MCP or A2A interoperability.

## Consequences

Positive:

- Each framework dependency tree can be upgraded and rolled back independently.
- A migration can be evaluated by behavior, test effort and operational cost before touching production code.
- Mainline services keep one framework and one observability model.

Costs:

- Small business interfaces are repeated in labs.
- A successful experiment still needs a production ADR, integration tests and rollout plan.
- Cross-framework examples cannot share framework DTOs or helper libraries.

## Promotion Conditions

A lab implementation can enter a production service only when all conditions are met:

- It materially improves a measured business outcome on the same dataset or workflow.
- Its dependency graph is compatible with the target service or it is deployed as an isolated service.
- Authentication, authorization, observability, timeout, retry, idempotency and rollback behavior have production evidence.
- The owning team accepts the framework's upgrade and incident-response cost.

Passing lab unit tests is necessary evidence, but it is not a promotion decision.
