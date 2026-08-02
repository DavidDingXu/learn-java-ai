# 带依据的 RAG 回答验证

Status: VERIFIED_LOCAL_GROUNDED_ANSWER_COMPOSITION

- Retrieval context adapter: `RetrievalPolicyContextSource`
- Answer use case: `KnowledgeAnswerService`
- Prompt contract: `GroundedPrompt`
- Output guard: `KnowledgeAnswerValidator`
- Prompt policy: `prompts/knowledge-answer/v1/system.txt`
- Runtime wiring: `KnowledgeRetrievalConfiguration`

## 已验证

- Controller 构造的可信身份范围和服务端当前时间会随问题进入检索，不由模型或用户输入生成。
- 检索结果保留文档 ID、版本、chunk ID、标题路径、条款和正文，并映射为可引用的 `PolicyContext`。
- 模型收到的是分离的系统规则、Prompt 版本、用户问题和上下文集合；系统规则明确把用户输入与政策正文中的命令式文字都视为数据。
- 非拒答结果至少需要一个引用，引用 ID 必须存在于本次检索上下文；拒答结果必须给出拒答原因。
- 对外 `Citation` 由服务端从已检索上下文重新构造，模型不能自行补造文档标题、版本和引用元数据。
- 输出校验同时拒绝虚构“已退款”“已创建工单”等未发生业务动作。
- Knowledge Service 默认使用 classpath 上下文；只有 `java-ai.knowledge.mode=postgres-rag` 才创建数据库检索适配器，不会把静态上下文误报成向量 RAG。

## 本地验证

```bash
./mvnw -pl services/knowledge-service \
  -Dtest=RetrievalPolicyContextSourceTest,KnowledgeAnswerServiceTest \
  test
```

这些测试覆盖可信检索参数传递、chunk 到引用上下文的映射、引用回填和未知引用拒绝。

## 证据边界

这些测试使用内存 Retriever 和模型替身，没有把 Provider Embedding、pgvector 和 Chat API 串成一次端到端调用。模型接口 Smoke 与 pgvector 外部测试各自检查一段链路，不能据此推断整条 RAG 链路满足目标环境要求。跨文档冲突裁决、引用原文片段校验、恶意知识内容扫描和检索型 Prompt Injection 防护也不在当前范围内。
