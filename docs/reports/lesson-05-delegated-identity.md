# Delegated Identity Verification

## Status

`LOCAL_SECURITY_CONTRACT_VERIFIED`

## Evidence boundary

- Knowledge Service validates JWT signature, issuer, audience, expiry, tenant and delegated actor before the request reaches business code.
- `knowledge:answer` is an authorization rule: a trusted token without that scope receives HTTP 403.
- Customer BFF exposes a `DelegatedTokenClient` port. The local HMAC signer is for development and automated tests only; the HTTP adapter uses RFC 8693 Token Exchange fields.
- Caller supplied identity headers are not authorization facts. The request body contains only the question.

## Executable evidence

```bash
./mvnw -pl services/knowledge-service,apps/customer-bff test
```

Covered cases are recorded in `datasets/security/jwt-boundary-cases-v1.jsonl` and executed by the service tests.

## Not claimed

This report does not prove connectivity, key rotation, client authentication or policy configuration for a company's identity platform. Those checks belong to the deployment environment.
