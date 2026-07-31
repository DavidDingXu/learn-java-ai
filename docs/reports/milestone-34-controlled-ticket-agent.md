# 里程碑 34：受控工单 Agent

状态：已完成本地受控 Agent 与真实模型规划验证，仍需共享环境联调（`VERIFIED_LOCAL_CONTROLLED_AGENT_LIVE_MODEL_SHARED_ENVIRONMENT_REQUIRED`）

## 已完成的能力

这一阶段已形成有步数上限的 Agent 任务状态机、Spring AI 2.0 结构化规划、由服务端持有的 Tool Catalog、带委托身份的知识查询工具、按风险分级的写工具、绑定任务版本的人工确认、远程结果分类、Java 8 任务客户端、可审计 OpenAPI 和独立 Agent 评测。

## 已验证的行为

- 真实模型规划检查返回了结构化的 `USE_TOOL / QUERY_KNOWLEDGE` 决策和模型用量元数据。
- Java 主工程、隔离框架 labs 和独立 Java 8 客户端都有可重复执行的验证入口。
- Agent 路径评测检查工具、参数、风险、角色和确认前禁止事件。
- 确定性业务测试覆盖未知工具、非法参数、拒绝、过期与重复确认，以及幂等冲突。
- 敏感信息扫描不允许 API Key 和服务令牌进入仓库。

## 适用范围

本地验证不能代表生产 IdP、Agent 任务/确认/审计的持久存储、已准备好的外部知识索引、Legacy Tool 的持久幂等、未知结果对账和端到端容量已经验收。开启写工具前，这些能力必须在公司共享环境重新验证。
