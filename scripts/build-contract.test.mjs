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

test("each service has one reader-facing runtime config with replaceable adapters", () => {
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
    assert.doesNotMatch(runtime, /profiles:\s*\n\s+default:|on-profile:/);
    assert.doesNotMatch(runtime, /\.env|\$\{JAVA_AI_[A-Z0-9_]+/);
    assert.equal(
      existsSync(path.join(projectRoot, service, "src/test/resources/application-test.yml")),
      true,
      `${service} must isolate deterministic defaults under src/test`,
    );
  }

  const knowledge = read("services/knowledge-service/src/main/resources/application.yml");
  const sharedModelConfig = read("config/application.yml");
  assert.match(knowledge, /import:[\s\S]*?config\/application\.yml/);
  assert.match(knowledge, /mode:\s*classpath/);
  assert.match(knowledge, /mode:\s*fixed/);
  assert.match(knowledge, /password:\s*replace-with-your-database-password/);
  assert.match(sharedModelConfig, /chat:\s*openai/);
  assert.match(sharedModelConfig, /embedding:\s*openai/);
  assert.match(sharedModelConfig, /api-key:\s*replace-with-your-api-key/);
  assert.match(sharedModelConfig, /base-url:\s*https:\/\/api\.openai\.com\/v1/);
  assert.doesNotMatch(knowledge, /execution-mode|LOCAL_DISABLED|PROVIDER_PROTOCOL_FIXTURE/);
  assert.doesNotMatch(knowledge, /external-integrations-enabled/);

  const ticket = read("services/ticket-agent-service/src/main/resources/application.yml");
  assert.match(ticket, /persistence:[\s\S]*?mode:\s*memory/);
  assert.match(ticket, /knowledge-tool:[\s\S]*?mode:\s*http/);
  assert.match(ticket, /write-tool:[\s\S]*?mode:\s*memory/);
  assert.match(ticket, /security:[\s\S]*?mode:\s*fixed/);
  assert.doesNotMatch(ticket, /external-integrations-enabled/);

  const bff = read("apps/customer-bff/src/main/resources/application.yml");
  const consultationConfiguration = read(
    "apps/customer-bff/src/main/java/com/xiaoding/javaai/customer/consultation/infrastructure/CustomerConsultationConfiguration.java",
  );
  assert.match(bff, /security:[\s\S]*?mode:\s*fixed/);
  assert.match(bff, /delegation-mode:\s*local/);
  assert.match(bff, /base-url:\s*http:\/\/localhost:8081/);
  assert.match(bff, /base-url:\s*http:\/\/localhost:8082/);
  assert.match(bff, /knowledge:[\s\S]*?base-url:\s*http:\/\/localhost:8081[\s\S]*?timeout:\s*35s/);
  assert.match(bff, /stream-idle-timeout:\s*30s/);
  assert.match(bff, /stream-total-timeout:\s*2m/);
  assert.match(consultationConfiguration, /java-ai\.downstream\.knowledge\.timeout:35s/);
  assert.match(consultationConfiguration, /stream-idle-timeout:30s/);
  assert.match(consultationConfiguration, /stream-total-timeout:2m/);

  for (const startupTest of [
    "services/knowledge-service/src/test/java/com/xiaoding/javaai/knowledge/KnowledgeServiceDefaultStartupTest.java",
    "services/ticket-agent-service/src/test/java/com/xiaoding/javaai/ticket/TicketAgentDefaultStartupTest.java",
    "apps/customer-bff/src/test/java/com/xiaoding/javaai/customer/CustomerBffDefaultStartupTest.java",
  ]) {
    assert.equal(existsSync(path.join(projectRoot, startupTest)), true, startupTest);
  }

  for (const service of services) {
    const mainPackage = service === "apps/customer-bff"
      ? "apps/customer-bff/src/main/java/com/xiaoding/javaai/customer"
      : `${service}/src/main/java/com/xiaoding/javaai/${
        service.includes("knowledge") ? "knowledge" : "ticket"
      }`;
    assert.equal(
      existsSync(path.join(projectRoot, mainPackage, "ProductionConfigurationValidator.java")),
      false,
      `${service} must validate the selected adapter instead of a production profile`,
    );
  }
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
