# Milestone 21 Enterprise RAG

Status: VERIFIED_LOCAL_CONTRACTS_EXTERNAL_ENVIRONMENT_REQUIRED

## Implementation

Commit: `0f5d9649e5e39ef2b54907dc394ffc871abffed0`

The milestone contains document lifecycle and upload boundaries, deterministic chunking, pgvector retrieval, ACL and effective-time filtering before TopK, embedding-model isolation, PostgreSQL trigram hybrid retrieval, RRF, controlled reranking, grounded answer context, recoverable index tasks and an authenticated retrieval evaluation runner.

## Verification

- Knowledge Service local contract suite: 70 tests passed after excluding tests that require loopback sockets, live model credentials or an external PostgreSQL environment.
- Customer BFF delegated-token contract: 1 test passed in the same reactor run.
- Eval Runner local suite: 14 tests passed.
- Project contract suite: 43 tests passed; 1 loopback-only test was skipped by the restricted sandbox.
- OpenAPI and JSON Schema validation: 3 OpenAPI files, 2 schemas, 2 positive fixtures and 2 negative fixtures passed.
- Article content, AI-flavor, code-link and visual-asset checks passed for the 21 available articles.

## External Boundary

The local result does not claim real PostgreSQL/pgvector capacity, real retrieval quality, production object storage or multi-process worker recovery. `PgVectorExternalIT` and `scripts/run-retrieval-eval.sh` must run against a dedicated prepared environment before publishing environment-specific Recall, latency or capacity conclusions.

## Tag Rule

`milestone-21-enterprise-rag` must point to implementation commit `0f5d9649e5e39ef2b54907dc394ffc871abffed0`. Documentation-only commits do not replace this implementation evidence point.
