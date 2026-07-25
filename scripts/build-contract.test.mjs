import assert from "node:assert/strict";
import { existsSync, readFileSync, readdirSync, statSync } from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");

function read(relativePath) {
  const absolutePath = path.join(projectRoot, relativePath);
  assert.equal(existsSync(absolutePath), true, `${relativePath} must exist`);
  return readFileSync(absolutePath, "utf8");
}

test("root build locks dependency and plugin versions", () => {
  const pom = read("pom.xml");

  assert.match(pom, /<resilience4j\.version>2\.4\.0<\/resilience4j\.version>/);
  assert.match(pom, /<swagger-parser\.version>2\.1\.45<\/swagger-parser\.version>/);
  assert.match(pom, /<json-schema-validator\.version>3\.0\.6<\/json-schema-validator\.version>/);
  assert.match(pom, /<maven-shade\.version>3\.6\.2<\/maven-shade\.version>/);
  assert.match(pom, /<mockwebserver\.version>4\.12\.0<\/mockwebserver\.version>/);
  assert.match(pom, /<bannedDependencies>/);
  assert.match(pom, /<exclude>com\.alibaba\.cloud\.ai:\*<\/exclude>/);
  assert.match(pom, /<exclude>dev\.langchain4j:\*<\/exclude>/);
  assert.match(pom, /<exclude>io\.agentscope:\*<\/exclude>/);
});

test("knowledge service owns WebFlux, Spring AI and model resilience", () => {
  const pom = read("services/knowledge-service/pom.xml");

  for (const artifact of [
    "spring-boot-starter-webflux",
    "spring-boot-starter-validation",
    "spring-boot-starter-security",
    "spring-security-oauth2-resource-server",
    "spring-security-oauth2-jose",
    "spring-ai-starter-model-openai",
    "spring-boot-micrometer-tracing-opentelemetry",
    "micrometer-tracing-bridge-otel",
    "micrometer-registry-prometheus",
    "resilience4j-spring-boot4",
    "resilience4j-reactor",
    "caffeine",
    "spring-security-test",
    "mockwebserver",
    "reactor-test",
  ]) {
    assert.match(pom, new RegExp(`<artifactId>${artifact}<\\/artifactId>`));
  }

  assert.doesNotMatch(pom, /<artifactId>spring-boot-starter-web<\/artifactId>/);
});

test("customer BFF owns WebFlux identity infrastructure but no Spring AI", () => {
  const pom = read("apps/customer-bff/pom.xml");

  for (const artifact of [
    "spring-boot-starter-webflux",
    "spring-boot-starter-webclient",
    "spring-boot-starter-validation",
    "spring-boot-starter-security",
    "spring-security-oauth2-resource-server",
    "spring-security-oauth2-jose",
    "spring-security-oauth2-client",
    "spring-security-test",
    "mockwebserver",
  ]) {
    assert.match(pom, new RegExp(`<artifactId>${artifact}<\\/artifactId>`));
  }

  assert.doesNotMatch(pom, /org\.springframework\.ai|<artifactId>spring-ai-/);
});

test("eval runner owns contract tooling and remains independent of services", () => {
  const pom = read("quality/eval-runner/pom.xml");

  for (const artifact of [
    "jackson-databind",
    "swagger-parser-v3",
    "json-schema-validator",
    "maven-shade-plugin",
  ]) {
    assert.match(pom, new RegExp(`<artifactId>${artifact}<\\/artifactId>`));
  }

  assert.doesNotMatch(pom, /<artifactId>swagger-parser<\/artifactId>/);
  assert.match(pom, /ServicesResourceTransformer/);
  assert.doesNotMatch(pom, /knowledge-service|ticket-agent-service|customer-bff|spring-ai-/);
});

test("each service exposes one production-facing runtime configuration", () => {
  const services = [
    "services/knowledge-service",
    "services/ticket-agent-service",
    "apps/customer-bff",
  ];

  for (const service of services) {
    const mainResources = path.join(projectRoot, service, "src/main/resources");
    const runtimeConfigs = readdirSync(mainResources)
      .filter((name) => /^application.*\.ya?ml$/.test(name))
      .sort();
    assert.deepEqual(runtimeConfigs, ["application.yml"], `${service} runtime configs`);

    const runtime = read(`${service}/src/main/resources/application.yml`);
    assert.doesNotMatch(runtime, /spring:\s*[\s\S]*?profiles:|on-profile:|local-lite|shared-dev/);
    assert.match(runtime, /import:\s*optional:file:\.env\[\.properties\]/);
    assert.equal(
      existsSync(path.join(projectRoot, service, "src/test/resources/application-test.yml")),
      true,
      `${service} must isolate deterministic defaults under src/test`,
    );
  }

  const knowledge = read("services/knowledge-service/src/main/resources/application.yml");
  assert.match(knowledge, /chat:\s*openai/);
  assert.match(knowledge, /embedding:\s*openai/);
  assert.match(knowledge, /\$\{JAVA_AI_CHAT_API_KEY}/);
  assert.match(knowledge, /\$\{JAVA_AI_POSTGRES_URL}/);
  assert.doesNotMatch(knowledge, /execution-mode|LOCAL_DISABLED|PROVIDER_PROTOCOL_FIXTURE/);

  const ticket = read("services/ticket-agent-service/src/main/resources/application.yml");
  assert.match(ticket, /persistence:\s*[\s\S]*?mode:\s*jdbc/);
  assert.match(ticket, /\$\{JAVA_AI_TICKET_DB_URL}/);
  assert.match(ticket, /downstream-enabled:\s*true/);

  const bff = read("apps/customer-bff/src/main/resources/application.yml");
  assert.match(bff, /\$\{JAVA_AI_TOKEN_EXCHANGE_ENDPOINT}/);
  assert.match(bff, /\$\{JAVA_AI_KNOWLEDGE_BASE_URL}/);
});

test("build verification is cross-platform and self-contained", {
  skip: process.platform === "win32",
}, () => {
  const shellPath = path.join(projectRoot, "scripts/verify-build.sh");
  const shell = read("scripts/verify-build.sh");
  const powershell = read("scripts/verify-build.ps1");

  assert.notEqual(statSync(shellPath).mode & 0o111, 0);
  for (const content of [shell, powershell]) {
    assert.match(content, /verify-unit/);
    assert.doesNotMatch(content, /node(?:\.exe)?\s+--test/);
    assert.doesNotMatch(content, /docker|testcontainers/i);
  }
});

test("CI proves real JDK 21 and separate JDK 8 builds", () => {
  const verifyWorkflow = read(".github/workflows/verify.yml");

  assert.match(verifyWorkflow, /node-version:\s*["']?24/);
  assert.match(verifyWorkflow, /java-version:\s*["']?21/);
  assert.match(verifyWorkflow, /java-version:\s*["']?8/);
  assert.match(verifyWorkflow, /JAVA_AI_MAIN_JAVA_HOME=\$JAVA_HOME/);
  assert.match(verifyWorkflow, /JAVA_AI_JDK8_HOME=\$JAVA_HOME/);
  assert.match(verifyWorkflow, /verify-build\.sh/);
});

test("pull requests reject newly introduced high-severity dependency vulnerabilities", () => {
  const workflow = read(".github/workflows/dependency-review.yml");

  assert.match(workflow, /pull_request:/);
  assert.match(workflow, /actions\/dependency-review-action@v5/);
  assert.match(workflow, /fail-on-severity:\s*high/);
  assert.match(workflow, /contents:\s*read/);
});

test("Failsafe loads compiled classes instead of the repackaged Boot jar", () => {
  const pom = read("services/knowledge-service/pom.xml");

  assert.match(pom, /<artifactId>maven-failsafe-plugin<\/artifactId>/);
  assert.match(
    pom,
    /<classesDirectory>\$\{project\.build\.outputDirectory\}<\/classesDirectory>/,
  );
  assert.match(pom, /<include>\*\*\/PgVectorExternalIT\.java<\/include>/);
  assert.doesNotMatch(pom, /<include>\*\*\/LiveModelSmokeIT\.java<\/include>/);
});

test("GitHub workflows use actions backed by Node 24", () => {
  for (const workflow of [
    ".github/workflows/verify.yml",
    ".github/workflows/live-model-smoke.yml",
    ".github/workflows/pgvector-integration.yml",
  ]) {
    const content = read(workflow);
    assert.doesNotMatch(content, /actions\/(?:checkout|setup-java|setup-node)@v4/);
    assert.match(content, /actions\/checkout@v6/);
    assert.match(content, /actions\/setup-java@v5/);
  }

  assert.match(read(".github/workflows/verify.yml"), /actions\/setup-node@v6/);
  assert.match(
    read(".github/workflows/live-model-smoke.yml"),
    /actions\/upload-artifact@v7/,
  );
});
