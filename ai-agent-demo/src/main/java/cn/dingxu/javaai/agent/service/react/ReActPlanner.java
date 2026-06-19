package cn.dingxu.javaai.agent.service.react;

import java.util.List;

public interface ReActPlanner {

    ReActAction next(String userInput, List<ReActStep> previousSteps);
}
