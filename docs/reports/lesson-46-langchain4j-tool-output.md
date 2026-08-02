# LangChain4j Tool 与结构化输出验证

Status: VERIFIED_READ_ONLY_TOOL_LOOP


## 已验证

- 真实 LangChain4j `@Tool` 在两轮模型调用中执行一次只读工单查询。
- 最终 JSON 映射为 `TicketDecision`，并与当前工单 ID 交叉校验。
- 测试确认只执行一次 Tool、调用两次模型，并拒绝指向其他工单的结构化结果。

## 验证命令

```bash
./mvnw -f labs/pom.xml -pl langchain4j-lab test
```

## 外部验证边界

No write tool is executed. Production writes must reuse confirmation, action id, idempotency, audit and UNKNOWN reconciliation.
