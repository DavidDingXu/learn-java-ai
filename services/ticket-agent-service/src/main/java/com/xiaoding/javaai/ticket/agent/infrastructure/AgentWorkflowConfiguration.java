package com.xiaoding.javaai.ticket.agent.infrastructure;

import com.xiaoding.javaai.ticket.agent.application.AgentAuditTrail;
import com.xiaoding.javaai.ticket.agent.application.AgentReadToolExecutor;
import com.xiaoding.javaai.ticket.agent.application.AgentRunAdmission;
import com.xiaoding.javaai.ticket.agent.application.AgentTelemetry;
import com.xiaoding.javaai.ticket.agent.application.BusinessToolCatalog;
import com.xiaoding.javaai.ticket.agent.application.ConfirmAgentAction;
import com.xiaoding.javaai.ticket.agent.application.ConfirmationDecisionStore;
import com.xiaoding.javaai.ticket.agent.application.InMemoryAgentAuditTrail;
import com.xiaoding.javaai.ticket.agent.application.InMemoryConfirmationDecisionStore;
import com.xiaoding.javaai.ticket.agent.application.LegacyWriteToolExecutor;
import com.xiaoding.javaai.ticket.agent.application.RunAgentTask;
import com.xiaoding.javaai.ticket.agent.application.TicketAgentOrchestrator;
import com.xiaoding.javaai.ticket.agent.application.TicketAgentPlanner;
import com.xiaoding.javaai.ticket.agent.application.ToolConfirmationService;
import com.xiaoding.javaai.ticket.agent.application.SemaphoreAgentRunAdmission;
import com.xiaoding.javaai.ticket.security.ConfirmationActorFactory;
import com.xiaoding.javaai.ticket.task.AgentTaskRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Configuration
class AgentWorkflowConfiguration {

    @Bean
    AgentTelemetry agentTelemetry(MeterRegistry meterRegistry) {
        return new MicrometerAgentTelemetry(meterRegistry);
    }

    @Bean
    AgentRunAdmission agentRunAdmission(
            @Value("${java-ai.agent.max-concurrent-runs:8}") int maxConcurrentRuns
    ) {
        return new SemaphoreAgentRunAdmission(maxConcurrentRuns);
    }

    @Bean
    ConfirmationActorFactory confirmationActorFactory() {
        return new ConfirmationActorFactory();
    }

    @Bean
    @ConditionalOnProperty(
            name = "java-ai.persistence.mode", havingValue = "memory", matchIfMissing = true)
    AgentAuditTrail agentAuditTrail() {
        return new InMemoryAgentAuditTrail();
    }

    @Bean
    @ConditionalOnProperty(
            name = "java-ai.persistence.mode", havingValue = "memory", matchIfMissing = true)
    ConfirmationDecisionStore confirmationDecisionStore() {
        return new InMemoryConfirmationDecisionStore();
    }

    @Bean
    @ConditionalOnProperty(name = "java-ai.persistence.mode", havingValue = "jdbc")
    AgentAuditTrail jdbcAgentAuditTrail(
            DataSource dataSource,
            PlatformTransactionManager transactionManager
    ) {
        return new JdbcAgentAuditTrail(
                new JdbcTemplate(dataSource), new TransactionTemplate(transactionManager));
    }

    @Bean
    @ConditionalOnProperty(name = "java-ai.persistence.mode", havingValue = "jdbc")
    ConfirmationDecisionStore jdbcConfirmationDecisionStore(
            DataSource dataSource,
            PlatformTransactionManager transactionManager,
            Clock ticketClock,
            @Value("${java-ai.persistence.confirmation-lease:30s}") Duration confirmationLease
    ) {
        return new JdbcConfirmationDecisionStore(
                new JdbcTemplate(dataSource), new TransactionTemplate(transactionManager),
                ticketClock, confirmationLease);
    }

    @Bean
    BusinessToolCatalog businessToolCatalog(
            @Value("${java-ai.agent.allowed-queues:refund-review,tier-2}") String[] allowedQueues
    ) {
        Set<String> queues = Arrays.stream(allowedQueues)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        return BusinessToolCatalog.standard(queues);
    }

    @Bean
    RunAgentTask runAgentTask(
            AgentTaskRepository repository,
            TicketAgentPlanner planner,
            BusinessToolCatalog toolCatalog,
            AgentReadToolExecutor readToolExecutor,
            AgentAuditTrail auditTrail,
            AgentTelemetry telemetry,
            Clock ticketClock,
            @Value("${java-ai.agent.confirmation-ttl:15m}") Duration confirmationTtl,
            @Value("${java-ai.agent.max-steps:4}") int maxSteps
    ) {
        return new TicketAgentOrchestrator(
                repository,
                planner,
                toolCatalog,
                readToolExecutor,
                auditTrail,
                () -> UUID.randomUUID().toString(),
                () -> UUID.randomUUID().toString(),
                ticketClock,
                confirmationTtl,
                maxSteps,
                telemetry);
    }

    @Bean
    ConfirmAgentAction confirmAgentAction(
            AgentTaskRepository repository,
            ConfirmationDecisionStore decisions,
            LegacyWriteToolExecutor executor,
            AgentAuditTrail auditTrail,
            Clock ticketClock,
            AgentTelemetry telemetry
    ) {
        return new ToolConfirmationService(
                repository, decisions, executor, auditTrail, ticketClock, telemetry);
    }
}
