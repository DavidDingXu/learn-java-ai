package cn.dingxu.javaai.observability.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class FeedbackStore {

    private final List<FeedbackRecord> records = new ArrayList<>();

    public synchronized FeedbackRecord record(String traceId, String scenario, String rating, String reason) {
        FeedbackRecord record = new FeedbackRecord(traceId, scenario, rating, reason, Instant.now());
        records.add(record);
        return record;
    }

    public synchronized QualityReport reportByScenario(String scenario) {
        List<FeedbackRecord> matched = records.stream()
                .filter(record -> record.scenario().equals(scenario))
                .toList();
        return QualityReport.from(scenario, matched);
    }
}
