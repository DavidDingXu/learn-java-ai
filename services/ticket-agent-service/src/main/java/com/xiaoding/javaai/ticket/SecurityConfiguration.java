package com.xiaoding.javaai.ticket;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectProvider<JwtDecoder> decoderProvider,
            @Value("${java-ai.security.jwt.enabled:false}") boolean jwtEnabled
    ) throws Exception {
        AuthenticationEntryPoint unauthorized = (request, response, error) ->
                response.sendError(HttpStatus.UNAUTHORIZED.value());
        http.csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(unauthorized));
        if (jwtEnabled) {
            JwtDecoder decoder = decoderProvider.getIfAvailable();
            if (decoder == null) throw new IllegalStateException("Ticket JWT security is enabled but no decoder is configured");
            http.oauth2ResourceServer(oauth2 -> oauth2
                            .authenticationEntryPoint(unauthorized)
                            .jwt(jwt -> jwt.decoder(decoder)))
                    .authorizeHttpRequests(authorize -> authorize
                            .requestMatchers(
                                    "/actuator/health", "/actuator/prometheus", "/actuator/env", "/error")
                            .permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/v1/agent/tasks")
                            .hasAuthority("SCOPE_ticket:task:create")
                            .requestMatchers(HttpMethod.POST, "/api/v1/agent/tasks/*/runs")
                            .hasAuthority("SCOPE_ticket:task:run")
                            .requestMatchers(HttpMethod.PUT, "/api/v1/agent/tasks/*/confirmation")
                            .hasAuthority("SCOPE_ticket:confirmation:decide")
                            .requestMatchers(HttpMethod.GET, "/api/v1/agent/tasks/**")
                            .hasAuthority("SCOPE_ticket:task:read")
                            .anyRequest().denyAll());
        } else {
            http.authorizeHttpRequests(authorize -> authorize
                    .requestMatchers(
                            "/actuator/health", "/actuator/prometheus", "/actuator/env", "/error")
                    .permitAll()
                    .anyRequest().denyAll());
        }
        return http.build();
    }
}
