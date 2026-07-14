# Lesson 41 Domestic Retrieval Replacement Evidence

Status: VERIFIED_DETERMINISTIC_COMPARISON

Implementation commit: `5ee567645050a76bf54719a460b5c7069678572d`

## Verified

- The replacement evaluator compares baseline and candidate results with Recall, MRR and p95 latency.
- A reindex decision requires compatible dimensions and measured quality evidence.
- Tests cover promotion, rejection and dual-index migration decisions.

## Verification

```bash
./mvnw -f labs/pom.xml -pl spring-ai-alibaba-lab test
```

## External Boundary

Deterministic samples prove the decision contract. Real domestic embedding and rerank quality requires the company corpus, prepared indexes, provider credentials and cost data.
