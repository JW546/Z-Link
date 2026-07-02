package integration;

import agent.ScenarioLibrary;
import evaluation.EvaluationService;
import simulation.SimulationScenarioService;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ScenarioLibraryTest {
    private static final List<String> REQUIRED_IDS = Arrays.asList(
        "hospital-emergency",
        "congestion-detour",
        "accident-avoidance",
        "uav-rsu-route-conflict",
        "neighbor-following-risk-conflict",
        "dds-delay",
        "dds-packet-loss",
        "low-confidence-message",
        "dynamic-replanning",
        "passenger-intent-tension"
    );

    public static void main(String[] args) {
        ScenarioLibrary library = ScenarioLibrary.getInstance();
        SimulationScenarioService simulation = new SimulationScenarioService();
        EvaluationService evaluation = new EvaluationService();

        require(library.list().size() >= REQUIRED_IDS.size(), "ScenarioLibrary contains at least ten scenarios");
        for (String id : REQUIRED_IDS) {
            Map<String, Object> scenario = library.get(id);
            require(id.equals(String.valueOf(scenario.get("scenarioId"))), id + " can be loaded");
            require(hasField(scenario, "passengerIntent"), id + " has passengerIntent");
            require(hasField(scenario, "initialWorldState"), id + " has initialWorldState");
            require(hasNonEmptyList(scenario, "ddsMessages"), id + " has ddsMessages");
            require(hasNonEmptyList(scenario, "candidateRoutes"), id + " has candidateRoutes");
            require(hasNonEmptyList(scenario, "expectedConflicts"), id + " has expectedConflicts");
            require(hasField(scenario, "expectedDecisionPrinciple"), id + " has expectedDecisionPrinciple");
            require(hasField(scenario, "evaluationTargets"), id + " has evaluationTargets");

            Map<String, Object> run = simulation.runScenario(id);
            require(hasNonEmptyList(run, "ddsTimeline"), id + " outputs DDS timeline");
            require(run.get("finalDecision") instanceof Map<?, ?>, id + " outputs final decision");
            Map<?, ?> finalDecision = (Map<?, ?>) run.get("finalDecision");
            require(finalDecision.get("finalRoute") != null, id + " final decision includes route");
            require(finalDecision.get("timelineEvent") instanceof Map<?, ?>, id + " final decision includes timeline event");
            require(run.get("evaluation") instanceof Map<?, ?>, id + " outputs evaluation");
            Map<String, Object> metrics = evaluation.evaluateScenario(scenario, castMap(run.get("finalDecision")));
            require(metrics.containsKey("successRate"), id + " can be used by EvaluationService");
        }

        System.out.println("[PASS] ScenarioLibrary test passed.");
    }

    private static boolean hasField(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null && !String.valueOf(value).isBlank();
    }

    private static boolean hasNonEmptyList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof List<?> && !((List<?>) value).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("[FAIL] " + message);
        }
        System.out.println("[PASS] " + message);
    }
}
