package cn.dingxu.javaai.enterprise.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class EnterpriseRagProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnterpriseRagProjectApplication.class, args);
    }

    @Bean
    EnterpriseRagApplicationService enterpriseRagApplicationService() {
        return EnterpriseRagApplicationService.seeded();
    }
}
