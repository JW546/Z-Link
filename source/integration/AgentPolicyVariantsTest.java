package integration;

import evaluation.EvaluationService;
import policy.AdaptiveAgentPolicy;
import policy.AgentAction;
import policy.AgentPolicy;
import policy.AgentState;
import policy.RuleBasedAgentPolicy;
import policy.WeightedUtilityAgentPolicy;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class AgentPolicyVariantsTest {
    public static void main(String[] args) {
        testRuleBasedPolicyBaseline();
        testWeightedUtilityPolicy();
        testAdaptivePolicyUpdatesWeights();

        System.out.println("[PASS] Agent policy variants test passed.");
    }

    private static void testRuleBasedPolicyBaseline() {
        RuleBasedAgentPolicy policy = new RuleBasedAgentPolicy();
        policy.configure(config("highRiskThreshold", 0.65, "lowConfidenceThreshold", 0.40));
        AgentState state = state(0.72, 0.65, 0.90, 0.92, true);
        AgentAction action = policy.decide(state);
        require(action.type == AgentAction.Type.REQUEST_REPLANNING, "RuleBasedAgentPolicy selects baseline replanning action");
        require(policy.explain(state, action).contains("Rule baseline"), "RuleBasedAgentPolicy outputs explanation");
        Map<String, Object> eval = policy.evaluationResult(state, action, outcome());
        require("baseline_only".equals(eval.get("policyRole")), "RuleBasedAgentPolicy is marked baseline only");
        require(eval.containsKey("reward"), "RuleBasedAgentPolicy outputs reward calculation");
        require(policy.extractState(state.toMap()).passengerIntent.contains("医院"), "RuleBasedAgentPolicy supports state extraction");
    }

    private static void testWeightedUtilityPolicy() {
        WeightedUtilityAgentPolicy policy = new WeightedUtilityAgentPolicy();
        policy.configure(config("timeWeight", 0.20, "safetyWeight", 0.42, "congestionWeight", 0.12,
            "conflictWeight", 0.14, "confidenceWeight", 0.07, "freshnessWeight", 0.05));
        AgentState state = state(0.38, 0.70, 0.88, 0.94, true);
        AgentAction action = policy.decide(state);
        require(action.type == AgentAction.Type.REQUEST_REPLANNING
            || action.type == AgentAction.Type.CHOOSE_ROUTE
            || action.type == AgentAction.Type.SWITCH_TO_TIME_PRIORITY,
            "WeightedUtilityAgentPolicy selects an action");
        require(policy.explain(state, action).contains("utility"), "WeightedUtilityAgentPolicy outputs utility explanation");
        Map<String, Object> eval = policy.evaluationResult(state, action, outcome());
        require(eval.containsKey("utilityBreakdown"), "WeightedUtilityAgentPolicy outputs evaluation breakdown");
        require(eval.containsKey("configuration"), "WeightedUtilityAgentPolicy outputs configuration");
        require(((Number) eval.get("reward")).doubleValue() != 0.0, "WeightedUtilityAgentPolicy calculates reward");
    }

    private static void testAdaptivePolicyUpdatesWeights() {
        AdaptiveAgentPolicy policy = new AdaptiveAgentPolicy();
        Map<String, Object> before = policy.configuration();
        Map<String, Object> scenarioEval = new EvaluationService().evaluateScenario("hospital-emergency");
        policy.updateFromEvaluation(scenarioEval);
        Map<String, Object> after = policy.configuration();
        require(!String.valueOf(before).equals(String.valueOf(after)), "AdaptiveAgentPolicy updates configuration from evaluation");
        AgentState state = state(0.58, 0.82, 0.86, 0.88, true);
        AgentAction action = policy.decide(state);
        require(policy.explain(state, action).contains("updated weights"), "AdaptiveAgentPolicy explains adaptation");
        Map<String, Object> eval = policy.evaluationResult(state, action, outcome());
        require("adaptive_heuristic_weight_update_no_rl_training".equals(eval.get("trainingStatus")),
            "AdaptiveAgentPolicy does not claim RL training");
        require(eval.containsKey("adaptationTrace"), "AdaptiveAgentPolicy outputs evaluation result");
    }

    private static AgentState state(double risk, double congestion, double confidence, double freshness, boolean conflict) {
        return AgentState.builder("CAR-CQ-001")
            .passengerIntent("尽快去医院，优先安全")
            .currentRoute("route_A_main_road")
            .candidateRoutes(Arrays.asList("route_A_main_road", "route_C_elevated_detour", "route_D_emergency_green_wave"))
            .safetyRisk(risk)
            .congestionLevel(congestion)
            .messageConfidence(confidence)
            .messageFreshness(freshness)
            .sourceTrust(0.90)
            .conflictStatus(conflict)
            .emergencyLevel(0.80)
            .build();
    }

    private static Map<String, Object> outcome() {
        Map<String, Object> outcome = new LinkedHashMap<String, Object>();
        outcome.put("arrived", true);
        outcome.put("arrivalMinutes", 20.0);
        outcome.put("safetyRisk", 0.30);
        outcome.put("congestionLevel", 0.35);
        outcome.put("conflictHandled", true);
        outcome.put("explanationComplete", true);
        outcome.put("replanningCount", 1);
        return outcome;
    }

    private static Map<String, Object> config(Object... values) {
        Map<String, Object> config = new LinkedHashMap<String, Object>();
        for (int i = 0; i < values.length; i += 2) {
            config.put(String.valueOf(values[i]), values[i + 1]);
        }
        return config;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("[FAIL] " + message);
        }
        System.out.println("[PASS] " + message);
    }
}
