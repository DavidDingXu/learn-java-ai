package com.xiaoding.javaai.ticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TicketAgentServiceApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(TicketAgentServiceApplication.class);
        application.addInitializers(context ->
                ProductionConfigurationValidator.validate(context.getEnvironment()));
        application.run(args);
    }
}
