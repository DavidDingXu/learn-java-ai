package cn.dingxu.javaai.observability;

import cn.dingxu.javaai.observability.service.FeedbackRecord;
import cn.dingxu.javaai.observability.service.FeedbackStore;
import cn.dingxu.javaai.observability.service.QualityReport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FeedbackStoreTest {

    @Test
    void collectsBadCasesAndBuildsQualityReportByScenario() {
        FeedbackStore store = new FeedbackStore();

        store.record("trace-1", "ticket-advice", "bad", "引用了错误退款制度");
        store.record("trace-2", "ticket-advice", "good", "建议可用");
        store.record("trace-3", "policy-qa", "bad", "没有拒答");

        QualityReport report = store.reportByScenario("ticket-advice");

        assertThat(report.scenario()).isEqualTo("ticket-advice");
        assertThat(report.total()).isEqualTo(2);
        assertThat(report.bad()).isEqualTo(1);
        assertThat(report.badRate()).isEqualByComparingTo("0.5");
        assertThat(report.badCases()).extracting(FeedbackRecord::traceId).containsExactly("trace-1");
    }
}
