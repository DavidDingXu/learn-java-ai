package cn.dingxu.javaai.agent;

import cn.dingxu.javaai.agent.service.react.GuardedReActAgent;
import cn.dingxu.javaai.agent.service.react.ReActAction;
import cn.dingxu.javaai.agent.service.react.ReActPolicy;
import cn.dingxu.javaai.agent.service.react.ReActRunResult;
import cn.dingxu.javaai.agent.service.react.ScriptedReActPlanner;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GuardedReActAgentTest {

    @Test
    void runsOnlyAllowedReadToolsAndReturnsStructuredTrace() {
        GuardedReActAgent agent = new GuardedReActAgent(new ScriptedReActPlanner(List.of(
                ReActAction.callTool("lookupTicket", "查询工单 T-1001"),
                ReActAction.callTool("retrievePolicy", "检索退款制度"),
                ReActAction.finish("建议先核对物流状态，并转人工确认。")
        )), ReActPolicy.readOnly(Set.of("lookupTicket", "retrievePolicy"), 5));

        ReActRunResult result = agent.run("客户申请退款，但订单已发货");

        assertThat(result.completed()).isTrue();
        assertThat(result.finalAnswer()).contains("转人工确认");
        assertThat(result.steps()).extracting("actionName")
                .containsExactly("lookupTicket", "retrievePolicy", "finish");
        assertThat(result.steps()).allSatisfy(step -> assertThat(step.observation()).isNotBlank());
    }

    @Test
    void rejectsWriteToolEvenIfPlannerSelectsIt() {
        GuardedReActAgent agent = new GuardedReActAgent(new ScriptedReActPlanner(List.of(
                ReActAction.callTool("lookupTicket", "查询工单 T-9001"),
                ReActAction.callTool("closeTicket", "关闭投诉工单")
        )), ReActPolicy.readOnly(Set.of("lookupTicket"), 5));

        ReActRunResult result = agent.run("客户要求关闭投诉工单");

        assertThat(result.completed()).isFalse();
        assertThat(result.stopReason()).isEqualTo("tool_not_allowed:closeTicket");
        assertThat(result.steps()).extracting("actionName")
                .containsExactly("lookupTicket", "closeTicket");
        assertThat(result.steps().getLast().observation()).contains("拒绝");
    }

    @Test
    void stopsWhenPlannerDoesNotFinishWithinMaxSteps() {
        GuardedReActAgent agent = new GuardedReActAgent(new ScriptedReActPlanner(List.of(
                ReActAction.callTool("lookupTicket", "查询工单"),
                ReActAction.callTool("retrievePolicy", "检索制度"),
                ReActAction.callTool("lookupTicket", "再次查询工单")
        )), ReActPolicy.readOnly(Set.of("lookupTicket", "retrievePolicy"), 2));

        ReActRunResult result = agent.run("客户反复追问退款进度");

        assertThat(result.completed()).isFalse();
        assertThat(result.stopReason()).isEqualTo("max_steps_exceeded");
        assertThat(result.steps()).hasSize(2);
    }
}
