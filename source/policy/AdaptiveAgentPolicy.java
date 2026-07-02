package policy;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adaptive heuristic policy that updates utility weights from evaluation output.
 * It does not perform RL/MARL training.
 */
public class AdaptiveAgentPolicy extends WeightedUtilityAgentPolicy {
    private double learningRate = 0.10;
    private String adaptationTrace = "not adapted";

    public void updateFromEvaluation(Map<String, Object> evaluationResult) {
        if (evaluationResult == null) return;
        Map<String, Object> metrics = castMetrics(evaluationResult);
        double safetyRisk = number(metrics, "averageSafetyRisk", number(metrics, "riskExposure", 0.30));
        double conflictAccuracy = number(metrics, "conflictResolutionAccuracy", 0.80);
        double travelTime = number(metrics, "averageTravelTime", number(metrics, "arrivalTimeMinutes", 20.0));
        double congestionAvoidance = number(metrics, "congestionAvoidanceRate", 0.80);
        double delayImpact = number(metrics, "messageDelayImpact", 0.0);
        double successRate = number(metrics, "successRate", 0.0);
        double reward = number(metrics, "averageReward", 0.0);

        if (safetyRisk > 0.40) safetyWeight += learningRate * safetyRisk;
        if (conflictAccuracy < 0.80) conflictWeight += learningRate * (1.0 - conflictAccuracy);
        if (travelTime > 24.0) timeWeight += learningRate * Math.min(0.5, (travelTime - 24.0) / 40.0);
        if (congestionAvoidance < 0.75) congestionWeight += learningRate * (0.75 - congestionAvoidance);
        if (delayImpact > 0.30) freshnessWeight += learningRate * delayImpact;
        if (successRate >= 0.90 && reward >= 0.50) {
            confidenceWeight += learningRate * 0.03;
            freshnessWeight += learningRate * 0.02;
        }
        normalizeWeights();
        adaptationTrace = "updated weights from evaluation: risk=" + round(safetyRisk)
            + ", conflictAccuracy=" + round(conflictAccuracy)
            + ", travelTime=" + round(travelTime)
            + ", congestionAvoidance=" + round(congestionAvoidance)
            + ", delayImpact=" + round(delayImpact);
    }

    public void configure(Map<String, Object> config) {
        super.configure(config);
        if (config != null) learningRate = number(config, "learningRate", learningRate);
    }

    public String explain(AgentState state, AgentAction action) {
        return "AdaptiveAgentPolicy " + adaptationTrace + "; " + lastExplanation;
    }

    public Map<String, Object> evaluationResult(AgentState state, AgentAction action, Map<String, Object> outcome) {
        Map<String, Object> result = super.evaluationResult(state, action, outcome);
        result.put("policyName", "AdaptiveAgentPolicy");
        result.put("adaptationTrace", adaptationTrace);
        result.put("trainingStatus", "adaptive_heuristic_weight_update_no_rl_training");
        return result;
    }

    public Map<String, Object> configuration() {
        Map<String, Object> config = super.configuration();
        config.put("learningRate", learningRate);
        config.put("adaptive", true);
        return config;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMetrics(Map<String, Object> evaluationResult) {
        Object metrics = evaluationResult.get("metrics");
        if (metrics instanceof Map<?, ?>) return (Map<String, Object>) metrics;
        Object aggregate = evaluationResult.get("aggregateMetrics");
        if (aggregate instanceof Map<?, ?>) return (Map<String, Object>) aggregate;
        return evaluationResult;
    }

    private void normalizeWeights() {
        double sum = timeWeight + safetyWeight + congestionWeight + conflictWeight + confidenceWeight + freshnessWeight;
        if (sum <= 0.0) return;
        timeWeight /= sum;
        safetyWeight /= sum;
        congestionWeight /= sum;
        conflictWeight /= sum;
        confidenceWeight /= sum;
        freshnessWeight /= sum;
    }
}
