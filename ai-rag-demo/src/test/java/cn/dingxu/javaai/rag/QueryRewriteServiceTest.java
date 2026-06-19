package cn.dingxu.javaai.rag;

import cn.dingxu.javaai.rag.service.QueryRewriteResult;
import cn.dingxu.javaai.rag.service.QueryRewriteService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QueryRewriteServiceTest {

    @Test
    void keepsOriginalQueryAndAddsBusinessSpecificQueries() {
        QueryRewriteService service = new QueryRewriteService();

        QueryRewriteResult result = service.rewrite("客户申请退款，但订单已经发货，应该怎么处理？");

        assertThat(result.originalQuery()).isEqualTo("客户申请退款，但订单已经发货，应该怎么处理？");
        assertThat(result.queries()).contains(
                "客户申请退款，但订单已经发货，应该怎么处理？",
                "发货后退款处理规则",
                "退款物流状态核对要求"
        );
        assertThat(result.reasons()).contains("detected refund scenario", "detected shipping status");
    }

    @Test
    void deduplicatesRewrittenQueries() {
        QueryRewriteService service = new QueryRewriteService();

        QueryRewriteResult result = service.rewrite("退款 退款 发货 发货");

        assertThat(result.queries()).doesNotHaveDuplicates();
    }
}
