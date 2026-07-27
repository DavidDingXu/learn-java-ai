# Release Checklist

## Direct Verification

Run the Java 21 main reactor directly:

```bash
./mvnw verify
```

Framework labs use `./mvnw -f labs/pom.xml verify`. The Java 8 client uses its independent POM with JDK8 selected in the IDE or Maven runtime. These commands do not require project-specific environment variables.

`scripts/release-gate.sh` and `scripts/release-gate.ps1` are optional aggregators. They execute the root reactor, isolated framework labs, Java 8 client, frontend and Node contracts, column contracts when present, and a tracked/untracked secret scan. They are not required to start any Java service.

## External Evidence

For a release that claims a deployed environment is ready, the deployment pipeline must supply the target service URL and retain its external health result. This automation detail is not part of local demo setup. The generic health smoke proves only one application health endpoint. Model, retrieval, Agent and security reports remain separate commands because they require different identities and datasets.

## Operational Review

- Database and schema migration owner is identified.
- Feature flags default to the intended release state.
- Model, embedding and rerank names are explicit.
- Secrets come from the approved secret system.
- Agent write tools have persistent idempotency and unknown-result recovery.
- Dashboards and alerts cover latency, error, token, capacity and uncertain execution.
- Rollback distinguishes application rollback, prompt/model rollback and data migration rollback.
- Runbooks contain on-call ownership and links to the exact reports used for the decision.
