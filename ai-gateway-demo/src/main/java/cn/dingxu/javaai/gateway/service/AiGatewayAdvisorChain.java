package cn.dingxu.javaai.gateway.service;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class AiGatewayAdvisorChain {

    private final List<AiGatewayAdvisor> advisors;

    public AiGatewayAdvisorChain(List<AiGatewayAdvisor> advisors) {
        this.advisors = advisors == null ? List.of() : advisors.stream()
                .sorted(Comparator.comparing(AiGatewayAdvisor::name))
                .toList();
    }

    public AiGatewayExchange apply(AiGatewayExchange exchange) {
        AiGatewayExchange current = exchange;
        for (AiGatewayAdvisor advisor : advisors) {
            current = advisor.advise(current);
        }
        return current;
    }
}
