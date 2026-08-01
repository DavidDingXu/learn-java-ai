# 国内检索组件替换验证

Status: VERIFIED_DETERMINISTIC_METRICS


## 已验证

- `RetrievalReplacementExperiment` maps DashScope embedding and rerank options from one retrieval profile.
- The experiment calculates Recall, MRR and p95 latency from supplied ranking cases, then checks explicit thresholds.
- `DomesticRetrievalProfile` requires a full reindex when the embedding model or vector dimension changes.
- Tests cover option mapping, metric calculation, threshold acceptance and the reindex predicate.

## 验证命令

```bash
./mvnw -f labs/pom.xml -pl spring-ai-alibaba-lab test
```

## 外部验证边界

Fixed ranking cases verify option mapping and metric calculations. They do not compare real providers or prove retrieval quality. A replacement decision still requires the same company corpus, prepared indexes, provider credentials, latency measurements and cost data.
