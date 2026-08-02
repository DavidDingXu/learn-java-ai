# 查询 Tool 验证

Status: VERIFIED_MINIMUM_PRIVILEGE_READ_TOOL


## 已验证

- `QUERY_KNOWLEDGE` is server-owned, read-only and accepts only a bounded `question` argument.
- 未知 Tool、缺失参数和额外参数都会在调用执行器前被拒绝。
- HTTP 执行器获取面向 `knowledge-service`、权限为 `knowledge:answer` 的任务级令牌。
- 知识查询结果会转换为字段受限的 `ToolObservation`，原始 HTTP 响应和 Provider 对象不会进入规划器。
- 下游超时和服务地址属于有类型的应用配置，不接受模型提供的值。

## 验证命令

```bash
./mvnw -pl services/ticket-agent-service \
  -Dtest=BusinessToolCatalogTest,HttpKnowledgeReadToolExecutorTest,TicketAgentOrchestratorTest test
```

## 生产接入边界

开发用 HMAC 令牌提供器不能进入生产，需要替换为公司 IdP 或 Token Exchange。共享环境验证应使用真实 Knowledge ACL 数据、已签名的短期令牌和有明确上限的返回结果。
