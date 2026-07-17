# ADR 0001: Use Spring AI as the Mainline Framework

Status: Accepted

Date: 2026-07-13

## Context

The target readers and expected delivery teams already use Spring Boot. The main business path needs model calls, structured output, streaming, RAG, Tool Calling, observations and MCP without replacing the surrounding Java application architecture.

The project also evaluates Spring AI Alibaba, LangChain4j and AgentScope. Putting all four frameworks in the main reactor would make dependency conflicts, runtime behavior and ownership harder to reason about. A framework comparison is useful only when the same business interface and dataset are kept stable.

Direct Provider SDK usage was also considered. It gives early access to vendor-specific features but pushes authentication, configuration, retries, metadata mapping and migration cost into business code.

## Decision

Use Spring AI 2.0.x with Spring Boot 4.1.x as the mainline AI framework in the Java 21 services.

Spring AI 2.0.0 is a GA release, not a milestone or release candidate. Its Boot 4 baseline, unified tool-calling advisor, immutable options and structured-output validation direction match the codebase being built here. Existing Spring Boot 3 applications stay on the supported Spring AI 1.1.x line until their platform upgrade is approved; they integrate with this system through versioned HTTP contracts rather than sharing framework classes.

- Spring AI types remain inside infrastructure adapters.
- Application ports use business language, such as a knowledge-answer model or ticket-planning model.
- The project does not create a universal model gateway and does not reduce Chat, RAG, Tool and Agent behavior to `String -> String`.
- Provider SDKs may be used behind a dedicated adapter only when Spring AI cannot expose a required capability and the gap is documented.
- Spring AI Alibaba, LangChain4j and AgentScope remain in the independent `labs` reactor. Candidate implementations use the same business interfaces and evaluation rules; production comparison still requires the same dataset and target environment.

## Consequences

Positive:

- Spring Boot teams can reuse familiar configuration, dependency management, testing and observability practices.
- Mainline services keep one coherent application architecture instead of switching structure for each framework.
- Framework migration chapters can measure real differences in code, behavior and maintenance cost.

Costs:

- Some Provider-specific features may lag behind the vendor SDK.
- Spring AI upgrades require compatibility checks against Spring Boot and the selected Provider.
- Business ports and framework adapters require more discipline than calling a client directly from a Controller.

## Replacement Conditions

Revisit this decision when at least one of these conditions is true:

- A required Provider capability cannot be implemented through Spring AI or a narrow adapter without losing essential behavior.
- The company already operates a governed AI platform whose contract is the required integration boundary.
- A different Java framework produces a materially better result on the same business interface and dataset, and the migration cost is acceptable.
- The selected Spring Boot and Spring AI versions no longer receive the security or compatibility support required by the deployment environment.

Changing the mainline requires a new ADR, dependency-tree evidence, regression results and a rollback path. Popularity or API preference alone is not enough.
