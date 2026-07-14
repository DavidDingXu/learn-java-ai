package com.xiaoding.javaai.customer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
class SecurityConfiguration {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            ObjectProvider<ReactiveJwtDecoder> jwtDecoderProvider,
            @Value("${java-ai.security.customer-jwt.enabled:false}") boolean jwtEnabled
    ) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)));
        if (jwtEnabled) {
            ReactiveJwtDecoder decoder = jwtDecoderProvider.getIfAvailable();
            if (decoder == null) {
                throw new IllegalStateException("Customer JWT security is enabled but no decoder is configured");
            }
            http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtDecoder(decoder)))
                    .authorizeExchange(exchanges -> exchanges
                            .pathMatchers("/actuator/health", "/actuator/env", "/error").permitAll()
                            .pathMatchers("/api/v1/customer/consultations/**")
                            .hasAuthority("SCOPE_consultation:use")
                            .anyExchange().denyAll());
        } else {
            http.authorizeExchange(exchanges -> exchanges
                    .pathMatchers("/actuator/health", "/actuator/env", "/error").permitAll()
                    .anyExchange().denyAll());
        }
        return http.build();
    }
}
