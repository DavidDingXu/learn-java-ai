# A2A 任务边界验证

Status: VERIFIED_CLIENT_INTEROPERABILITY_AND_LOCAL_TASK_CONTRACT

## 已验证

- 服务端根据租户和业务请求字段计算规范化、带长度前缀的 SHA-256 请求指纹，不接受调用方提供可信哈希。
- 聚焦测试明确校验相邻字段边界，不依赖分隔符拼接。
- 同一租户、幂等键和请求的重复提交复用一个任务；相同键对应不同请求时会被拒绝。
- 不同租户命名空间使用相同幂等键时，仍创建相互隔离的任务。
- 状态只能单向推进；终态不能回退，交付结果不确定时可先进入 UNKNOWN，再等待可信终态。
- 只有状态和回执完全一致时，重复终态回调才按幂等处理；冲突终态会作为协议冲突被拒绝。
- 未知任务 ID 会被拒绝，且不会创建本地状态。
- A2A Java SDK `1.2.0.Final` discovers a standard Agent Card, validates an allowlisted skill and sends a message over JSON-RPC.
- 官方客户端把响应映射为带 Artifact 的已完成 A2A Task。
- AgentScope `A2aTaskCoordinator` 继续承担业务状态边界，SDK 枚举不会作为领域契约持久化。

## 验证命令

```bash
./mvnw -f labs/pom.xml -pl protocol-interop-lab,agentscope-lab test
```

## 外部验证边界

协议测试使用官方 A2A 客户端调用本地标准 Agent Card 和 JSON-RPC 服务，覆盖发现、消息发送和完成任务映射，不包含远程状态查询。生产认证、流式交互、推送通知、长时间远程执行、取消、回调和 Inbox/Outbox 持久化，仍需要在目标 Agent 和网络环境验证。
