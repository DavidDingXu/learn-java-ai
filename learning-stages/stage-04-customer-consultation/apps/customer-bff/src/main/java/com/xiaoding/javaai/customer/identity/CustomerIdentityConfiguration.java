package com.xiaoding.javaai.customer.identity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CustomerIdentityConfiguration {

    @Bean
    @ConditionalOnProperty(
            name = "java-ai.security.mode", havingValue = "fixed", matchIfMissing = true)
    CustomerAccessTokenProvider fixedCustomerAccessTokenProvider(
            @Value("${java-ai.security.fixed.tenant-id:tenant-a}") String tenantId,
            @Value("${java-ai.security.fixed.subject:local-user}") String subject,
            @Value("${java-ai.security.fixed.roles:customer}") String[] roles,
            @Value("${java-ai.security.fixed.departments:support}") String[] departments
    ) {
        return new FixedCustomerAccessTokenProvider(
                tenantId, subject, normalized(roles), normalized(departments));
    }

    @Bean
    @ConditionalOnProperty(name = "java-ai.security.mode", havingValue = "jwt")
    CustomerAccessTokenProvider jwtCustomerAccessTokenProvider() {
        return new JwtCustomerAccessTokenProvider(new CustomerJwtIdentityFactory());
    }

    private static List<String> normalized(String[] values) {
        return Arrays.stream(values)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }
}
