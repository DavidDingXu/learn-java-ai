# Customer BFF

客户咨询渠道的后端边界。该模块建立客户身份，生成面向下游的短时委托令牌，并组合 Knowledge Service 与 Ticket Agent Service 的 HTTP/SSE 协议。它不调用模型 SDK，也不复制知识、检索或工单领域规则。

## 主要能力

- 完整回答和具有命名事件的 SSE 回答；
- 按回答 attempt 记录反馈和重试；
- 将未解决问题作为不可变快照幂等升级为工单；
- 稳定映射下游超时、失败和 SSE 取消。

公开契约见 [`contracts/openapi/customer-bff-v1.yaml`](../../contracts/openapi/customer-bff-v1.yaml)。

## 直接启动

先运行 `KnowledgeServiceApplication` 和 `TicketAgentServiceApplication`，再用 IDE 运行 `CustomerBffApplication`。启动后访问 `http://localhost:8080/actuator/health`。

默认使用固定客户身份和本地委托令牌，并通过 HTTP 调用两个下游服务，不需要先部署身份平台。前端运行方式见 [`apps/customer-web/README.md`](../customer-web/README.md)。

接入公司身份平台时，将 `java-ai.security.mode` 改为 `jwt`，并将 `java-ai.identity.delegation-mode` 改为 `oauth2`。两个下游 HTTP 客户端默认已经启用。完整清单见[运行配置](../../docs/runbooks/runtime-configuration.md)。

多实例部署前，需要将进程内会话与限流替换为共享实现，并验证 TTL、原子版本更新、故障恢复和网关超时。
