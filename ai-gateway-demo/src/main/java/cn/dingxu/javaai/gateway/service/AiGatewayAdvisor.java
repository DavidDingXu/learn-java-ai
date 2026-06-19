package cn.dingxu.javaai.gateway.service;

public interface AiGatewayAdvisor {

    String name();

    AiGatewayExchange advise(AiGatewayExchange exchange);
}
