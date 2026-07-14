# Spring AI Alibaba Lab

This lab keeps the business inputs and evaluation rules stable while experimenting with Spring AI Alibaba 1.1.2.3.

- `DashScopeProviderAdapter` maps system/user messages and Provider options without exposing credentials.
- `RetrievalReplacementExperiment` evaluates Recall@K, MRR and p95 on one Golden Set and detects embedding reindex requirements.
- `ConfirmationGraph` uses the real Graph runtime for low-risk direct execution and high-risk approval routing.
- `FrameworkCompatibilityDecision` prevents a Boot 3.5 / Spring AI 1.1 stack from being mixed into the Boot 4 / Spring AI 2 mainline by accident.

```bash
../../mvnw -f ../pom.xml -pl spring-ai-alibaba-lab test
```
