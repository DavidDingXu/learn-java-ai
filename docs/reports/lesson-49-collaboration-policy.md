# 多 Agent 在线协作与失败转人工

Status: VERIFIED_LIVE_PROVIDER_AND_FAILURE_PATH


## 读者入口

- `MultiAgentCollaborationApplication`：两个真实 ReActAgent 并行调用隔离的只读 Tool，第三个 ReActAgent 汇总结果。
- `MultiAgentFailureApplication`：一个必要专家失败后，协调器跳过汇总并返回 `HUMAN_REQUIRED`。

## 已观察结果

```text
decision=MULTI_AGENT
status=COMPLETED
policy-agent=退款审核通过后一到五个工作日原路到账
ticket-agent=T-100 OPEN
summary=工单是否完成退款审核仍缺少事实；审核通过后预计一到五个工作日到账，建议人工核对审核状态
```

失败入口返回 `HUMAN_REQUIRED`，原因包含 `ticket-agent: ticket service unavailable`，预设汇总答案没有执行。

`CollaborationPolicy` 另外守住拆分前决策：依赖任务保留单 Agent，多个独立只读任务允许拆分，含副作用任务生成 AgentScope `RequireUserConfirmEvent`。

## 外部验证边界

在线入口中的多个 Agent 运行在同一 JVM，业务事实来自本地只读 Tool。跨服务接入仍需持久任务、消息投递、确认与任务绑定、委托身份、预算、超时、版本化结果契约和人工工作台。
