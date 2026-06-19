package cn.dingxu.javaai.eval.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class HarnessExperimentRunner {

    public HarnessExperimentReport run(List<HarnessExperimentCase> cases,
                                       List<HarnessStrategyObservation> observations) {
        Map<String, List<HarnessStrategyObservation>> observationsByCase =
                (observations == null ? List.<HarnessStrategyObservation>of() : observations).stream()
                        .collect(Collectors.groupingBy(HarnessStrategyObservation::caseId));

        List<HarnessExperimentResult> results = (cases == null ? List.<HarnessExperimentCase>of() : cases).stream()
                .map(evalCase -> evaluate(evalCase, observationsByCase.getOrDefault(evalCase.caseId(), List.of())))
                .toList();
        return new HarnessExperimentReport(results.size(), 0, results);
    }

    private HarnessExperimentResult evaluate(HarnessExperimentCase evalCase,
                                             List<HarnessStrategyObservation> observations) {
        Map<String, HarnessStrategyObservation> byStrategy = observations.stream()
                .collect(Collectors.toMap(HarnessStrategyObservation::strategy, Function.identity(), (left, right) -> right));
        HarnessStrategyObservation baseline = byStrategy.get("baseline");
        HarnessStrategyObservation candidate = byStrategy.get("candidate");
        if (baseline == null || candidate == null) {
            return new HarnessExperimentResult(
                    evalCase.caseId(),
                    evalCase.scenario(),
                    "baseline",
                    false,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    "missing baseline or candidate observation"
            );
        }

        BigDecimal qualityDelta = candidate.qualityScore().subtract(baseline.qualityScore()).stripTrailingZeros();
        BigDecimal costIncreaseRate = increaseRate(candidate.costUnits(), baseline.costUnits());
        BigDecimal latencyIncreaseRate = increaseRate(candidate.latencyMillis(), baseline.latencyMillis());

        if (qualityDelta.compareTo(evalCase.minQualityImprovement()) < 0) {
            return result(evalCase, "baseline", false, qualityDelta, costIncreaseRate, latencyIncreaseRate,
                    "quality improvement below threshold");
        }
        if (costIncreaseRate.compareTo(evalCase.maxCostIncreaseRate()) > 0) {
            return result(evalCase, "baseline", false, qualityDelta, costIncreaseRate, latencyIncreaseRate,
                    "cost increase exceeds limit");
        }
        if (latencyIncreaseRate.compareTo(evalCase.maxLatencyIncreaseRate()) > 0) {
            return result(evalCase, "baseline", false, qualityDelta, costIncreaseRate, latencyIncreaseRate,
                    "latency increase exceeds limit");
        }
        return result(evalCase, "candidate", true, qualityDelta, costIncreaseRate, latencyIncreaseRate,
                "candidate passes quality cost latency guardrails");
    }

    private HarnessExperimentResult result(HarnessExperimentCase evalCase,
                                           String winner,
                                           boolean promotable,
                                           BigDecimal qualityDelta,
                                           BigDecimal costIncreaseRate,
                                           BigDecimal latencyIncreaseRate,
                                           String reason) {
        return new HarnessExperimentResult(
                evalCase.caseId(),
                evalCase.scenario(),
                winner,
                promotable,
                qualityDelta,
                costIncreaseRate,
                latencyIncreaseRate,
                reason
        );
    }

    private BigDecimal increaseRate(int candidate, int baseline) {
        if (baseline <= 0) {
            return candidate <= 0 ? BigDecimal.ZERO : BigDecimal.ONE;
        }
        return BigDecimal.valueOf(candidate - baseline)
                .divide(BigDecimal.valueOf(baseline), 4, RoundingMode.HALF_UP)
                .stripTrailingZeros();
    }
}
