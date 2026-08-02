# 可观测性验证

Status: VERIFIED_LOW_CARDINALITY_AGENT_METRICS


## 已验证

- Ticket Agent 通过应用层遥测接口记录规划次数、总 Token 分布以及 Tool 耗时与结果。
- 指标标签只包含决策、结束原因、Tool 和结果；任务、Prompt、问题、租户和模型不会成为指标标签。
- 只读与写 Tool 的成功、拒绝、结果不确定和本地失败都在所属应用边界记录。
- 遥测失败不会改变 Agent 规划结果，也不会覆盖已经持久化的 Tool 结果。
- 运行配置开放健康检查和 Prometheus，测试配置只开放健康检查。
- Knowledge operations use Micrometer Observation and can return the current trace ID; Agent business IDs remain in the audit model rather than metric tags.

## 验证命令

```bash
./mvnw -pl services/ticket-agent-service \
  -Dtest=MicrometerAgentTelemetryTest,TicketAgentOrchestratorTest,ToolConfirmationServiceTest,TicketAgentServiceApplicationTest test
```

## 生产接入边界

No current test proves an end-to-end BFF-to-Knowledge-to-Agent-to-JDK8 Trace or automatic Trace-to-audit correlation. Company deployment must provide an OTel exporter or agent, protected management network, dashboards, SLOs, alert routes and a versioned model-price source. Local metrics do not establish capacity or cost targets.
