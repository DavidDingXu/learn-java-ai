package com.xiaoding.javaai.knowledge;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class SecurityConfiguration {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            ObjectProvider<ReactiveJwtDecoder> jwtDecoderProvider,
            @Value("${java-ai.security.mode:fixed}") String securityMode
    ) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)));

        if ("jwt".equals(securityMode)) {
            ReactiveJwtDecoder jwtDecoder = jwtDecoderProvider.getIfAvailable();
            if (jwtDecoder == null) {
                throw new IllegalStateException("JWT security is enabled but no ReactiveJwtDecoder is configured");
            }
            http.oauth2ResourceServer(oauth2 -> oauth2
                            .authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
                            .jwt(jwt -> jwt.jwtDecoder(jwtDecoder)))
                    .authorizeExchange(exchanges -> exchanges
                            .pathMatchers("/actuator/health", "/error").permitAll()
                            .pathMatchers("/api/v1/knowledge/answers/**")
                            .hasAuthority("SCOPE_knowledge:answer")
                            .pathMatchers("/api/v1/knowledge/documents/**")
                            .hasAuthority("SCOPE_knowledge:write")
                            .pathMatchers("/internal/v1/knowledge/index-tasks/run-once")
                            .hasAuthority("SCOPE_knowledge:index")
                            .pathMatchers("/internal/v1/knowledge/retrieval/evaluations")
                            .hasAuthority("SCOPE_knowledge:eval")
                            .anyExchange().denyAll());
        } else if ("fixed".equals(securityMode)) {
            http.authorizeExchange(exchanges -> exchanges
                    .pathMatchers("/actuator/health", "/error").permitAll()
                    .pathMatchers("/api/v1/knowledge/**", "/internal/v1/knowledge/**").permitAll()
                    .anyExchange().denyAll());
        } else {
            throw new IllegalStateException(
                    "java-ai.security.mode must be either fixed or jwt");
        }
        return http.build();
    }
}
