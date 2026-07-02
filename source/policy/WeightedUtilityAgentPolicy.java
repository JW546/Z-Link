package policy;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Multi-objective weighted utility policy over time, safety, congestion,
 * conflict, confidence, and freshness.
 */
public class WeightedUtilityAgentPolicy implements AgentPolicy {
    protected double timeWeight = 0.22;
    protected double safetyWeight = 0.34;
    protected double congestionWeight = 0.14;
    protected double conflictWeight = 0.12;
    protected double confidenceWeight = 0.10;
    protected double freshnessWeight = 0.08;
    protected double replanningPenaltyWeight = 0.08;
    protected String lastExplanation = "";
    protected Map<String, Object> lastEvaluation = new LinkedHashMap<String, Object>();

    public AgentAction decide(AgentState state) {
        double safetyCost = state.safetyRisk * safetyWeight;
        double congestionCost = state.congestionLevel * congestionWeight;
        double conflictCost = state.conflictStatus ? conflictWeight : 0.0;
        double evidenceReward = state.messageConfidence * confidenceWeight + state.messageFreshness * freshnessWeight;
        double timeReward = (state.passengerUrgent ? 1.0 : 0.45) * timeWeight + state.emergencyLevel * 0.05;
        double replanCost = state.replanRequired ? replanningPenaltyWeight : 0.0;
        double utility = clamp(0.50 + timeReward + evidenceReward - safetyCost - congestionCost - conflictCost - replanCost);

        AgentAction action;
        double riskThreshold = Math.max(0.58, 0.86 - safetyWeight);
        if (state.safetyRisk >= riskThreshold) {
            action = AgentAction.switchToSafetyPriority("Weighted utility selected safety priority because safety risk is high.");
        } else if (state.conflictStatus && state.messageConfidence < 0.55) {
            action = AgentAction.requestMoreInformation("Weighted utility needs more DDS evidence because conflict and confidence are weak.");
        } else if ((state.conflictStatus || state.replanRequired) && replanningPenaltyWeight < 0.22) {
            action = AgentAction.requestReplanning("Weighted utility requests replanning due to conflict or dynamic replan flag.");
        } else if (state.conflictStatus || state.replanRequired) {
            action = AgentAction.switchToSafetyPriority("Weighted utility avoids excessive replanning and switches to safety priority.");
        } else if (state.passengerUrgent && state.safetyRisk < 0.45) {
            action = AgentAction.switchToTimePriority("Weighted utility switches to time priority because urgency is high and risk is acceptable.");
        } else {
            action = AgentAction.chooseRoute(bestRoute(state), utility, "Weighted utility chooses route with best multi-objective score.");
        }

        lastExplanation = "WeightedUtilityAgentPolicy utility=" + round(utility)
            + " time=" + round(timeReward)
            + " safetyCost=" + round(safetyCost)
            + " congestionCost=" + round(congestionCost)
            + " conflictCost=" + round(conflictCost)
            + " replanCost=" + round(replanCost)
            + " evidenceReward=" + round(evidenceReward);
        lastEvaluation = new LinkedHashMap<String, Object>();
        lastEvaluation.put("utility", round(utility));
        lastEvaluation.put("timeReward", round(timeReward));
        lastEvaluation.put("safetyCost", round(safetyCost));
        lastEvaluation.put("congestionCost", round(congestionCost));
        lastEvaluation.put("conflictCost", round(conflictCost));
        lastEvaluation.put("replanCost", round(replanCost));
        lastEvaluation.put("evidenceReward", round(evidenceReward));
        return new AgentAction(action.type, action.route, action.suggestionSource,
            Math.max(action.priority, utility), lastExplanation, action.safetyPriority, action.timePriority);
    }

    public void configure(Map<String, Object> config) {
        if (config == null) return;
        timeWeight = number(config, "timeWeight", timeWeight);
        safetyWeight = number(config, "safetyWeight", safetyWeight);
        congestionWeight = number(config, "congestionWeight", congestionWeight);
        conflictWeight = number(config, "conflictWeight", conflictWeight);
        confidenceWeight = number(config, "confidenceWeight", confidenceWeight);
        freshnessWeight = number(config, "freshnessWeight", freshnessWeight);
        replanningPenaltyWeight = number(config, "replanningPenaltyWeight", replanningPenaltyWeight);
    }

    public String explain(AgentState state, AgentAction action) {
        return lastExplanation;
    }

    public Map<String, Object> evaluationResult(AgentState state, AgentAction action, Map<String, Object> outcome) {
        Map<String, Object> result = AgentPolicy.super.evaluationResult(state, action, outcome);
        result.put("policyName", "WeightedUtilityAgentPolicy");
        result.put("configuration", configuration());
        result.put("utilityBreakdown", lastEvaluation);
        return result;
    }

    public Map<String, Object> configuration() {
        Map<String, Object> config = new LinkedHashMap<String, Object>();
        config.put("timeWeight", timeWeight);
        config.put("safetyWeight", safetyWeight);
        config.put("congestionWeight", congestionWeight);
        config.put("conflictWeight", conflictWeight);
        config.put("confidenceWeight", confidenceWeight);
        config.put("freshnessWeight", freshnessWeight);
        config.put("replanningPenaltyWeight", replanningPenaltyWeight);
        return config;
    }

    protected String bestRoute(AgentState state) {
        if (state.candidateRoutes.isEmpty()) {
            return state.currentRoute == null || state.currentRoute.isBlank() ? "route_A_main_road" : state.currentRoute;
        }
        if (state.passengerSafetyFirst) {
            for (String route : state.candidateRoutes) {
                if (route.contains("school") || route.contains("elevated")) return route;
            }
        }
        if (state.passengerUrgent) {
            for (String route : state.candidateRoutes) {
                if (route.contains("green_wave") || route.contains("main_road")) return route;
            }
        }
        return state.candidateRoutes.get(0);
    }

    protected double number(Map<String, Object> map, String key, double fallback) {
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : fallback;
    }

    protected double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    protected double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
