# Agent 受控运行验证

Status: VERIFIED_CONTROLLED_AGENT_STATE_MACHINE


## 已验证

- Agent Task 显式区分 `ACCEPTED`、`RUNNING`、`WAITING_CONFIRMATION`、`EXECUTING`、`EXECUTION_UNCERTAIN`、`COMPLETED`、`REJECTED` 和 `FAILED` 状态。
- 编排器只接受 `USE_TOOL`、`FINISH` 和 `REFUSE` 三类规划结果，并限制最大执行步数。
- 只读 Tool 在进入下一轮规划前记录观察结果；写 Tool 在绑定任务版本的确认单处暂停。
- 规划元数据保留模型、用量和结束原因，Spring AI 响应类型不会进入应用层接口。
- 审计只记录低敏感度的决策元数据和任务状态变化，不记录 Prompt 正文或客户内容。

## 验证命令

```bash
./mvnw -pl services/ticket-agent-service \
  -Dtest=AgentTaskStateTest,TicketAgentOrchestratorTest,SpringAiTicketAgentPlannerPromptTest test
```

## 生产接入边界

测试配置把仓储和审计状态保存在内存中；运行配置则通过 JDBC/Flyway 持久化任务快照、版本、确认决策和审计事件。启用生产写 Tool 前，公司部署仍要验证目标数据库并发、Worker 归属、恢复和容量。
