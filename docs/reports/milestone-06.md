# Milestone 06 Identity and Contract Boundaries

## Status

`VERIFIED_LOCAL_BOUNDARIES`

Starting from commit `f1a0989`, the repository now has executable JWT delegation rules, HTTP/OpenAPI contracts, JSON Schema fixtures and a separately compiled JDK8 DTO boundary.

The milestone is satisfied when all of the following pass on one commit:

```bash
node --test scripts/lesson-05-06-contract.test.mjs
./mvnw -pl services/knowledge-service,apps/customer-bff,quality/eval-runner verify
./mvnw -f integrations/jdk8-client/pom.xml verify
```

## Accepted claims

- A customer token for `customer-bff` cannot be used directly against Knowledge Service.
- A delegated JWT must target `knowledge-service`, preserve subject and tenant, identify `customer-bff` as actor, and carry `knowledge:answer` scope.
- Public request contracts do not accept identity or authorization facts in JSON.
- The legacy contract DTO compiles and runs on JDK8.

## Release boundary

The `milestone-06-boundaries` tag binds this evidence after the checks pass. Production identity-provider integration, real key rotation and external legacy deployment remain environment-specific acceptance work.
