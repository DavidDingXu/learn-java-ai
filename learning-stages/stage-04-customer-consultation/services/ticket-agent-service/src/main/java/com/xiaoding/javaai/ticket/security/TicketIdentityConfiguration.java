package com.xiaoding.javaai.ticket.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class TicketIdentityConfiguration {

    @Bean
    @ConditionalOnProperty(
            name = "java-ai.security.mode", havingValue = "fixed", matchIfMissing = true)
    TicketIdentityProvider fixedTicketIdentityProvider(
            @Value("${java-ai.security.fixed.tenant-id:tenant-a}") String tenantId,
            @Value("${java-ai.security.fixed.subject:local-user}") String subject,
            @Value("${java-ai.security.fixed.roles:TICKET_OPERATOR}") String[] roles,
            @Value("${java-ai.security.fixed.departments:support}") String[] departments
    ) {
        return new FixedTicketIdentityProvider(
                tenantId, subject, normalized(roles), normalized(departments));
    }

    @Bean
    @ConditionalOnProperty(name = "java-ai.security.mode", havingValue = "jwt")
    TicketIdentityProvider jwtTicketIdentityProvider() {
        return new JwtTicketIdentityProvider(new DelegatedTicketIdentityFactory());
    }

    private static List<String> normalized(String[] values) {
        return Arrays.stream(values)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }
}
