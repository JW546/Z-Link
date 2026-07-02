package integration;

import policy.PolicyOptimizationService;

import java.util.List;
import java.util.Map;

public class PolicyOptimizationServiceTest {
    public static void main(String[] args) {
        PolicyOptimizationService service = new PolicyOptimizationService();

        Map<String, Object> grid = service.gridSearch();
        require(grid.get("bestConfig") instanceof Map<?, ?>, "grid search outputs best config");
        require(grid.get("optimizationLog") instanceof List<?>, "grid search outputs optimization log");

        Map<String, Object> random = service.randomSearch(16, 20260630L);
        require(random.get("bestConfig") instanceof Map<?, ?>, "random search outputs best config");
        require(random.get("optimizationLog") instanceof List<?>, "random search outputs optimization log");

        Map<String, Object> optimized = service.optimize();
        Map<String, Object> beforeMetrics = metrics(optimized.get("before"));
        Map<String, Object> afterMetrics = metrics(optimized.get("after"));
        Map<String, Object> bestConfig = castMap(optimized.get("bestPolicyConfiguration"));

        require(number(afterMetrics, "averageReward") > number(beforeMetrics, "averageReward"),
            "optimized averageReward improves baseline");
        require(number(afterMetrics, "averageSafetyRisk") <= number(beforeMetrics, "averageSafetyRisk") + 0.0001,
            "optimized averageSafetyRisk does not increase");
        require(number(bestConfig, "safetyWeight") >= 0.34,
            "safety-first scenarios keep safetyWeight above threshold");
        require(number(bestConfig, "confidenceWeight") > 0.0,
            "confidenceWeight is active for low-confidence message scenarios");
        require(optimized.get("optimizationLog") instanceof List<?>, "optimization outputs log");
        require(optimized.get("improvement") instanceof Map<?, ?>, "optimization outputs before/after comparison");
        require(String.valueOf(optimized.get("trainingStatus")).contains("no_rl_training"),
            "optimization does not claim RL training");

        System.out.println("[PASS] PolicyOptimizationService test passed.");
    }

    private static Map<String, Object> metrics(Object evaluation) {
        return castMap(castMap(evaluation).get("metrics"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : new java.util.LinkedHashMap<String, Object>();
    }

    private static double number(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("[FAIL] " + message);
        }
        System.out.println("[PASS] " + message);
    }
}
