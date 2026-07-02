package policy;

import java.util.Map;

public class HeuristicPolicy extends WeightedUtilityAgentPolicy {
    public HeuristicPolicy() {
        configure(defaultConfig());
    }

    public Map<String, Object> evaluationResult(AgentState state, AgentAction action, Map<String, Object> outcome) {
        Map<String, Object> result = super.evaluationResult(state, action, outcome);
        result.put("policyName", "HeuristicPolicy");
        result.put("policyRole", "baseline_only");
        result.put("trainingStatus", "heuristic_baseline_no_rl_training");
        return result;
    }

    private Map<String, Object> defaultConfig() {
        Map<String, Object> config = new java.util.LinkedHashMap<String, Object>();
        config.put("timeWeight", 0.18);
        config.put("safetyWeight", 0.42);
        config.put("congestionWeight", 0.14);
        config.put("conflictWeight", 0.14);
        config.put("confidenceWeight", 0.07);
        config.put("freshnessWeight", 0.05);
        config.put("replanningPenaltyWeight", 0.08);
        return config;
    }
}
