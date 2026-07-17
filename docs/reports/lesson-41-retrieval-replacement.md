# Lesson 41 Domestic Retrieval Replacement Evidence

Status: VERIFIED_DETERMINISTIC_METRICS

Implementation commit: `5ee567645050a76bf54719a460b5c7069678572d`

## Verified

- `RetrievalReplacementExperiment` maps DashScope embedding and rerank options from one retrieval profile.
- The experiment calculates Recall, MRR and p95 latency from supplied ranking cases, then checks explicit thresholds.
- `DomesticRetrievalProfile` requires a full reindex when the embedding model or vector dimension changes.
- Tests cover option mapping, metric calculation, threshold acceptance and the reindex predicate.

## Verification

```bash
./mvnw -f labs/pom.xml -pl spring-ai-alibaba-lab test
```

## External Boundary

Fixed ranking cases verify option mapping and metric calculations. They do not compare real providers or prove retrieval quality. A replacement decision still requires the same company corpus, prepared indexes, provider credentials, latency measurements and cost data.
