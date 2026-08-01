# Confirmation Graph Verification

Status: VERIFIED_ISOLATED_GRAPH


## Verified

- A real Spring AI Alibaba `StateGraph` models prepare, wait-for-confirmation and execute nodes.
- Business confirmation state remains explicit and is not hidden in model messages.
- Tests cover the confirmation route and state output.

## Verification

```bash
./mvnw -f labs/pom.xml -pl spring-ai-alibaba-lab test
```

## External Boundary

The graph is an isolated flow comparison. Durable checkpoint storage, concurrent decisions and production recovery require target-environment integration tests.
