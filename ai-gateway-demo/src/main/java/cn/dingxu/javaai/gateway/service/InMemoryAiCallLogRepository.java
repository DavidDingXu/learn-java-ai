package cn.dingxu.javaai.gateway.service;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Repository
public class InMemoryAiCallLogRepository implements AiCallLogRepository {

    private final List<AiCallLogEntry> entries = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void save(AiCallLogEntry entry) {
        entries.add(entry);
    }

    public List<AiCallLogEntry> findAll() {
        synchronized (entries) {
            return List.copyOf(entries);
        }
    }
}
