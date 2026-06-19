package com.xiaoding.javaai.agent;

import com.xiaoding.javaai.agent.service.memory.AgentContextAssembler;
import com.xiaoding.javaai.agent.service.memory.BusinessSnapshot;
import com.xiaoding.javaai.agent.service.memory.ConversationMemoryStore;
import com.xiaoding.javaai.agent.service.memory.MemoryEntry;
import com.xiaoding.javaai.agent.service.memory.MemoryEntryType;
import com.xiaoding.javaai.agent.service.memory.MemoryScope;
import com.xiaoding.javaai.agent.service.memory.UserPreference;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentContextAssemblerTest {

    @Test
    void assemblesContextFromConversationSnapshotAndPreferencesWithoutMixingSources() {
        ConversationMemoryStore store = new ConversationMemoryStore(4);
        store.append(new MemoryEntry("tenant-a", "session-1", "u1001", MemoryEntryType.USER_MESSAGE, "这个退款工单怎么处理？", Map.of("traceId", "trace-1"), Instant.parse("2026-06-07T10:00:00Z")));
        store.append(new MemoryEntry("tenant-a", "session-1", "u1001", MemoryEntryType.AGENT_MESSAGE, "需要先核对物流状态。", Map.of("traceId", "trace-1"), Instant.parse("2026-06-07T10:01:00Z")));
        store.saveBusinessSnapshot(new BusinessSnapshot(
                "tenant-a",
                "session-1",
                "T-1001",
                Map.of("ticketStatus", "OPEN", "orderStatus", "SHIPPED"),
                Instant.parse("2026-06-07T10:02:00Z")
        ));
        store.savePreference(new UserPreference(
                "tenant-a",
                "u1001",
                MemoryScope.LONG_TERM,
                "replyStyle",
                "先结论后依据",
                Instant.parse("2026-06-07T10:03:00Z")
        ));

        AgentContextAssembler assembler = new AgentContextAssembler(store);

        var context = assembler.assemble("tenant-a", "session-1", "T-1001", "u1001");

        assertThat(context.conversationWindow()).extracting(MemoryEntry::content)
                .containsExactly("这个退款工单怎么处理？", "需要先核对物流状态。");
        assertThat(context.businessSnapshot()).isPresent();
        assertThat(context.businessSnapshot().orElseThrow().fields())
                .containsEntry("ticketStatus", "OPEN")
                .containsEntry("orderStatus", "SHIPPED");
        assertThat(context.preferences()).extracting(UserPreference::key)
                .containsExactly("replyStyle");
        assertThat(context.sources()).containsExactly("conversation", "businessSnapshot", "preferences");
    }

    @Test
    void omitsBusinessSnapshotWhenTicketDoesNotMatchCurrentSession() {
        ConversationMemoryStore store = new ConversationMemoryStore(4);
        store.saveBusinessSnapshot(new BusinessSnapshot(
                "tenant-a",
                "session-2",
                "T-1001",
                Map.of("ticketStatus", "OPEN"),
                Instant.parse("2026-06-07T10:00:00Z")
        ));

        AgentContextAssembler assembler = new AgentContextAssembler(store);

        var context = assembler.assemble("tenant-a", "session-1", "T-1001", "u1001");

        assertThat(context.businessSnapshot()).isEmpty();
        assertThat(context.sources()).containsExactly("conversation");
    }
}
