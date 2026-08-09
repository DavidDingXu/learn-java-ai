package com.xiaoding.javaai.ticket.task;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name = "java-ai.persistence.mode", havingValue = "jdbc")
class TicketJdbcConfiguration {

    @Bean(destroyMethod = "close")
    DataSource ticketDataSource(
            @Value("${java-ai.persistence.jdbc.url}") String url,
            @Value("${java-ai.persistence.jdbc.username}") String username,
            @Value("${java-ai.persistence.jdbc.password}") String password,
            @Value("${java-ai.persistence.jdbc.maximum-pool-size:10}") int maximumPoolSize
    ) {
        HikariConfig config = new HikariConfig();
        config.setPoolName("ticket-postgres");
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(maximumPoolSize);
        config.setConnectionTimeout(3000);
        HikariDataSource dataSource = new HikariDataSource(config);
        Flyway.configure().dataSource(dataSource).load().migrate();
        return dataSource;
    }

    @Bean
    PlatformTransactionManager ticketTransactionManager(DataSource ticketDataSource) {
        return new DataSourceTransactionManager(ticketDataSource);
    }
}
