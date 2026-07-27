package policy;

import java.util.LinkedHashMap;
import java.util.Map;

public class LearnedRoutePolicy implements AgentPolicy {
    private final Map<String, Map<String, Double>> qTable;
    private String lastStateKey = "";
    private String lastActionKey = "";

    public LearnedRoutePolicy(Map<String, Map<String, Double>> qTable) {
        this.qTable = qTable == null ? new LinkedHashMap<String, Map<String, Double>>() : qTable;
    }

    @Override
    public AgentAction decide(AgentState state) {
        lastStateKey = RouteTrainingStateEncoder.encode(state);
        lastActionKey = bestAction(lastStateKey);
        return RouteTrainingActionSpace.action(lastActionKey);
    }

    @Override
    public String explain(AgentState state, AgentAction action) {
        return "LearnedRoutePolicy selected " + lastActionKey
            + " for encoded state " + lastStateKey
            + " using tabular Q-learning values from simulation.";
    }

    @Override
    public Map<String, Object> evaluationResult(AgentState state, AgentAction action, Map<String, Object> outcome) {
        Map<String, Object> result = AgentPolicy.super.evaluationResult(state, action, outcome);
        result.put("policyName", "LearnedRoutePolicy");
        result.put("policyRole", "learned_simulation_policy");
        result.put("trainingStatus", "tabular_q_learning_simulation_policy_not_deep_rl");
        result.put("stateKey", lastStateKey);
        result.put("actionKey", lastActionKey);
        return result;
    }

    private String bestAction(String stateKey) {
        Map<String, Double> values = qTable.get(stateKey);
        if (values == null || values.isEmpty()) {
            return "request_replanning";
        }
        String best = "request_replanning";
        double bestValue = Double.NEGATIVE_INFINITY;
        for (Map.Entry<String, Double> entry : values.entrySet()) {
            double value = entry.getValue() == null ? 0.0 : entry.getValue();
            if (value > bestValue) {
                bestValue = value;
                best = entry.getKey();
            }
        }
        return best;
    }
}
