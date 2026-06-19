package cn.dingxu.javaai.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "cn.dingxu.javaai")
public class AiGatewayDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiGatewayDemoApplication.class, args);
    }
}
