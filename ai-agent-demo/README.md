# ai-agent-demo

受控工作流 Agent demo。

本模块演示企业里第一版 Agent 更适合做成受控工作流：先查工单，再查订单，再检索制度，然后评估风险，最后生成建议。高风险动作只给建议，不直接执行。

模块里也包含一个受限 ReAct 示例：用脚本 planner 模拟模型选择工具，但通过 `ReActPolicy` 限制最大步骤和工具白名单。它不接真实 LLM，重点是演示 Java 工程里 ReAct 循环外面的护栏。

受限 ReAct 示例还补了 `AgentHookChain`。Hook 会在动作执行前处理 PII 脱敏、工具白名单等治理逻辑，并把 `hookEvents` 写入 `ReActStep`。它和 `ReActPolicy` 的分工不同：Hook 更适合做动作级拦截、输入改写和风险标记，Policy 负责默认执行边界。

此外，本模块提供了一个内存版 `ConversationMemoryStore`，用来演示 Agent 记忆的工程边界：会话消息只保留最近窗口，业务状态单独保存，长期用户偏好不能和工单状态混在一起。当前实现不接 Redis 或数据库，只覆盖接口边界和测试合同。

`AgentContextAssembler` 演示多轮工单处理里的上下文组装：按租户、会话、业务对象和用户读取必要上下文，并保留 conversation、businessSnapshot、preferences 三类来源。它不负责调模型，也不负责 Prompt 渲染。

`ContextEngineeringService` 在组装结果后再做一层上下文工程处理：按 token 预算保留业务快照、最近对话和用户偏好；超过窗口的历史对话会被压缩成摘要切片，并保留来源标记。当前实现是规则版，不接真实 tokenizer，也不写入 Redis 或数据库，重点是表达上下文预算和来源追踪的工程合同。

## 启动

```bash
mvn -pl ai-agent-demo spring-boot:run
```

打开前端页面：

```text
http://localhost:8089/
```

页面会提交工单上下文，并把 Agent 的执行步骤、工具输出和最终建议分开展示。页面下方还有 Agent 链路实验，可以观察上下文预算如何裁剪记忆、ReAct 循环如何记录 Hook 事件、PII 如何脱敏、未授权工具如何被拒绝。

## 验证

```bash
curl -X POST http://localhost:8089/api/agent/tickets/advice \
  -H 'Content-Type: application/json' \
  -d '{
    "ticketId": "T-9001",
    "userQuestion": "客户要求立刻退款 5000 元，并关闭投诉工单。",
    "userId": "u9001",
    "tenantId": "tenant-a",
    "department": "support"
  }'
```

重点看返回里的：

```text
requiresHumanApproval=true
steps 包含 LOOKUP_ORDER 和 ASSESS_RISK
ASSESS_RISK.autoExecutable=false
```

上下文和 Hook 也可以直接用接口观察：

```bash
curl -X POST http://localhost:8089/api/agent/lab/react \
  -H 'Content-Type: application/json' \
  -d '{
    "scenario": "pii",
    "userInput": "请处理退款，手机号 13812345678",
    "maxSteps": 5
  }'
```

返回结果里应该能看到 `hookEvents` 包含 `pii-masking:MASK_PHONE`，并且工具输入中的手机号已被脱敏。

## 测试

```bash
mvn -pl ai-agent-demo test
```

当前测试覆盖：

```text
TicketAgentTest：受控工作流 Agent 的步骤顺序和高风险人工确认。
GuardedReActAgentTest：只读工具白名单、未授权工具拒绝、最大步骤停止。
AgentHookChainTest：动作执行前的手机号脱敏、Hook 工具白名单拒绝和 hookEvents 记录。
ConversationMemoryStoreTest：会话窗口、租户隔离、业务快照和长期偏好分离。
AgentContextAssemblerTest：按 session scope 组装多轮上下文，防止跨会话复用业务快照。
ContextEngineeringServiceTest：按预算裁剪上下文，保留业务快照、最近对话、用户偏好和历史摘要。
TicketAgentControllerTest：覆盖工单建议接口、上下文实验接口和 ReAct Hook 实验接口。
AiAgentApplicationContextTest：覆盖前端页面必须暴露工单建议、上下文预算和 ReAct 实验入口。
```
