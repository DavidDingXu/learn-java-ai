# ai-a2a-demo

这个模块演示 A2A 在 Java AI 工程里的最小边界：Agent Card、Skill、Task 状态、流式事件和回调。

示例场景是“工单 Agent 对外暴露处理建议 Skill”：

- `AgentCard` 暴露 Agent 名称、入口和 Skill 清单。
- `AgentSkill` 描述 `ticket.advice` 的输入 schema。
- `AgentTask` 表达一次远程 Agent 调用的状态化任务。
- `TaskEvent` 模拟流式任务状态更新。
- `AgentArtifact` 表达任务完成后的结构化产物。
- `TaskCallback` 模拟异步消费者收到最终任务状态。
- `A2aClient#getTask` 模拟调用方按 `taskId` 查询任务。
- `INPUT_REQUIRED` 表达远程 Agent 需要调用方补充人工确认或额外输入。
- `PushNotificationConfig` 表达任务状态变化后的回调订阅配置。

## 核心类

```text
HelpdeskAgentSkillServer // 模拟 A2A Server，暴露工单建议 Skill
A2aClient                // 模拟调用方 Agent / 系统
AgentCard                // Agent 对外能力卡片
AgentSkill               // 可调用能力单元
AgentTask                // 状态化任务
TaskEvent                // 流式状态事件
AgentArtifact            // 任务产物
TaskCallback             // 异步任务完成通知
PushNotificationConfig   // 任务状态变化后的回调配置
```

## 运行测试

```bash
mvn -pl ai-a2a-demo test
```

重点测试：

```text
agentCardPublishesStableSkillsAndEndpoint
clientCreatesTaskAndReceivesStatusEvents
serverRejectsUnknownSkillWithoutCreatingBusinessArtifact
serverRejectsInvalidTaskInputAndKeepsFailureEvents
clientCanLoadTaskStateAfterCreation
callbackReceivesFinalTaskStateForAsyncConsumer
taskCanPauseWithInputRequiredAndResumeAfterHumanInput
pushNotificationConfigReceivesInputRequiredAndFinalState
inputRequiredTaskFailsWhenHumanRejectsTheAction
```

## 重点边界

A2A 解决的是 Agent 能力怎么被其他 Agent 或系统发现、调用和追踪。

这个模块没有启动真实远程网络服务。它用 Java 对象模拟 Agent Card、Task、Event、Callback 的合同，重点看清远程 Agent 调用里的状态机：

```text
SUBMITTED -> WORKING -> COMPLETED
SUBMITTED -> WORKING -> INPUT_REQUIRED -> COMPLETED
SUBMITTED -> WORKING -> INPUT_REQUIRED -> FAILED
SUBMITTED -> FAILED
```

`INPUT_REQUIRED` 不能被当成普通失败。它表示远程 Agent 暂停在一个需要调用方参与的节点，例如高风险工单需要人工确认。调用方通过 `submitInput` 补充输入后，任务继续推进到 `COMPLETED` 或 `FAILED`。

`PushNotificationConfig` 只表达回调配置和订阅关系。生产项目里还要补回调鉴权、签名、重试、幂等和死信处理。

它不替代：

- MCP 工具调用
- Java 业务权限
- 输入参数校验之外的业务风控
- 任务幂等
- 回调鉴权
- trace 和审计
