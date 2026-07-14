# Lesson 23 Conversation State Evidence

Status: VERIFIED_LOCAL_CONVERSATION_CONTRACT

Implementation commit: `2cbe5398e6cfec9090bed091947f6b0d261077ee`

## Verified

- `ConsultationSession` 同时绑定 conversation、tenant、customer 和版本号，其他客户不能读取或续写该会话。
- 每次回答创建独立 `AnswerAttempt`，状态为 `PENDING`、`COMPLETED` 或 `FAILED`；重试不能覆盖原尝试。
- 会话包含 TTL，读取、续写、反馈和升级都会检查过期状态。
- `ConversationWindowPolicy` 同时约束消息数量、估算 Token 和摘要长度，而不是只按轮数截断。
- 被裁剪历史只摘要“用户问了什么、是否已回答”，不会把旧助手答案写成业务事实。
- Knowledge Service 将摘要、历史消息和当前问题都放在不可信分区，检索仍只使用当前问题，可信政策只能来自授权检索结果。
- 测试配置使用带版本检查的内存存储端口，冲突不会静默覆盖。

## Local Verification

```bash
./mvnw -pl apps/customer-bff,services/knowledge-service \
  -Dtest=ConsultationSessionTest,ConversationWindowPolicyTest,CustomerConsultationServiceTest,KnowledgeAnswerControllerTest,SpringAiKnowledgePromptTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

## Production Boundary

内存存储不能用于多实例生产。公司项目应使用支持 TTL、原子创建和乐观版本控制的 Redis 或数据库适配器，并为热 Key、过期清理、容量上限和数据合规设置独立策略。会话摘要是交互上下文，不是订单、工单或政策事实来源。
