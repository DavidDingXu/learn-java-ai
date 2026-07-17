# Lesson 08 Prompt Boundary Evidence

Status: VERIFIED

- Implementation commit: `f5532160ae5018da237567e5855bd20bb7ce2123`
- Prompt version: `knowledge-answer-v1`
- Prompt resource: `prompts/knowledge-answer/v1/system.txt`
- Live case: `prompt-injection` in `golden-set-v2.jsonl`

## Verified

- System rule、可信政策上下文和不可信用户输入使用独立分区。
- 请求显式携带 Prompt 版本，但日志和指标不记录问题正文或完整 Prompt。
- 政策正文和用户问题中的命令式文字都不能覆盖系统规则。
- 模型 Golden Set 中的 Prompt Injection 案例通过，回答没有泄露系统提示词。

## Replacement Boundary

公司项目应把 Prompt 当作带版本的发布物，变更需要 Golden Set 回归。模板平台、配置中心或数据库只是存储方式，不能替代信任分区、评测和回滚规则。
