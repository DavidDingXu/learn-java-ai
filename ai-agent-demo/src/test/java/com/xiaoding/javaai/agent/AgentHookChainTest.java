package com.xiaoding.javaai.agent;

import com.xiaoding.javaai.agent.service.react.GuardedReActAgent;
import com.xiaoding.javaai.agent.service.react.ReActAction;
import com.xiaoding.javaai.agent.service.react.ReActPolicy;
import com.xiaoding.javaai.agent.service.react.ReActRunResult;
import com.xiaoding.javaai.agent.service.react.ScriptedReActPlanner;
import com.xiaoding.javaai.agent.service.react.hook.AgentHookChain;
import com.xiaoding.javaai.agent.service.react.hook.PiiMaskingAgentHook;
import com.xiaoding.javaai.agent.service.react.hook.ToolAllowlistAgentHook;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AgentHookChainTest {

    @Test
    void masksSensitiveActionInputBeforeToolExecution() {
        GuardedReActAgent agent = new GuardedReActAgent(new ScriptedReActPlanner(List.of(
                ReActAction.callTool("lookupTicket", "查询客户手机号 13812345678 的工单"),
                ReActAction.finish("已查询工单。")
        )), ReActPolicy.readOnly(Set.of("lookupTicket"), 5), new AgentHookChain(List.of(
                new PiiMaskingAgentHook()
        )));

        ReActRunResult result = agent.run("客户手机号 13812345678，帮我查工单");

        assertThat(result.completed()).isTrue();
        assertThat(result.steps().getFirst().actionInput()).contains("138****5678");
        assertThat(result.steps().getFirst().actionInput()).doesNotContain("13812345678");
        assertThat(result.steps().getFirst().hookEvents()).containsExactly("pii-masking:MASK_PHONE");
    }

    @Test
    void hookCanRejectToolBeforeDefaultPolicyAndLeaveTraceEvent() {
        GuardedReActAgent agent = new GuardedReActAgent(new ScriptedReActPlanner(List.of(
                ReActAction.callTool("exportTicket", "导出工单明细")
        )), ReActPolicy.readOnly(Set.of("lookupTicket", "exportTicket"), 5), new AgentHookChain(List.of(
                new ToolAllowlistAgentHook(Set.of("lookupTicket"))
        )));

        ReActRunResult result = agent.run("帮我导出工单");

        assertThat(result.completed()).isFalse();
        assertThat(result.stopReason()).isEqualTo("hook_rejected:tool-allowlist");
        assertThat(result.steps()).singleElement().satisfies(step -> {
            assertThat(step.actionName()).isEqualTo("exportTicket");
            assertThat(step.observation()).contains("Hook 拒绝");
            assertThat(step.hookEvents()).containsExactly("tool-allowlist:REJECT_TOOL");
        });
    }
}
