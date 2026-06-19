package com.xiaoding.javaai.helpdesk.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class HelpdeskAgentProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(HelpdeskAgentProjectApplication.class, args);
    }

    @Bean
    HelpdeskAgentApplicationService helpdeskAgentApplicationService() {
        return HelpdeskAgentApplicationService.seeded();
    }
}
