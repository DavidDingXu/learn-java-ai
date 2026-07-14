# ADR 0002: Separate Mainline, Labs, Java 8 and Web Builds

Status: Accepted

Date: 2026-07-12

## Context

The repository contains four different compatibility domains:

1. Java 21 Spring Boot services and the Eval Runner.
2. Framework migration experiments with independent dependency graphs.
3. A client that must compile and run on Java 8.
4. A Vue/TypeScript customer application.

A single reactor would allow experimental framework dependencies to leak into production services, force the Java 8 client to inherit Java 21 bytecode settings, and couple Node tooling to Maven lifecycle assumptions. A shared domain module would also encourage services to compile against each other's internal models instead of maintaining explicit contracts.

## Decision

Use four independent build boundaries.

### Main Reactor

The root `pom.xml` aggregates only:

- `services/knowledge-service`
- `services/ticket-agent-service`
- `apps/customer-bff`
- `quality/eval-runner`

It compiles with `maven.compiler.release=21` and owns the Spring Boot and Spring AI mainline versions.

### Framework Labs

`labs/pom.xml` is a separate reactor. Each child imports only its own framework BOM:

- `labs/spring-ai-alibaba-lab`
- `labs/langchain4j-lab`
- `labs/agentscope-lab`

The labs cannot become transitive dependencies of the main services.

### Java 8 Client

`integrations/jdk8-client/pom.xml` has no parent and compiles with `maven.compiler.release=8`. It is verified with a full JDK 8 containing both `java` and `javac`.

### Customer Web

`apps/customer-web` is an independent Node product. It does not enter a Maven reactor.

Services do not share a domain JAR. Cross-service reuse is limited to versioned OpenAPI, JSON Schema, error contracts and test fixtures stored under `contracts`.

## Consequences

Positive:

- Java compatibility is explicit and testable.
- Framework experiments cannot silently change the production dependency tree.
- The project can release or verify each product independently.
- Service contracts remain visible instead of being hidden by shared implementation classes.

Costs:

- Verification scripts must run multiple builds.
- Contract changes require provider and consumer tests rather than a single compiler error.
- Some DTO shapes may be repeated across generated or hand-maintained clients.

## Replacement Conditions

Merge build boundaries only when the products share the same runtime, release cadence and dependency policy, and the change does not weaken Java 8 compatibility or framework isolation.

Introduce a shared library only for a stable technical concern with no domain ownership, after proving that an HTTP/OpenAPI contract or local duplication is worse. Shared domain entities, persistence models and service-internal DTOs remain prohibited.
