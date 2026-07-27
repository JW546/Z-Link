package integration;

import policy.RouteTrainingService;

import java.util.List;
import java.util.Map;

public class RouteTrainingServiceTest {
    public static void main(String[] args) {
        RouteTrainingService service = new RouteTrainingService();
        Map<String, Object> report = service.runExperiment(80);

        require("tabular_q_learning_simulation_training_complete".equals(report.get("trainingStatus")),
            "training reports a real small-scale simulation status");
        require(((Number) report.get("episodes")).intValue() == 80, "training uses requested episode count");
        require(report.containsKey("stateEncoding"), "report documents state encoding");
        require(report.containsKey("actionSpace"), "report documents action space");
        require(report.containsKey("rewardDesign"), "report documents reward design");

        Map<String, Object> comparison = map(report.get("baselineComparison"));
        double learnedReward = number(comparison, "learnedAverageReward");
        double randomReward = number(comparison, "randomAverageReward");
        require(learnedReward >= randomReward, "learned policy is no worse than random baseline");
        require(comparison.containsKey("ruleBaselineAverageReward"), "rule baseline remains visible");

        List<?> learnedPolicy = (List<?>) report.get("learnedPolicy");
        require(!learnedPolicy.isEmpty(), "learned policy table is exported");
        require(String.valueOf(report.get("limitations")).contains("not DQN/PPO"),
            "report states limits and does not overclaim deep RL");

        System.out.println("[PASS] Route training service test passed.");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    private static double number(Map<String, Object> map, String key) {
        return ((Number) map.get(key)).doubleValue();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("[FAIL] " + message);
        }
        System.out.println("[PASS] " + message);
    }
}
