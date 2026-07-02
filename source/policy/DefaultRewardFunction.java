package policy;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Heuristic reward function for policy evaluation. This is not RL/MARL training.
 */
public class DefaultRewardFunction implements RewardFunction {
    public double reward(AgentState state, AgentAction action, double outcomeRisk, double arrivalMinutes) {
        return evaluate(state, action, true, arrivalMinutes, outcomeRisk,
            state.congestionLevel, conflictHandled(state, action), explanationComplete(action),
            1, false, false).totalReward;
    }

    public RewardBreakdown evaluate(AgentState state, AgentAction action, boolean arrived,
                                    double arrivalMinutes, double safetyRisk,
                                    double congestionLevel, boolean conflictHandled,
                                    boolean explanationComplete, int replanningCount,
                                    boolean trustedLowConfidenceMessage,
                                    boolean ignoredHighTrustRiskMessage) {
        double reward = 0.0;
        reward += arrived ? 1.0 : -1.0;
        reward += Math.max(-1.0, 0.45 - Math.max(0.0, arrivalMinutes - 18.0) / 40.0);
        reward -= Math.min(2.8, safetyRisk * 3.2);
        reward -= congestionLevel * 0.45;
        reward += conflictHandled ? 0.75 : (state.conflictStatus ? -0.85 : 0.10);
        reward -= trustedLowConfidenceMessage ? 0.70 : 0.0;
        reward -= ignoredHighTrustRiskMessage ? 1.10 : 0.0;
        reward -= Math.max(0, replanningCount - 1) * 0.12;
        reward += explanationComplete ? 0.35 : -0.25;

        if (safetyRisk >= 0.80 && !action.safetyPriority && action.type != AgentAction.Type.REJECT_ROUTE
            && action.type != AgentAction.Type.REQUEST_REPLANNING
            && action.type != AgentAction.Type.SWITCH_TO_SAFETY_PRIORITY) {
            reward -= 2.5;
        }

        RewardBreakdown breakdown = new RewardBreakdown();
        breakdown.totalReward = round(reward);
        breakdown.arrived = arrived;
        breakdown.arrivalMinutes = arrivalMinutes;
        breakdown.safetyRisk = safetyRisk;
        breakdown.congestionLevel = congestionLevel;
        breakdown.conflictHandled = conflictHandled;
        breakdown.trustedLowConfidenceMessage = trustedLowConfidenceMessage;
        breakdown.ignoredHighTrustRiskMessage = ignoredHighTrustRiskMessage;
        breakdown.replanningCount = replanningCount;
        breakdown.explanationComplete = explanationComplete;
        breakdown.hardSafetyViolation = safetyRisk >= 0.80 && breakdown.totalReward < 0.0;
        return breakdown;
    }

    public RewardBreakdown evaluate(AgentState state, AgentAction action, Map<String, Object> outcome) {
        Map<String, Object> safeOutcome = outcome == null ? new LinkedHashMap<String, Object>() : outcome;
        return evaluate(state, action,
            bool(safeOutcome, "arrived", true),
            number(safeOutcome, "arrivalMinutes", 20.0),
            number(safeOutcome, "safetyRisk", state.safetyRisk),
            number(safeOutcome, "congestionLevel", state.congestionLevel),
            bool(safeOutcome, "conflictHandled", conflictHandled(state, action)),
            bool(safeOutcome, "explanationComplete", explanationComplete(action)),
            (int) number(safeOutcome, "replanningCount", state.replanRequired ? 1.0 : 0.0),
            bool(safeOutcome, "trustedLowConfidenceMessage", trustedLowConfidence(state, action)),
            bool(safeOutcome, "ignoredHighTrustRiskMessage", ignoredHighTrustRisk(state, action)));
    }

    private boolean conflictHandled(AgentState state, AgentAction action) {
        if (!state.conflictStatus) return true;
        return action.type == AgentAction.Type.REJECT_ROUTE
            || action.type == AgentAction.Type.REJECT_SUGGESTION
            || action.type == AgentAction.Type.REQUEST_REPLANNING
            || action.type == AgentAction.Type.SWITCH_TO_SAFETY_PRIORITY
            || action.safetyPriority;
    }

    private boolean trustedLowConfidence(AgentState state, AgentAction action) {
        return state.messageConfidence < 0.45
            && (action.type == AgentAction.Type.ACCEPT_SUGGESTION || action.type == AgentAction.Type.CHOOSE_ROUTE);
    }

    private boolean ignoredHighTrustRisk(AgentState state, AgentAction action) {
        return state.sourceTrust >= 0.85 && state.safetyRisk >= 0.65
            && action.type != AgentAction.Type.REQUEST_REPLANNING
            && action.type != AgentAction.Type.REJECT_ROUTE
            && action.type != AgentAction.Type.SWITCH_TO_SAFETY_PRIORITY
            && !action.safetyPriority;
    }

    private boolean explanationComplete(AgentAction action) {
        return action.reason != null && action.reason.length() >= 12;
    }

    private double number(Map<String, Object> map, String key, double fallback) {
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : fallback;
    }

    private boolean bool(Map<String, Object> map, String key, boolean fallback) {
        Object value = map.get(key);
        return value instanceof Boolean ? (Boolean) value : fallback;
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    public static class RewardBreakdown {
        public double totalReward;
        public boolean arrived;
        public double arrivalMinutes;
        public double safetyRisk;
        public double congestionLevel;
        public boolean conflictHandled;
        public boolean trustedLowConfidenceMessage;
        public boolean ignoredHighTrustRiskMessage;
        public int replanningCount;
        public boolean explanationComplete;
        public boolean hardSafetyViolation;

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            map.put("totalReward", totalReward);
            map.put("arrived", arrived);
            map.put("arrivalMinutes", arrivalMinutes);
            map.put("safetyRisk", safetyRisk);
            map.put("congestionLevel", congestionLevel);
            map.put("conflictHandled", conflictHandled);
            map.put("trustedLowConfidenceMessage", trustedLowConfidenceMessage);
            map.put("ignoredHighTrustRiskMessage", ignoredHighTrustRiskMessage);
            map.put("replanningCount", replanningCount);
            map.put("explanationComplete", explanationComplete);
            map.put("hardSafetyViolation", hardSafetyViolation);
            map.put("trainingStatus", "heuristic_reward_only_no_rl_training");
            return map;
        }
    }
}
