# 第 12 篇：模型交互评测与观测验证记录

Status: VERIFIED_LIVE_MODEL

- Implementation commit: `d9fd1db812f132e25fe7bd82c02bddeba7fd1107`
- Dataset: `golden-set-v2`
- Contract report: `lesson-12-contract-eval.md`
- Live report: `lesson-12-live-model-eval.md`

## 已验证的行为

- Eval Runner 独立读取 JSONL，通过公开 HTTP 接口评测，不依赖 Knowledge Service 内部类。
- 接口契约检查 5/5 通过，并明确标记 `CONTRACT_FIXTURE`，不会与真实模型结果混淆。
- 真实 Provider API 评测 5/5 通过，覆盖正常回答、证据不足拒答、Prompt Injection、敏感凭证和未授权写操作。
- HTTP 评测结果包含有效 Trace ID。报告记录模型、Prompt 版本、低敏环境标识、Commit、整轮总 Token，以及每条样例的延迟和稳定失败原因。未预期的底层异常不会原样进入报告。
- 只有代码、数据集和 Prompt 版本一致的结果才具有可比性。任一基线不同，报告只能说明各自运行时的结果，不能用于判断模型或 Prompt 是否回归。
- Observation 只使用固定操作标签 `context_load` 和 `model_call`，不把问题、Prompt、用户或租户写进指标标签。

## 适用范围

5 条样例只能检查链路和评测方法，不能代表生产准确率。公司落地时应从线上脱敏坏案例扩充 Golden Set，按业务风险分层，并把模型、Prompt、检索和权限变更纳入同一回归。
