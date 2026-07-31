# Milestone 12 Model Engineering

Status: VERIFIED_LIVE_MODEL

## 已完成的能力

The milestone contains versioned trust-partitioned prompts, structured output and business validation, SSE contracts, use-case resilience, low-cardinality observations and an independent HTTP Eval Runner.

## 已验证的行为

- Java 21 主工程、框架 labs 和独立 JDK8 构建均有可重复执行的验证入口。
- 接口契约模式与真实模型模式分别运行，不会把固定响应结果当成真实模型证据。
- 固定评测样例覆盖正常回答、证据不足拒答、Prompt Injection、敏感凭证和未授权写操作。
- 模型报告记录模型、Token、延迟和 Trace；默认测试不要求真实密钥。

## 适用范围

小样本用于验证链路和评测方法，不代表生产准确率。模型、Prompt、数据或环境变化后，需要在相同条件下重新运行评测。
