# ai-legacy-demo

这个模块演示老 Java / JDK8 系统接入外部 Agent 服务的边界。

这个模块保持 JDK8 编译边界，不接 Spring Boot 3，也不提供前端页面。可视化工单 AI 助手请看 `project-helpdesk-agent` 或 `ai-agent-demo`；本模块重点用契约测试保护老系统和 Agent 服务之间的 API 隔离。

核心原则：

- 老系统保留身份、权限、状态流转和审计。
- 老系统通过 AgentTask API 请求 AI 建议。
- Agent 服务只能通过老系统暴露的 Tool API 查询业务数据。
- Agent 服务不依赖老系统内部类，也不直接连老系统数据库。

## 运行

```bash
mvn -pl ai-legacy-demo test
```

## 核心类

| 类 | 说明 |
|---|---|
| `LegacyTicketSystem` | 模拟老工单系统入口 |
| `LegacyAgentClient` | 老系统调用外部 Agent 的客户端边界 |
| `ExternalAgentService` | 外部 Agent 服务 |
| `LegacyToolApiFacade` | 老系统暴露给 Agent 的受控 Tool API |
| `OperatorContext` | 身份、租户、部门、权限上下文 |

## 当前测试覆盖

```text
legacySystemShouldKeepPermissionAndSubmitAgentTaskOnly
legacyToolApiShouldFilterUnauthorizedDepartment
externalAgentShouldUseLegacyToolApiInsteadOfLegacyInternalClasses
agentTaskApiAndToolApiShouldStayAsSeparateContracts
auditShouldKeepOperatorTenantDepartmentsAndPermissionsSnapshot
```

关键边界：

- `AgentTaskRequest#getContractName()` 标识老系统提交给 Agent 的任务合同。
- `TicketSnapshot#getContractName()` 标识 Agent 通过老系统 Tool API 读取到的业务快照合同。
- `LegacyAuditRecord` 会保存 operator 的租户、部门和权限快照，便于审计复盘。
