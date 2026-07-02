package policy;

import java.util.LinkedHashMap;
import java.util.Map;

public interface AgentPolicy {
    AgentAction decide(AgentState state);

    default AgentState extractState(Map<String, Object> rawState) {
        if (rawState == null) {
            return AgentState.builder("AGENT-UNKNOWN").build();
        }
        return AgentState.builder(String.valueOf(rawState.getOrDefault("agentId", "AGENT-UNKNOWN")))
            .passengerIntent(String.valueOf(rawState.getOrDefault("passengerIntent", "")))
            .currentRoute(String.valueOf(rawState.getOrDefault("currentRoute", "")))
            .congestionLevel(number(rawState, "congestionLevel", 0.30))
            .safetyRisk(number(rawState, "safetyRisk", number(rawState, "localRisk", 0.20)))
            .sourceTrust(number(rawState, "sourceTrust", 0.70))
            .messageConfidence(number(rawState, "messageConfidence", 0.0))
            .messageFreshness(number(rawState, "messageFreshness", 1.0))
            .conflictStatus(bool(rawState, "conflictStatus", bool(rawState, "hasConflict", false)))
            .emergencyLevel(number(rawState, "emergencyLevel", 0.0))
            .replanRequired(bool(rawState, "replanRequired", false))
            .build();
    }

    default double calculateReward(AgentState state, AgentAction action, Map<String, Object> outcome) {
        return new DefaultRewardFunction().evaluate(state, action, outcome).totalReward;
    }

    default String explain(AgentState state, AgentAction action) {
        return action == null ? "" : action.reason;
    }

    default void configure(Map<String, Object> config) {
        // Optional for simple policies.
    }

    default Map<String, Object> evaluationResult(AgentState state, AgentAction action, Map<String, Object> outcome) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("action", action == null ? new LinkedHashMap<String, Object>() : action.toMap());
        result.put("reward", calculateReward(state, action, outcome));
        result.put("explanation", explain(state, action));
        result.put("trainingStatus", "heuristic_policy_evaluation_only_no_rl_training");
        return result;
    }

    static double number(Map<String, Object> map, String key, double fallback) {
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : fallback;
    }

    static boolean bool(Map<String, Object> map, String key, boolean fallback) {
        Object value = map.get(key);
        return value instanceof Boolean ? (Boolean) value : fallback;
    }
}
