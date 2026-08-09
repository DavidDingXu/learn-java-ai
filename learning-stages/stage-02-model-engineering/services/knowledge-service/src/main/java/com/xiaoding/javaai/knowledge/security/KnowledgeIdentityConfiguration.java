package com.xiaoding.javaai.knowledge.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KnowledgeIdentityConfiguration {

    @Bean
    KnowledgeAccessScopeProvider knowledgeAccessScopeProvider(
            @Value("${java-ai.security.mode:fixed}") String securityMode
    ) {
        return switch (securityMode) {
            case "fixed" -> new FixedKnowledgeAccessScopeProvider();
            case "jwt" -> new JwtKnowledgeAccessScopeProvider();
            default -> throw new IllegalStateException(
                    "java-ai.security.mode must be either fixed or jwt");
        };
    }
}
