# 查询 Tool 验证

Status: VERIFIED_MINIMUM_PRIVILEGE_READ_TOOL


## 已验证

- `QUERY_KNOWLEDGE` is server-owned, read-only and accepts only a bounded `question` argument.
- Unknown tools, missing arguments and additional arguments are rejected before any executor is called.
- The HTTP executor obtains a task-derived token for `knowledge-service` with `knowledge:answer` scope.
- Knowledge results are mapped to a bounded `ToolObservation`; raw HTTP responses and provider objects do not enter the planner.
- Downstream timeouts and service URLs are typed application configuration, not model arguments.

## 验证命令

```bash
./mvnw -pl services/ticket-agent-service \
  -Dtest=BusinessToolCatalogTest,HttpKnowledgeReadToolExecutorTest,TicketAgentOrchestratorTest test
```

## 生产接入边界

The development HMAC token provider must be replaced by company IdP or Token Exchange. Shared environment verification must use real Knowledge ACL data, signed short-lived tokens and bounded result sizes.
