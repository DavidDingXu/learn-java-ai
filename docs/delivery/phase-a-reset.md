# Phase A Reset Delivery Record

Date: 2026-07-12

Implementation commit: `e82c967`

## Delivered

- Replaced the obsolete demo reactor with four explicit mainline modules.
- Added three bootable HTTP applications with non-conflicting ports and a `local-lite` profile.
- Exposed Actuator health only; `/actuator/env` remains unavailable.
- Added a dependency-light Eval Runner entry point.
- Added an independent Java 8 client build and compatibility test.
- Added an isolated labs reactor for Spring AI Alibaba, LangChain4j and AgentScope.
- Added macOS/Linux and Windows verification entry points.
- Added repository contracts that reject obsolete modules, framework dependency leakage and unsupported infrastructure dependencies.

## Verification Environment

| Item | Value |
|---|---|
| Operating system | macOS |
| Main compile JDK | JDK 26.0.1 |
| Main bytecode target | Java 21 via `--release 21` |
| Legacy client JDK | Oracle JDK 8u481 |
| Maven Wrapper | 3.9.14 |
| Docker | Not used |
| Model key | Not used |
| External business network | Not used |

The main build result proves Java 21 bytecode compilation on JDK 26. It does not replace a CI run on a real JDK 21 JVM.

## Commands

```bash
JAVA_AI_MAIN_JAVA_HOME=/path/to/jdk-26 \
JAVA_AI_JDK8_HOME=/path/to/full-jdk8 \
JAVA_AI_REQUIRE_COLUMN_TESTS=1 \
scripts/verify-unit.sh
```

The verified run completed these boundaries:

- project and workspace Node contracts;
- root Java 21 reactor;
- independent labs reactor;
- independent Java 8 client.

The three Spring Boot tests started random HTTP ports, asserted health `UP`, asserted `local-lite`, asserted external integrations disabled and verified that Actuator env returned 404.

## Deliberately Not Delivered

- No real model or Provider configuration.
- No document upload, parser, embedding or vector retrieval.
- No PostgreSQL, pgvector, Redis, Kafka, MinIO, Flyway or Testcontainers dependency.
- No customer question endpoint, ticket workflow, Tool, Agent or human-confirmation flow.
- No formal Customer Web application.
- No production deployment, database migration, model evaluation or end-to-end report.
- No real Windows execution evidence; PowerShell behavior is contract-checked on macOS and still requires a Windows run before release.

## Next Replacement Boundary

The next vertical slice may add a model-backed Knowledge Service use case. It must preserve the service ownership and build boundaries, keep default tests free of model network calls, and store real model evidence separately from deterministic protocol fixtures.
