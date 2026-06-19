package cn.dingxu.javaai.gateway.client;

public interface ModelClient {

    String modelName();

    default int priority() {
        return 100;
    }

    String chat(String prompt);
}
