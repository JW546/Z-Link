package integration;

import evaluation.BaselineComparison;
import evaluation.EvaluationService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class EvaluationServiceTest {
    public static void main(String[] args) {
        EvaluationService service = new EvaluationService();

        Map<String, Object> single = service.evaluateScenario("hospital-emergency");
        require(single.get("metrics") instanceof Map<?, ?>, "single scenario returns metrics");
        Map<?, ?> metrics = (Map<?, ?>) single.get("metrics");
        requireHas(metrics, "successRate");
        requireHas(metrics, "averageTravelTime");
        requireHas(metrics, "averageSafetyRisk");
        requireHas(metrics, "congestionAvoidanceRate");
        requireHas(metrics, "replanningCount");
        requireHas(metrics, "conflictResolutionAccuracy");
        requireHas(metrics, "explanationCompleteness");
        requireHas(metrics, "messageDelayImpact");
        requireHas(metrics, "packetLossImpact");
        requireHas(metrics, "averageReward");
        require("heuristic_evaluation_only_no_rl_training".equals(metrics.get("policyOptimizationMode")),
            "evaluation does not claim RL training");

        Map<String, Object> library = service.evaluateScenarioLibrary();
        require(((Number) library.get("scenarioCount")).intValue() >= 10, "batch evaluation covers ScenarioLibrary");
        require(library.get("aggregateMetrics") instanceof Map<?, ?>, "batch evaluation returns aggregate metrics");
        require(library.get("algorithmComparison") instanceof Map<?, ?>, "batch evaluation returns algorithm comparison");

        Map<String, Object> comparison = new BaselineComparison().compareScenario(
            agent.ScenarioLibrary.getInstance().get("uav-rsu-route-conflict"));
        Object algorithms = comparison.get("algorithms");
        require(algorithms instanceof List<?> && ((List<?>) algorithms).size() == 4,
            "BaselineComparison compares four planners");
        require(String.valueOf(comparison.get("bestAlgorithm")).length() > 0,
            "BaselineComparison returns best algorithm");

        Path log = service.exportExperimentLog("artifacts/evaluation-test");
        require(Files.exists(log), "evaluation log is exported");
        require(log.toString().endsWith(".json"), "evaluation log is JSON");

        System.out.println("[PASS] EvaluationService test passed.");
    }

    private static void requireHas(Map<?, ?> map, String key) {
        require(map.containsKey(key), "metrics include " + key);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("[FAIL] " + message);
        }
        System.out.println("[PASS] " + message);
    }
}
