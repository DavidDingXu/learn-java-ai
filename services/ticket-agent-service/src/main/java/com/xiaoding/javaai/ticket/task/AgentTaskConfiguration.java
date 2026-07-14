package com.xiaoding.javaai.ticket.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.xiaoding.javaai.ticket.security.DelegatedTicketIdentityFactory;

import javax.sql.DataSource;
import java.time.Clock;
import java.util.UUID;

@Configuration
public class AgentTaskConfiguration {

    @Bean
    @ConditionalOnProperty(
            name = "java-ai.persistence.mode", havingValue = "memory", matchIfMissing = true)
    AgentTaskRepository agentTaskRepository() {
        return new InMemoryAgentTaskRepository();
    }

    @Bean
    @ConditionalOnProperty(name = "java-ai.persistence.mode", havingValue = "jdbc")
    AgentTaskRepository jdbcAgentTaskRepository(
            DataSource dataSource,
            PlatformTransactionManager transactionManager,
            ObjectMapper objectMapper
    ) {
        return new JdbcAgentTaskRepository(
                new JdbcTemplate(dataSource), new TransactionTemplate(transactionManager), objectMapper);
    }

    @Bean
    Clock ticketClock() {
        return Clock.systemUTC();
    }

    @Bean
    DelegatedTicketIdentityFactory delegatedTicketIdentityFactory() {
        return new DelegatedTicketIdentityFactory();
    }

    @Bean
    AgentTaskIntakeService agentTaskIntakeService(
            AgentTaskRepository repository,
            Clock ticketClock
    ) {
        return new AgentTaskIntakeService(() -> UUID.randomUUID().toString(), repository, ticketClock);
    }
}
