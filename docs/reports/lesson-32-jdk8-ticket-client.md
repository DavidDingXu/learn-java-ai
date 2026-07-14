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
JAVA_HOME="$JAVA8_HOME" PATH="$JAVA8_HOME/bin:$PATH" \
  ./mvnw -f integrations/jdk8-client/pom.xml verify
```

Result: 6 Java 8 tests passed in the unified verification run.

## Production Boundary

Company integration must replace base URL discovery, TLS, proxy and token acquisition, and must confirm that any shared HTTP SDK also disables unsafe write retries. Callback delivery is optional and cannot replace task result queries.
