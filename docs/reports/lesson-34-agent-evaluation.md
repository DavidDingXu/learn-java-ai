# Lesson 34 Agent Evaluation Evidence

Status: VERIFIED_AGENT_EVALUATION_PIPELINE

Implementation commit: `44713c1a26c1e9a4d47354032db8c3e32d5e0b49`

## Verified

- The versioned JSONL dataset records objective, business context, expected state, tool, risk, role and forbidden audit events.
- Eval Runner creates and runs tasks through public HTTP, then reads audit through a third endpoint.
- Create, run and read use separate bearer tokens; Eval Runner has no confirmation capability.
- The evaluator reports exact mismatches and client errors per case instead of hiding them in an average score.
- JSON and Markdown reports bind dataset version, code version, case results and latency.
- Forbidden execution events prevent a write-side-effect regression from passing evaluation.

## Verification

```bash
./mvnw -pl quality/eval-runner \
  -Dtest=AgentEvalDatasetLoaderTest,AgentEvaluatorTest,AgentTaskHttpEvaluationClientTest,AgentEvaluationReportWriterTest test
```

## External Boundary

The three-case dataset proves the evaluation path, not production accuracy. Company gates must add de-identified high-risk, refusal, injection, authorization and parameter cases, and run against a dedicated tenant with no production write credentials.
