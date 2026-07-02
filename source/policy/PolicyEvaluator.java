package policy;

import agent.ScenarioLibrary;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import evaluation.EvaluationService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PolicyEvaluator {
    private final EvaluationService evaluationService = new EvaluationService();
    private final DefaultRewardFunction rewardFunction = new DefaultRewardFunction();

    public double evaluate(AgentPolicy policy, RewardFunction rewardFunction, List<AgentState> states) {
        if (policy == null || rewardFunction == null || states == null || states.isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        for (AgentState state : states) {
            AgentAction action = policy.decide(state);
            total += rewardFunction.reward(state, action, state.localRisk, 20.0);
        }
        return total / states.size();
    }

    public Map<String, Object> evaluateDefaultPolicies() {
        return evaluatePolicies(Arrays.asList(
            new RuleBasedAgentPolicy(),
            new WeightedUtilityAgentPolicy(),
            new AdaptiveAgentPolicy()
        ));
    }

    public Map<String, Object> evaluatePolicies(List<AgentPolicy> policies) {
        List<Map<String, Object>> policyResults = new ArrayList<Map<String, Object>>();
        for (AgentPolicy policy : policies) {
            policyResults.add(evaluatePolicy(policy));
        }
        Map<String, Object> best = bestPolicy(policyResults);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("scenarioCount", ScenarioLibrary.getInstance().list().size());
        result.put("policyResults", policyResults);
        result.put("bestPolicy", best);
        result.put("whyBestPolicy", explainBest(best, policyResults));
        result.put("frontendJsonSchema", "policy-comparison/v1");
        result.put("trainingStatus", "heuristic_policy_evaluation_only_no_rl_training");
        return result;
    }

    public Map<String, Object> evaluatePolicy(AgentPolicy policy) {
        List<Map<String, Object>> scenarioResults = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> scenario : ScenarioLibrary.getInstance().list()) {
            Map<String, Object> scenarioEvaluation = evaluationService.evaluateScenario(String.valueOf(scenario.get("scenarioId")));
            if (policy instanceof AdaptiveAgentPolicy) {
                ((AdaptiveAgentPolicy) policy).updateFromEvaluation(scenarioEvaluation);
            }
            scenarioResults.add(evaluateScenarioForPolicy(policy, scenario));
        }
        Map<String, Object> aggregate = aggregate(scenarioResults);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("policyName", policyName(policy));
        result.put("policyRole", policy instanceof RuleBasedAgentPolicy ? "baseline_only" : "candidate_policy");
        result.put("scenarioResults", scenarioResults);
        result.put("metrics", aggregate);
        result.put("configuration", configuration(policy));
        result.put("trainingStatus", policy instanceof AdaptiveAgentPolicy
            ? "adaptive_heuristic_weight_update_no_rl_training"
            : "heuristic_policy_evaluation_only_no_rl_training");
        return result;
    }

    public String toFrontendJson(Map<String, Object> evaluationResult) {
        return new GsonBuilder().setPrettyPrinting().create().toJson(evaluationResult);
    }

    private Map<String, Object> evaluateScenarioForPolicy(AgentPolicy policy, Map<String, Object> scenario) {
        AgentState state = stateFromScenario(scenario);
        AgentAction action = policy.decide(state);
        Map<String, Object> outcome = outcomeFor(scenario, state, action);
        DefaultRewardFunction.RewardBreakdown reward = rewardFunction.evaluate(state, action, outcome);

        Map<String, Object> metrics = new LinkedHashMap<String, Object>();
        metrics.put("successRate", number(outcome, "arrived", true) ? 1.0 : 0.0);
        metrics.put("averageTravelTime", round(number(outcome, "arrivalMinutes", 24.0)));
        metrics.put("averageSafetyRisk", round(number(outcome, "safetyRisk", state.safetyRisk)));
        metrics.put("conflictResolutionAccuracy", number(outcome, "conflictHandled", true) ? 1.0 : 0.0);
        metrics.put("replanningCount", (int) number(outcome, "replanningCount", 0.0));
        metrics.put("explanationCompleteness", number(outcome, "explanationComplete", true) ? 1.0 : 0.0);
        metrics.put("averageReward", reward.totalReward);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("scenarioId", scenario.get("scenarioId"));
        result.put("policyName", policyName(policy));
        result.put("state", state.toMap());
        result.put("action", action.toMap());
        result.put("outcome", outcome);
        result.put("metrics", metrics);
        result.put("rewardBreakdown", reward.toMap());
        result.put("explanation", policy.explain(state, action));
        result.put("evaluationResult", policy.evaluationResult(state, action, outcome));
        return result;
    }

    @SuppressWarnings("unchecked")
    private AgentState stateFromScenario(Map<String, Object> scenario) {
        Map<String, Object> world = castMap(scenario.get("initialWorldState"));
        List<Map<String, Object>> routes = (List<Map<String, Object>>) scenario.getOrDefault("candidateRoutes",
            java.util.Collections.<Map<String, Object>>emptyList());
        List<Map<String, Object>> messages = (List<Map<String, Object>>) scenario.getOrDefault("ddsMessages",
            java.util.Collections.<Map<String, Object>>emptyList());
        List<String> conflicts = (List<String>) scenario.getOrDefault("expectedConflicts",
            java.util.Collections.<String>emptyList());

        AgentState.Builder builder = AgentState.builder("POLICY-EVAL-AGENT")
            .passengerIntent(String.valueOf(scenario.getOrDefault("passengerIntent", "")))
            .currentRoute(routes.isEmpty() ? "route_A_main_road" : String.valueOf(routes.get(0).get("routeId")))
            .congestionLevel(number(world, "congestionLevel", 0.30))
            .safetyRisk(number(world, "roadRisk", 0.30))
            .sourceTrust(average(messages, "sourceTrust", 0.78))
            .messageConfidence(average(messages, "confidence", 0.80))
            .messageFreshness(Math.max(0.15, 1.0 - average(messages, "ddsDelayMs", 0.0) / 2_000.0))
            .conflictStatus(!conflicts.isEmpty())
            .emergencyLevel(String.valueOf(scenario.getOrDefault("passengerIntent", "")).contains("尽快") ? 0.80 : 0.30)
            .replanRequired(conflicts.contains("SAFETY_RISK_CONFLICT"));

        for (Map<String, Object> route : routes) {
            builder.addCandidateRoute(String.valueOf(route.get("routeId")));
        }
        for (Map<String, Object> message : messages) {
            builder.addDdsMessage(message);
            if (String.valueOf(message.getOrDefault("topic", "")).contains("drone")) builder.droneSuggestion(message);
            if (String.valueOf(message.getOrDefault("topic", "")).contains("roadside")) builder.roadsideSuggestion(message);
            if (String.valueOf(message.getOrDefault("topic", "")).contains("neighbor")) builder.neighborVehicleSuggestion(message);
        }
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> outcomeFor(Map<String, Object> scenario, AgentState state, AgentAction action) {
        Map<String, Object> targets = castMap(scenario.get("evaluationTargets"));
        List<String> conflicts = (List<String>) scenario.getOrDefault("expectedConflicts",
            java.util.Collections.<String>emptyList());
        boolean conflictHandled = !state.conflictStatus
            || action.type == AgentAction.Type.REQUEST_REPLANNING
            || action.type == AgentAction.Type.REJECT_ROUTE
            || action.type == AgentAction.Type.SWITCH_TO_SAFETY_PRIORITY
            || action.safetyPriority;
        double arrival = number(targets, "arrivalTimeMinutes", 24.0);
        if (action.type == AgentAction.Type.SWITCH_TO_TIME_PRIORITY) arrival -= 2.0;
        if (action.type == AgentAction.Type.REQUEST_REPLANNING) arrival += 1.5;
        if (action.type == AgentAction.Type.WAIT_FOR_MORE_DDS_MESSAGES) arrival += 3.0;

        double risk = number(targets, "riskExposure", state.safetyRisk);
        if (action.type == AgentAction.Type.SWITCH_TO_TIME_PRIORITY) risk += 0.10;
        if (action.type == AgentAction.Type.SWITCH_TO_SAFETY_PRIORITY || action.type == AgentAction.Type.REQUEST_REPLANNING) {
            risk = Math.max(0.05, risk - 0.08);
        }
        boolean trustedLowConfidence = state.messageConfidence < 0.45
            && (action.type == AgentAction.Type.CHOOSE_ROUTE || action.type == AgentAction.Type.ACCEPT_SUGGESTION);
        boolean ignoredHighTrustRisk = state.sourceTrust >= 0.85 && state.safetyRisk >= 0.65 && !conflictHandled;

        Map<String, Object> outcome = new LinkedHashMap<String, Object>();
        outcome.put("arrived", action.type != AgentAction.Type.WAIT_FOR_MORE_DDS_MESSAGES || arrival <= 28.0);
        outcome.put("arrivalMinutes", round(Math.max(1.0, arrival)));
        outcome.put("safetyRisk", round(clamp(risk)));
        outcome.put("congestionLevel", state.congestionLevel);
        outcome.put("conflictHandled", conflictHandled);
        outcome.put("explanationComplete", action.reason != null && action.reason.length() >= 12);
        outcome.put("replanningCount", action.type == AgentAction.Type.REQUEST_REPLANNING ? Math.max(1, (int) number(targets, "replanCount", 1.0)) : 0);
        outcome.put("trustedLowConfidenceMessage", trustedLowConfidence);
        outcome.put("ignoredHighTrustRiskMessage", ignoredHighTrustRisk);
        outcome.put("expectedConflicts", conflicts);
        return outcome;
    }

    private Map<String, Object> aggregate(List<Map<String, Object>> scenarioResults) {
        Map<String, Object> metrics = new LinkedHashMap<String, Object>();
        if (scenarioResults.isEmpty()) return metrics;
        double success = 0.0;
        double time = 0.0;
        double risk = 0.0;
        double conflict = 0.0;
        double explanation = 0.0;
        double reward = 0.0;
        int replans = 0;
        for (Map<String, Object> item : scenarioResults) {
            Map<String, Object> m = castMap(item.get("metrics"));
            success += number(m, "successRate", 0.0);
            time += number(m, "averageTravelTime", 0.0);
            risk += number(m, "averageSafetyRisk", 0.0);
            conflict += number(m, "conflictResolutionAccuracy", 0.0);
            explanation += number(m, "explanationCompleteness", 0.0);
            reward += number(m, "averageReward", 0.0);
            replans += (int) number(m, "replanningCount", 0.0);
        }
        int n = scenarioResults.size();
        metrics.put("successRate", round(success / n));
        metrics.put("averageTravelTime", round(time / n));
        metrics.put("averageSafetyRisk", round(risk / n));
        metrics.put("conflictResolutionAccuracy", round(conflict / n));
        metrics.put("replanningCount", replans);
        metrics.put("explanationCompleteness", round(explanation / n));
        metrics.put("averageReward", round(reward / n));
        return metrics;
    }

    private Map<String, Object> bestPolicy(List<Map<String, Object>> policyResults) {
        Map<String, Object> best = new LinkedHashMap<String, Object>();
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Map<String, Object> policyResult : policyResults) {
            Map<String, Object> metrics = castMap(policyResult.get("metrics"));
            double score = number(metrics, "averageReward", 0.0)
                + number(metrics, "conflictResolutionAccuracy", 0.0) * 0.25
                - number(metrics, "averageSafetyRisk", 1.0) * 0.25;
            if (score > bestScore) {
                best = policyResult;
                bestScore = score;
            }
        }
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("policyName", best.get("policyName"));
        summary.put("metrics", best.get("metrics"));
        summary.put("selectionScore", round(bestScore));
        return summary;
    }

    private String explainBest(Map<String, Object> best, List<Map<String, Object>> policyResults) {
        Map<String, Object> metrics = castMap(best.get("metrics"));
        return best.get("policyName") + " is selected because it has the strongest combined reward, conflict resolution accuracy, and safety-risk profile: averageReward="
            + metrics.get("averageReward") + ", conflictResolutionAccuracy=" + metrics.get("conflictResolutionAccuracy")
            + ", averageSafetyRisk=" + metrics.get("averageSafetyRisk")
            + ". RuleBasedAgentPolicy remains a baseline, and AdaptiveAgentPolicy uses heuristic weight updates only.";
    }

    private String policyName(AgentPolicy policy) {
        return policy.getClass().getSimpleName();
    }

    private Map<String, Object> configuration(AgentPolicy policy) {
        if (policy instanceof RuleBasedAgentPolicy) return ((RuleBasedAgentPolicy) policy).configuration();
        if (policy instanceof WeightedUtilityAgentPolicy) return ((WeightedUtilityAgentPolicy) policy).configuration();
        return new LinkedHashMap<String, Object>();
    }

    private double average(List<Map<String, Object>> items, String key, double fallback) {
        if (items == null || items.isEmpty()) return fallback;
        double total = 0.0;
        for (Map<String, Object> item : items) total += number(item, key, fallback);
        return total / items.size();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : new LinkedHashMap<String, Object>();
    }

    private double number(Map<String, Object> map, String key, double fallback) {
        Object value = map == null ? null : map.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : fallback;
    }

    private boolean number(Map<String, Object> map, String key, boolean fallback) {
        Object value = map == null ? null : map.get(key);
        return value instanceof Boolean ? (Boolean) value : fallback;
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
