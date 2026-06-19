package cn.dingxu.javaai.gateway.service;

import java.util.List;

public interface GatewayConversationMemory {

    void append(String userId, String message);

    List<String> recent(String userId, int limit);
}
