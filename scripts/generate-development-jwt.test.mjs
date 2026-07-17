import assert from "node:assert/strict";
import { createHmac } from "node:crypto";
import { spawnSync } from "node:child_process";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const script = path.join(projectRoot, "scripts/generate-development-jwt.mjs");
const secret = "local-development-secret-32-bytes-minimum";

function run(args, overrides = {}) {
  return spawnSync(process.execPath, [script, ...args], {
    cwd: projectRoot,
    encoding: "utf8",
    env: {
      ...process.env,
      JAVA_AI_DEV_JWT_HMAC_SECRET: secret,
      JAVA_AI_JWT_ISSUER: "https://identity.test",
      JAVA_AI_JWT_AUDIENCE: "knowledge-service",
      ...overrides,
    },
  });
}

test("generates a short-lived delegated JWT with the requested access scope", () => {
  const result = run([
    "--scope", "knowledge:write",
    "--subject", "editor-42",
    "--tenant", "tenant-a",
    "--actor", "customer-bff",
    "--departments", "support,retail",
  ]);

  assert.equal(result.status, 0, result.stderr);
  const [header, payload, signature] = result.stdout.trim().split(".");
  assert.equal(createHmac("sha256", secret).update(`${header}.${payload}`).digest("base64url"), signature);

  const claims = JSON.parse(Buffer.from(payload, "base64url").toString("utf8"));
  assert.equal(claims.iss, "https://identity.test");
  assert.deepEqual(claims.aud, ["knowledge-service"]);
  assert.equal(claims.scope, "knowledge:write");
  assert.equal(claims.tenantId, "tenant-a");
  assert.deepEqual(claims.departmentIds, ["support", "retail"]);
  assert.deepEqual(claims.act, { sub: "customer-bff" });
  assert.ok(claims.exp - claims.iat <= 900);
});

test("rejects a missing scope and a short signing secret", () => {
  const missingScope = run([]);
  assert.equal(missingScope.status, 2);
  assert.match(missingScope.stderr, /--scope is required/);

  const shortSecret = run(["--scope", "knowledge:index"], {
    JAVA_AI_DEV_JWT_HMAC_SECRET: "too-short",
  });
  assert.equal(shortSecret.status, 2);
  assert.match(shortSecret.stderr, /at least 32 bytes/);
});
