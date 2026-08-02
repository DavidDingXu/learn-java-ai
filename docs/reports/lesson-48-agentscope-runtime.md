# AgentScope Runtime 适配验证

Status: VERIFIED_PERMISSION_MAPPING


## 已验证

- `AgentScopeTicketRuntime` uses real `RuntimeContext`, `Toolkit` and `PermissionEngine` types.
- 查询、更新和客户数据导出分别映射为 ALLOW、ASK 和 DENY。
- 未知 Tool 会在权限计算前被拒绝。
- 可信执行身份继续使用业务对象保存；权限决定同时保留 Tool、规则来源和可读原因。

## 验证命令

```bash
./mvnw -f labs/pom.xml -pl agentscope-lab test
```

## 外部验证边界

该实验只映射运行时决策。生产环境的确认持久化、业务授权和审计存储仍由应用服务负责。
