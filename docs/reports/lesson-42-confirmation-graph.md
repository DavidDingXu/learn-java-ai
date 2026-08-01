# 人工确认 Graph 路由验证

Status: VERIFIED_ISOLATED_GRAPH


## 已验证

- A real Spring AI Alibaba `StateGraph` models prepare, wait-for-confirmation and execute nodes.
- Business confirmation state remains explicit and is not hidden in model messages.
- Tests cover the confirmation route and state output.

## 验证命令

```bash
./mvnw -f labs/pom.xml -pl spring-ai-alibaba-lab test
```

## 外部验证边界

The graph is an isolated flow comparison. Durable checkpoint storage, concurrent decisions and production recovery require target-environment integration tests.
