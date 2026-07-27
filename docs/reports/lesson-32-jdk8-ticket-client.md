# Lesson 32 JDK8 Ticket Client Evidence

Status: VERIFIED_JAVA8_HTTP_CLIENT_CONTRACT

Implementation commit: `44713c1a26c1e9a4d47354032db8c3e32d5e0b49`

## Verified

- The JDK8 module compiles with a full Temurin 8 toolchain and does not depend on the Java 21 reactor.
- The client uses bounded task IDs, a connection pool, connect/response timeouts and disables automatic HTTP retries.
- Bearer tokens come from an `AccessTokenProvider`; idempotency stays in the HTTP header.
- Read transport errors and write outcome-unknown errors have different exception types.
- Structured API errors preserve status, code and retryability without exposing arbitrary untrusted bodies.
- The Agent Task OpenAPI contains task read, confirmation, audit, states and error responses used by the legacy integration.

## Verification

```bash
./mvnw -f integrations/jdk8-client/pom.xml verify
```

该独立 POM 要求 Maven 由 JDK8 运行。在 IDE 或 Maven 运行配置中选择 JDK8 即可，无需配置项目专用环境变量。

Result: 7 Java 8 tests passed in the latest focused verification run.

## Production Boundary

Company integration must replace base URL discovery, TLS, proxy and token acquisition, and must confirm that any shared HTTP SDK also disables unsafe write retries. Callback delivery is optional and cannot replace task result queries.
