# Lesson 24 Trust, Feedback And Retry Evidence

Status: VERIFIED_LOCAL_ATTEMPT_SCOPED_INTERACTION

Implementation commit: `2cbe5398e6cfec9090bed091947f6b0d261077ee`

## Verified

- C 端回答 DTO 保留引用、拒答原因、conversation、attempt 和 trace，不暴露 Provider 模型对象或 Token 元数据。
- 引用继续由 Knowledge Service 根据本次授权上下文校验，BFF 只转换为渠道 DTO。
- 流式 `completed` 显式携带拒答字段，BFF 保存拒答状态后再向 Customer Web 完成当前 attempt。
- 反馈必须绑定已完成的回答尝试；`NOT_HELPFUL` 必须携带稳定原因码，评论有长度上限。
- `NOT_HELPFUL` 的条件要求同时进入 Bean Validation 和 OpenAPI 3.1，接口文档不再允许运行时必然失败的空原因请求。
- 同一尝试的反馈使用 PUT 语义，可按业务规则更新，不创建无归属的匿名评价。
- 重试创建新 attempt，并通过 `retryOfAttemptId` 指回原回答；旧回答、旧引用和旧反馈保持不变。
- 会话归属校验在反馈、重试和工单升级前重复执行，不能只在首次提问时检查。
- 下游失败、限流、权限错误和状态冲突映射为不同 HTTP 语义，不统一包装成通用 500。

## Local Verification

```bash
./mvnw -pl apps/customer-bff \
  -Dtest=ConsultationSessionTest,CustomerConsultationServiceTest,CustomerConsultationControllerTest,AnswerFeedbackRequestTest \
  test
```

Customer Web 的反馈、重试和移动端展示验证记录在 `docs/reports/lesson-22-customer-web.md`。

## Production Boundary

当前反馈覆盖领域规则与 HTTP 接口，尚未接入分析仓库、人工标注队列或在线质量看板。公司项目需要按隐私和保留策略决定是否保存问题、回答和评论原文，并将反馈原因码纳入 Golden Set 与坏案例流程。
