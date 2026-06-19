package com.xiaoding.javaai.eval;

import com.xiaoding.javaai.eval.service.EvalCase;
import com.xiaoding.javaai.eval.service.EvalReport;
import com.xiaoding.javaai.eval.service.EvalRunner;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EvalRunnerTest {

    @Test
    void calculatesPassRateForGoldenSet() {
        EvalRunner runner = new EvalRunner();

        EvalReport report = runner.run(List.of(
                new EvalCase("case-1", "退款怎么处理", "核对物流状态", "需要先核对物流状态，再决定是否转人工。"),
                new EvalCase("case-2", "能直接关闭工单吗", "人工确认", "关闭工单需要人工确认。")
        ));

        assertThat(report.total()).isEqualTo(2);
        assertThat(report.passed()).isEqualTo(2);
        assertThat(report.passRate()).isEqualByComparingTo("1.0");
    }
}
