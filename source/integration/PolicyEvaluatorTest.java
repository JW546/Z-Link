package integration;

import policy.PolicyEvaluator;

import java.util.List;
import java.util.Map;

public class PolicyEvaluatorTest {
    public static void main(String[] args) {
        PolicyEvaluator evaluator = new PolicyEvaluator();
        Map<String, Object> result = evaluator.evaluateDefaultPolicies();

        require(((Number) result.get("scenarioCount")).intValue() >= 10, "PolicyEvaluator runs all ScenarioLibrary scenarios");
        require(result.get("policyResults") instanceof List<?>, "PolicyEvaluator outputs policyResults");
        List<?> policies = (List<?>) result.get("policyResults");
        require(policies.size() == 3, "PolicyEvaluator compares three policies");

        boolean rule = false;
        boolean weighted = false;
        boolean adaptive = false;
        for (Object item : policies) {
            Map<?, ?> policy = (Map<?, ?>) item;
            String name = String.valueOf(policy.get("policyName"));
            rule = rule || "RuleBasedAgentPolicy".equals(name);
            weighted = weighted || "WeightedUtilityAgentPolicy".equals(name);
            adaptive = adaptive || "AdaptiveAgentPolicy".equals(name);
            require(policy.get("metrics") instanceof Map<?, ?>, name + " outputs metrics");
            Map<?, ?> metrics = (Map<?, ?>) policy.get("metrics");
            requireHas(metrics, "successRate", name);
            requireHas(metrics, "averageTravelTime", name);
            requireHas(metrics, "averageSafetyRisk", name);
            requireHas(metrics, "conflictResolutionAccuracy", name);
            requireHas(metrics, "replanningCount", name);
            requireHas(metrics, "explanationCompleteness", name);
            requireHas(metrics, "averageReward", name);
        }
        require(rule && weighted && adaptive, "PolicyEvaluator includes RuleBased, WeightedUtility, and Adaptive policies");
        require(result.get("bestPolicy") instanceof Map<?, ?>, "PolicyEvaluator outputs best policy");
        require(String.valueOf(result.get("whyBestPolicy")).contains("because"), "PolicyEvaluator explains why best policy is better");

        String json = evaluator.toFrontendJson(result);
        require(json.contains("policyResults") && json.contains("bestPolicy"), "PolicyEvaluator generates frontend JSON");
        require(String.valueOf(result.get("trainingStatus")).contains("no_rl_training"), "PolicyEvaluator does not claim RL training");

        System.out.println("[PASS] PolicyEvaluator test passed.");
    }

    private static void requireHas(Map<?, ?> metrics, String key, String policyName) {
        require(metrics.containsKey(key), policyName + " metrics include " + key);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("[FAIL] " + message);
        }
        System.out.println("[PASS] " + message);
    }
}
