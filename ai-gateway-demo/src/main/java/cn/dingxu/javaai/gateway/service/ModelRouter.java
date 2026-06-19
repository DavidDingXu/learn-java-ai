package cn.dingxu.javaai.gateway.service;

import cn.dingxu.javaai.gateway.client.ModelClient;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class ModelRouter {

    private final List<ModelClient> clients;

    public ModelRouter(List<ModelClient> clients) {
        if (clients == null || clients.isEmpty()) {
            throw new IllegalArgumentException("model clients must not be empty");
        }
        this.clients = clients.stream()
                .sorted(Comparator.comparing(ModelClient::priority))
                .toList();
    }

    public List<ModelClient> candidates() {
        return clients;
    }
}
