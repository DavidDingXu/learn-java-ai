package cn.dingxu.javaai.observability.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record QualityReport(
        String scenario,
        int total,
        int bad,
        BigDecimal badRate,
        List<FeedbackRecord> badCases
) {
    public QualityReport {
        if (scenario == null || scenario.isBlank()) {
            throw new IllegalArgumentException("scenario must not be blank");
        }
        badRate = badRate == null ? BigDecimal.ZERO : badRate;
        badCases = badCases == null ? List.of() : List.copyOf(badCases);
    }

    public static QualityReport from(String scenario, List<FeedbackRecord> records) {
        List<FeedbackRecord> safeRecords = records == null ? List.of() : List.copyOf(records);
        List<FeedbackRecord> badCases = safeRecords.stream()
                .filter(FeedbackRecord::bad)
                .toList();
        BigDecimal badRate = safeRecords.isEmpty()
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(badCases.size()).divide(BigDecimal.valueOf(safeRecords.size()), 4, RoundingMode.HALF_UP)
                        .stripTrailingZeros();
        return new QualityReport(scenario, safeRecords.size(), badCases.size(), badRate, badCases);
    }
}
