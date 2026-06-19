package cn.dingxu.javaai.agent.service.react;

import cn.dingxu.javaai.agent.service.react.hook.AgentHookChain;
import cn.dingxu.javaai.agent.service.react.hook.HookResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GuardedReActAgent {

    private final ReActPlanner planner;
    private final ReActPolicy policy;
    private final AgentHookChain hookChain;

    public GuardedReActAgent(ReActPlanner planner, ReActPolicy policy) {
        this(planner, policy, new AgentHookChain(List.of()));
    }

    public GuardedReActAgent(ReActPlanner planner, ReActPolicy policy, AgentHookChain hookChain) {
        if (planner == null) {
            throw new IllegalArgumentException("planner must not be null");
        }
        if (policy == null) {
            throw new IllegalArgumentException("policy must not be null");
        }
        this.planner = planner;
        this.policy = policy;
        this.hookChain = hookChain == null ? new AgentHookChain(List.of()) : hookChain;
    }

    public ReActRunResult run(String userInput) {
        List<ReActStep> steps = new ArrayList<>();
        for (int i = 1; i <= policy.maxSteps(); i++) {
            ReActAction action = planner.next(userInput, steps);
            HookResult hookResult = hookChain.beforeAction(userInput, steps, action);
            ReActAction guardedAction = hookResult.action();
            if (hookResult.rejected()) {
                steps.add(new ReActStep(i, guardedAction.name(), guardedAction.input(), "Hook 拒绝：" + hookResult.reason(), hookResult.events()));
                return ReActRunResult.stopped("hook_rejected:" + hookResult.reason(), steps);
            }
            if (guardedAction.type() == ReActActionType.FINISH) {
                steps.add(new ReActStep(i, guardedAction.name(), guardedAction.input(), "模型结束执行", hookResult.events()));
                return ReActRunResult.completed(guardedAction.input(), steps);
            }
            if (!policy.allows(guardedAction.name())) {
                steps.add(new ReActStep(i, guardedAction.name(), guardedAction.input(), "拒绝调用未授权工具：" + guardedAction.name(), hookResult.events()));
                return ReActRunResult.stopped("tool_not_allowed:" + guardedAction.name(), steps);
            }
            steps.add(new ReActStep(i, guardedAction.name(), guardedAction.input(), executeReadOnlyTool(guardedAction), hookResult.events()));
        }
        return ReActRunResult.stopped("max_steps_exceeded", steps);
    }

    private String executeReadOnlyTool(ReActAction action) {
        Map<String, String> observations = Map.of(
                "lookupTicket", "已读取工单：状态 OPEN，问题为退款咨询。",
                "lookupOrder", "已读取订单：状态 SHIPPED，金额 5000。",
                "retrievePolicy", "已读取制度：发货后退款需核对物流，高金额需人工复核。"
        );
        return observations.getOrDefault(action.name(), "已执行只读工具：" + action.name());
    }
}
