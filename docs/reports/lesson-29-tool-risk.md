# Tool 风险分级验证

Status: VERIFIED_SERVER_SIDE_RISK_CLASSIFICATION


## 已验证

- Tool 的副作用、风险级别和所需角色由服务端目录策略定义，不接受模型提供的值。
- `QUERY_KNOWLEDGE` is `READ_ONLY`; `ADD_INTERNAL_NOTE` and `REQUEST_MANUAL_REVIEW` are `LOW`; `ASSIGN_QUEUE` is `MEDIUM`; `ISSUE_REFUND` is `HIGH`.
- 所有写 Tool 都会在执行前暂停，并生成结构化确认请求。
- 退款与队列参数在生成确认单前仍要经过确定性的业务校验。

## 验证命令

```bash
./mvnw -pl services/ticket-agent-service \
  -Dtest=BusinessToolCatalogTest,TicketAgentOrchestratorTest,AgentTaskStateTest test
```

## 生产接入边界

示例静态矩阵需要按公司的动作归属、金额分段、数据敏感度、可逆性和审批规则替换或扩展。模型置信度不能降低最终风险等级。
