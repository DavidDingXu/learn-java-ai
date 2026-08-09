package com.xiaoding.javaai.labs.alibaba;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ConfirmationGraph {

    private static final String RISK = "risk";
    private static final String APPROVAL = "approval";
    private static final String STATUS = "status";

    private final CompiledGraph graph;

    public ConfirmationGraph() {
        try {
            this.graph = buildGraph().compile();
        } catch (GraphStateException exception) {
            throw new IllegalStateException("cannot compile confirmation graph", exception);
        }
    }

    public ConfirmationResult run(RiskLevel risk, ApprovalDecision approval) {
        List<NodeOutput> outputs = graph.stream(Map.of(RISK, risk.name(), APPROVAL, approval.name()))
                .collectList()
                .block();
        if (outputs == null || outputs.isEmpty()) {
            throw new IllegalStateException("confirmation graph produced no output");
        }
        NodeOutput last = outputs.get(outputs.size() - 1);
        String status = last.state().value(STATUS, String.class)
                .orElseThrow(() -> new IllegalStateException("confirmation graph produced no status"));
        List<String> visited = outputs.stream()
                .map(NodeOutput::node)
                .filter(node -> !StateGraph.START.equals(node) && !StateGraph.END.equals(node))
                .toList();
        return new ConfirmationResult(ConfirmationStatus.valueOf(status), visited);
    }

    private StateGraph buildGraph() throws GraphStateException {
        StateGraph stateGraph = new StateGraph(ConfirmationGraph::stateStrategies);
        stateGraph.addNode("classify", AsyncNodeAction.node_async(state -> Map.of()));
        stateGraph.addNode("confirm", AsyncNodeAction.node_async(state -> Map.of()));
        stateGraph.addNode("execute", AsyncNodeAction.node_async(state -> Map.of(STATUS, ConfirmationStatus.EXECUTED.name())));
        stateGraph.addNode("pending", AsyncNodeAction.node_async(state -> Map.of(STATUS, ConfirmationStatus.PENDING.name())));
        stateGraph.addNode("reject", AsyncNodeAction.node_async(state -> Map.of(STATUS, ConfirmationStatus.REJECTED.name())));

        stateGraph.addEdge(StateGraph.START, "classify");
        stateGraph.addConditionalEdges("classify",
                AsyncEdgeAction.edge_async(state -> state.<String>value(RISK).orElseThrow().equals(RiskLevel.LOW.name())
                        ? "execute" : "confirm"),
                Map.of("execute", "execute", "confirm", "confirm"));
        stateGraph.addConditionalEdges("confirm",
                AsyncEdgeAction.edge_async(state -> switch (ApprovalDecision.valueOf(state.<String>value(APPROVAL).orElseThrow())) {
                    case APPROVED -> "execute";
                    case REJECTED -> "reject";
                    case MISSING, NOT_REQUIRED -> "pending";
                }),
                Map.of("execute", "execute", "reject", "reject", "pending", "pending"));
        stateGraph.addEdge("execute", StateGraph.END);
        stateGraph.addEdge("pending", StateGraph.END);
        stateGraph.addEdge("reject", StateGraph.END);
        return stateGraph;
    }

    private static Map<String, KeyStrategy> stateStrategies() {
        Map<String, KeyStrategy> strategies = new LinkedHashMap<>();
        strategies.put(RISK, new ReplaceStrategy());
        strategies.put(APPROVAL, new ReplaceStrategy());
        strategies.put(STATUS, new ReplaceStrategy());
        return strategies;
    }
}
