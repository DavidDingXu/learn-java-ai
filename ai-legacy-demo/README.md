# ai-legacy-demo

这个模块演示老 Java / JDK8 系统接入外部 Agent 服务的边界。

这个模块保持 JDK8 编译边界，不接 Spring Boot 3，也不提供前端页面。可视化工单 AI 助手请看 `project-helpdesk-agent` 或 `ai-agent-demo`；本模块重点用契约测试保护老系统和 Agent 服务之间的 API 隔离。

真实 AI 交互通过 `HttpLegacyAgentClient` 完成：老系统只发 `AgentTask API` HTTP 请求到外部 Agent 服务，例如 `project-helpdesk-agent` 的 `/api/helpdesk-agent/advice/live`。老系统本身不直接依赖 Spring AI、模型 SDK 或新服务内部类。

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
| `HttpLegacyAgentClient` | JDK8 兼容的 HTTP Agent Client，可调用真实外部 Agent 服务 |
| `ExternalAgentService` | 外部 Agent 服务 |
| `LegacyToolApiFacade` | 老系统暴露给 Agent 的受控 Tool API |
| `OperatorContext` | 身份、租户、部门、权限上下文 |

## 调用真实外部 Agent 服务

先启动 `project-helpdesk-agent` 并配置真实 `AI_API_KEY`：

```bash
mvn -pl project-helpdesk-agent spring-boot:run
```

老系统侧使用 `HttpLegacyAgentClient` 指向：

```text
http://localhost:8091/api/helpdesk-agent/advice/live
```

测试 `legacySystemCanCallRealExternalAgentServiceOverHttp` 用本地 HTTP Server 验证了请求体、响应解析和人工确认字段，不需要在 JDK8 模块里启动 Spring Boot。

## 当前测试覆盖

```text
legacySystemShouldKeepPermissionAndSubmitAgentTaskOnly
legacyToolApiShouldFilterUnauthorizedDepartment
externalAgentShouldUseLegacyToolApiInsteadOfLegacyInternalClasses
legacySystemCanCallRealExternalAgentServiceOverHttp
agentTaskApiAndToolApiShouldStayAsSeparateContracts
auditShouldKeepOperatorTenantDepartmentsAndPermissionsSnapshot
```

关键边界：

- `AgentTaskRequest#getContractName()` 标识老系统提交给 Agent 的任务合同。
- `TicketSnapshot#getContractName()` 标识 Agent 通过老系统 Tool API 读取到的业务快照合同。
- `LegacyAuditRecord` 会保存 operator 的租户、部门和权限快照，便于审计复盘。
