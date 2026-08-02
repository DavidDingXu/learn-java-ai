# 多 Agent 协作策略验证

Status: VERIFIED_DETERMINISTIC_POLICY


## 已验证

- 工作单元是否独立、是否包含副作用，决定采用单 Agent、多 Agent 或人工介入执行。
- 存在副作用时生成真实 AgentScope `RequireUserConfirmEvent`。
- 测试覆盖单 Agent、多 Agent 和人工介入三条分支，并校验 AgentScope 确认事件类型。

## 验证命令

```bash
./mvnw -f labs/pom.xml -pl agentscope-lab test
```

## 外部验证边界

该策略不包含远程多 Agent 执行。生产接入还需要持久任务、确认与任务绑定、委托身份、预算、超时和结果接口契约。
