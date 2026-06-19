package cn.dingxu.javaai.agent;

import cn.dingxu.javaai.agent.service.context.ContextBudget;
import cn.dingxu.javaai.agent.service.context.ContextEngineeringService;
import cn.dingxu.javaai.agent.service.context.ContextSliceType;
import cn.dingxu.javaai.agent.service.memory.AgentContext;
import cn.dingxu.javaai.agent.service.memory.BusinessSnapshot;
import cn.dingxu.javaai.agent.service.memory.MemoryEntry;
import cn.dingxu.javaai.agent.service.memory.MemoryEntryType;
import cn.dingxu.javaai.agent.service.memory.MemoryScope;
import cn.dingxu.javaai.agent.service.memory.UserPreference;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ContextEngineeringServiceTest {

    private final ContextEngineeringService service = new ContextEngineeringService();

    @Test
    void keepsBusinessSnapshotAndNewestConversationWithinBudget() {
        AgentContext context = new AgentContext(
                "tenant-a",
                "session-1",
                "T-1001",
                "u1001",
                List.of(
                        entry("旧消息，已经不应该进入最终上下文", "2026-06-07T10:00:00Z"),
                        entry("客户第一次说明退款原因", "2026-06-07T10:01:00Z"),
                        entry("客服补充订单已经发货", "2026-06-07T10:02:00Z")
                ),
                Optional.of(new BusinessSnapshot(
                        "tenant-a",
                        "session-1",
                        "T-1001",
                        Map.of("ticketStatus", "OPEN", "orderStatus", "SHIPPED"),
                        Instant.parse("2026-06-07T10:03:00Z")
                )),
                List.of(new UserPreference(
                        "tenant-a",
                        "u1001",
                        MemoryScope.LONG_TERM,
                        "replyStyle",
                        "先结论后依据",
                        Instant.parse("2026-06-07T10:04:00Z")
                )),
                List.of("conversation", "businessSnapshot", "preferences")
        );

        var engineered = service.assemble(context, new ContextBudget(70, 2));

        assertThat(engineered.slices()).extracting(slice -> slice.type())
                .contains(ContextSliceType.BUSINESS_SNAPSHOT, ContextSliceType.CONVERSATION, ContextSliceType.PREFERENCE);
        assertThat(engineered.slices()).noneMatch(slice -> slice.content().contains("旧消息"));
        assertThat(engineered.totalEstimatedTokens()).isLessThanOrEqualTo(70);
        assertThat(engineered.sources()).containsExactly("businessSnapshot", "conversation", "preferences");
    }

    @Test
    void summarizesOverflowConversationInsteadOfDroppingAllHistory() {
        AgentContext context = new AgentContext(
                "tenant-a",
                "session-1",
                "T-1001",
                "u1001",
                List.of(
                        entry("第一轮：客户说商品已签收但要求退款", "2026-06-07T10:00:00Z"),
                        entry("第二轮：客服确认订单金额 5000 元", "2026-06-07T10:01:00Z"),
                        entry("第三轮：客户要求今天必须处理", "2026-06-07T10:02:00Z"),
                        entry("第四轮：客服补充物流已发货", "2026-06-07T10:03:00Z")
                ),
                Optional.empty(),
                List.of(),
                List.of("conversation")
        );

        var engineered = service.assemble(context, new ContextBudget(45, 2));

        assertThat(engineered.slices()).anySatisfy(slice -> {
            assertThat(slice.type()).isEqualTo(ContextSliceType.CONVERSATION_SUMMARY);
            assertThat(slice.content()).contains("历史摘要");
        });
        assertThat(engineered.slices()).filteredOn(slice -> slice.type() == ContextSliceType.CONVERSATION)
                .hasSizeLessThanOrEqualTo(2);
        assertThat(engineered.totalEstimatedTokens()).isLessThanOrEqualTo(45);
    }

    private MemoryEntry entry(String content, String occurredAt) {
        return new MemoryEntry(
                "tenant-a",
                "session-1",
                "u1001",
                MemoryEntryType.USER_MESSAGE,
                content,
                Map.of(),
                Instant.parse(occurredAt)
        );
    }
}
