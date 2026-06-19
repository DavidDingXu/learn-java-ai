package cn.dingxu.javaai.agent.service.react.hook;

import cn.dingxu.javaai.agent.service.react.ReActAction;
import cn.dingxu.javaai.agent.service.react.ReActStep;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PiiMaskingAgentHook implements AgentHook {

    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(1[3-9]\\d)(\\d{4})(\\d{4})(?!\\d)");

    @Override
    public String name() {
        return "pii-masking";
    }

    @Override
    public HookResult beforeAction(String userInput, List<ReActStep> previousSteps, ReActAction action) {
        Matcher matcher = PHONE.matcher(action.input());
        if (!matcher.find()) {
            return HookResult.accepted(action, List.of());
        }
        String masked = matcher.replaceAll("$1****$3");
        return HookResult.accepted(new ReActAction(action.type(), action.name(), masked), List.of(name() + ":MASK_PHONE"));
    }
}
