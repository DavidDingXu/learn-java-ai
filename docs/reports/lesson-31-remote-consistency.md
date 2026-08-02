# 远程写操作一致性验证

Status: VERIFIED_REMOTE_OUTCOME_CLASSIFICATION


## 已验证

- 确认通过的任务会在远程写入前进入 `EXECUTING` 状态。
- 旧系统请求使用带版本的 SHA-256 幂等键，输入按长度编码租户和动作字段；原始动作 ID 仍保留在请求和审计记录中。
- 有效的 2xx 回执完成任务；明确的 4xx 拒绝会让任务失败，且不会自动重试。
- 超时、5xx、空响应、格式错误和动作 ID 不一致都会进入 `EXECUTION_UNCERTAIN`。
- 未预期的本地运行时错误会让任务从 `EXECUTING` 转为 `FAILED`，并继续交给统一异常处理。
- 审计事件关联 Agent 动作 ID 与下游业务审计 ID，但不保存原始响应正文。

## 验证命令

```bash
./mvnw -pl services/ticket-agent-service \
  -Dtest=ToolConfirmationServiceTest,HttpLegacyWriteToolExecutorTest,AgentTaskStateTest test
```

## 生产接入边界

最终业务系统必须在写操作的同一本地事务中持久化 action ID、请求指纹和执行结果。公司部署还要补充结果查询、卡住任务扫描、未知结果对账和告警。
