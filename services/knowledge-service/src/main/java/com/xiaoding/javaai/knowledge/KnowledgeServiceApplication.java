package com.xiaoding.javaai.knowledge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KnowledgeServiceApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(KnowledgeServiceApplication.class);
        application.addInitializers(context ->
                ProductionConfigurationValidator.validate(context.getEnvironment()));
        application.run(args);
    }
}
