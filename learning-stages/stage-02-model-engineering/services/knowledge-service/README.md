# Knowledge Service：模型工程阶段

这个切片只包含第 04-12 篇使用的回答链路：真实 Chat Provider、委托身份、Prompt、结构化输出、SSE、超时重试和模型元数据。`document`、`indexing`、`retrieval` 与 PostgreSQL 尚未进入本阶段。

## 直接启动

项目根目录唯一的 `config/application-default.yml` 保存 API Key、Base URL 和模型名。在 IDEA 中运行 `KnowledgeServiceApplication`，再打开阶段根目录的 `model-engineering-learning-journey.http`：

- `02-first-model-answer` 观察回答、模型名和 Trace；
- `03-structured-refusal` 观察稳定的结构化响应；
- `04-streaming-answer` 观察 SSE 响应。

阅读代码时从 `KnowledgeAnswerController` 进入，再沿 `KnowledgeAnswerService`、`KnowledgeAnswerModel` 到 `SpringAiKnowledgeAnswerModel`。这一阶段可以忽略其他阶段目录和完整项目中的 RAG 包。
