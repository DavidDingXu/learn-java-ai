# 国内检索组件替换验证

Status: VERIFIED_DETERMINISTIC_METRICS


## 已验证

- `RetrievalReplacementExperiment` maps DashScope embedding and rerank options from one retrieval profile.
- 实验根据给定排序样例计算 Recall、MRR 和 p95 延迟，再检查明确的准入阈值。
- `DomesticRetrievalProfile` requires a full reindex when the embedding model or vector dimension changes.
- 测试覆盖选项映射、指标计算、阈值判定和是否需要重建索引的条件。

## 验证命令

```bash
./mvnw -f labs/pom.xml -pl spring-ai-alibaba-lab test
```

## 外部验证边界

Fixed ranking cases verify option mapping and metric calculations. They do not compare real providers or prove retrieval quality. A replacement decision still requires the same company corpus, prepared indexes, provider credentials, latency measurements and cost data.
