# 阶段 03：企业 RAG

本阶段在模型调用链上加入文档生命周期、切分、PostgreSQL/pgvector、ACL、混合检索、增量索引和检索评测，代码仍然来自当前主线实现。

1. 准备一个空的专用 PostgreSQL 数据库，填写 `config/application.yml` 中的 Chat、Embedding API 和数据库配置。
2. 在 IDEA 中运行 `KnowledgeServiceApplication`。
3. 打开 `rag-learning-journey.http`，从上到下执行请求，依次观察上传、发布、索引、检索、ACL 负例、混合检索和带引用回答。

Embedding 只依赖 Spring AI 的 `EmbeddingModel` 接口。默认可使用 OpenAI 兼容 API；若选择 Ollama，把 `spring.ai.model.embedding` 改为 `ollama` 并填写同一 YAML 中的 Ollama 配置，业务代码不需要改。

HTTP 文件使用稳定文档 ID，便于第 21 篇直接回放 Golden Set。完整执行一次后若要从头重跑，请换一个新的空数据库；接口会拒绝旧 revision，不会覆盖已有文档或自动清库。
