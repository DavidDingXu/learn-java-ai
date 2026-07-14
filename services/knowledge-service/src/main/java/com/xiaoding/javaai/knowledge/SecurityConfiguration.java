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
            @Value("${java-ai.security.jwt.enabled:false}") boolean jwtEnabled,
            @Value("${java-ai.security.allow-insecure-local-http:false}") boolean allowInsecureLocalHttp
    ) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)));

        if (jwtEnabled) {
            ReactiveJwtDecoder jwtDecoder = jwtDecoderProvider.getIfAvailable();
            if (jwtDecoder == null) {
                throw new IllegalStateException("JWT security is enabled but no ReactiveJwtDecoder is configured");
            }
            http.oauth2ResourceServer(oauth2 -> oauth2
                            .authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
                            .jwt(jwt -> jwt.jwtDecoder(jwtDecoder)))
                    .authorizeExchange(exchanges -> exchanges
                            .pathMatchers("/actuator/health", "/actuator/env", "/error").permitAll()
                            .pathMatchers("/api/v1/knowledge/answers/**")
                            .hasAuthority("SCOPE_knowledge:answer")
                            .pathMatchers("/internal/v1/knowledge/retrieval/evaluations")
                            .hasAuthority("SCOPE_knowledge:eval")
                            .anyExchange().denyAll());
        } else {
            http.authorizeExchange(exchanges -> {
                exchanges.pathMatchers("/actuator/health", "/actuator/env", "/error").permitAll();
                if (allowInsecureLocalHttp) {
                    exchanges.pathMatchers("/api/v1/knowledge/answers/**").permitAll();
                }
                exchanges.anyExchange().denyAll();
            });
        }
        return http.build();
    }
}
