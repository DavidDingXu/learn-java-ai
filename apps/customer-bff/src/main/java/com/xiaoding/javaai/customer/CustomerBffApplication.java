package com.xiaoding.javaai.customer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CustomerBffApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(CustomerBffApplication.class);
        application.addInitializers(context ->
                ProductionConfigurationValidator.validate(context.getEnvironment()));
        application.run(args);
    }
}
