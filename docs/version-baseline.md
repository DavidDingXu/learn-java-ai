# Version Baseline

Date locked: 2026-07-14

These versions are the repository's compatibility baseline. They are not a claim that every company project should use the newest available release.

| Area | Version | Source of truth |
|---|---:|---|
| Main Java bytecode | 21 | root `pom.xml` |
| Spring Boot | 4.1.0 | root `pom.xml` |
| Spring AI BOM | 2.0.0 | root `pom.xml` |
| Maven Compiler Plugin | 3.15.0 | root and independent POMs |
| Maven Enforcer Plugin | 3.6.3 | root and independent POMs |
| Maven Surefire/Failsafe | 3.5.6 | root and independent POMs |
| JaCoCo | 0.8.15 | root `pom.xml` |
| ArchUnit | 1.4.2 | root `pom.xml` |
| Maven Wrapper | 3.9.14 | `.mvn/wrapper/maven-wrapper.properties` |
| Spring AI Alibaba BOM | 1.1.2.3 | `labs/pom.xml` |
| LangChain4j BOM | 1.17.2 | `labs/pom.xml` |
| AgentScope BOM | 2.0.0 | `labs/pom.xml` |
| MCP Java SDK | 2.0.0 | `labs/pom.xml` and `labs/protocol-interop-lab/pom.xml` |
| A2A Java SDK | 1.1.0.Final | `labs/pom.xml` and `labs/protocol-interop-lab/pom.xml` |
| Labs JUnit | 6.0.3 | `labs/pom.xml` |
| Legacy Java target | 8 | `integrations/jdk8-client/pom.xml` |
| Legacy JUnit | 5.11.4 | `integrations/jdk8-client/pom.xml` |

## Rules

- Main services inherit versions from the root dependency and plugin management. Do not repeat versions in child POMs without a compatibility reason.
- Each labs child imports only its own framework BOM. A labs dependency cannot enter the main reactor.
- The Java 8 client keeps its own plugin and test versions and never inherits Java 21 build settings.
- Provider, database, vector-store and object-storage dependencies are added only when the corresponding vertical slice exists and has a verification plan.
- Patch upgrades require unit, contract and applicable live evidence. Major upgrades require an ADR with dependency changes, behavior differences and rollback conditions.
- CI runs the main reactor on a real JDK 21. Local compilation on JDK 26 with `--release 21` remains useful but is not equivalent runtime evidence.
- Stable GA versions are preferred over milestone and release-candidate builds. `Spring AI Alibaba 2.0.0-M1.1` is therefore not used even though Maven metadata lists it as the latest artifact.
- `Spring AI Alibaba 1.1.2.3`, `LangChain4j 1.17.2`, `AgentScope 2.0.0`, MCP Java SDK `2.0.0` and A2A Java SDK `1.1.0.Final` were resolved from Maven Central and verified by the labs reactor on the lock date.
