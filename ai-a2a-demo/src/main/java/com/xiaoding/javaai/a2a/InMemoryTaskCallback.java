package com.xiaoding.javaai.a2a;

import java.util.ArrayList;
import java.util.List;

public class InMemoryTaskCallback implements TaskCallback {

    private final List<AgentTask> received = new ArrayList<>();

    @Override
    public synchronized void onCompleted(AgentTask task) {
        received.add(task);
    }

    public synchronized List<AgentTask> received() {
        return List.copyOf(received);
    }
}
