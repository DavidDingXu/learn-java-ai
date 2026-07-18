import { createHmac } from "node:crypto";
import process from "node:process";

try {
  process.loadEnvFile(".env");
} catch (error) {
  if (error?.code !== "ENOENT") throw error;
}

function argumentValue(name, fallback) {
  const index = process.argv.indexOf(`--${name}`);
  if (index === -1) return fallback;
  const value = process.argv[index + 1];
  if (!value || value.startsWith("--")) {
    throw new Error(`--${name} requires a value`);
  }
  return value;
}

function required(value, name) {
  if (!value?.trim()) throw new Error(`${name} is required`);
  return value.trim();
}

function list(value) {
  return value.split(",").map((item) => item.trim()).filter(Boolean);
}

function base64Url(value) {
  return Buffer.from(value).toString("base64url");
}

function createToken() {
  const profile = argumentValue("profile", "service");
  if (profile !== "service" && profile !== "customer") {
    throw new Error("--profile must be service or customer");
  }
  const customerProfile = profile === "customer";
  const secretVariable = customerProfile
    ? "JAVA_AI_CUSTOMER_JWT_HMAC_SECRET"
    : "JAVA_AI_DEV_JWT_HMAC_SECRET";
  const secret = required(process.env[secretVariable], secretVariable);
  if (Buffer.byteLength(secret, "utf8") < 32) {
    throw new Error(`${secretVariable} must contain at least 32 bytes`);
  }

  const issuerVariable = customerProfile ? "JAVA_AI_CUSTOMER_JWT_ISSUER" : "JAVA_AI_JWT_ISSUER";
  const issuer = required(process.env[issuerVariable], issuerVariable);
  const audience = argumentValue(
    "audience",
    customerProfile ? "customer-bff" : process.env.JAVA_AI_JWT_AUDIENCE ?? "knowledge-service",
  );
  const scope = required(argumentValue("scope", customerProfile ? "consultation:use" : undefined), "--scope");
  const subject = required(argumentValue("subject", customerProfile ? "customer-42" : "editor-42"), "--subject");
  const tenant = required(argumentValue("tenant", "tenant-a"), "--tenant");
  const actor = customerProfile ? null : required(argumentValue("actor", "customer-bff"), "--actor");
  const departments = list(argumentValue("departments", "support"));
  const roles = list(argumentValue("roles", customerProfile ? "customer" : "EDITOR"));
  const lifetimeSeconds = Number.parseInt(argumentValue("lifetime-seconds", "900"), 10);
  if (!Number.isInteger(lifetimeSeconds) || lifetimeSeconds < 60 || lifetimeSeconds > 3600) {
    throw new Error("--lifetime-seconds must be an integer between 60 and 3600");
  }

  const issuedAt = Math.floor(Date.now() / 1000);
  const header = base64Url(JSON.stringify({ alg: "HS256", typ: "JWT" }));
  const claims = {
    iss: issuer,
    sub: subject,
    aud: [audience],
    iat: issuedAt,
    exp: issuedAt + lifetimeSeconds,
    scope,
    tenantId: tenant,
    roles,
    departmentIds: departments,
    ...(actor ? { act: { sub: actor } } : {}),
  };
  const payload = base64Url(JSON.stringify(claims));
  const signingInput = `${header}.${payload}`;
  const signature = createHmac("sha256", secret).update(signingInput).digest("base64url");
  return `${signingInput}.${signature}`;
}

try {
  process.stdout.write(`${createToken()}\n`);
} catch (error) {
  process.stderr.write(`ERROR: ${error.message}\n`);
  process.exitCode = 2;
}
