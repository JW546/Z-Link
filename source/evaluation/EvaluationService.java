package evaluation;

import agent.ScenarioLibrary;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import simulation.SimulationScenarioService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes experiment metrics for collaboration policies.
 */
public class EvaluationService {
    private final BaselineComparison baselineComparison = new BaselineComparison();

    public Map<String, Object> evaluate(int totalRuns, int successfulRuns, double arrivalMinutes,
                                        double riskExposure, int replanCount, int resolvedConflicts,
                                        int totalConflicts) {
        Map<String, Object> metrics = new LinkedHashMap<String, Object>();
        int safeTotalRuns = Math.max(1, totalRuns);
        int safeConflicts = Math.max(1, totalConflicts);
        metrics.put("successRate", round((double) successfulRuns / safeTotalRuns));
        metrics.put("arrivalTimeMinutes", round(arrivalMinutes));
        metrics.put("averageTravelTime", round(arrivalMinutes));
        metrics.put("riskExposure", round(riskExposure));
        metrics.put("averageSafetyRisk", round(riskExposure));
        metrics.put("replanCount", replanCount);
        metrics.put("replanningCount", replanCount);
        metrics.put("conflictResolutionAccuracy", round((double) resolvedConflicts / safeConflicts));
        return metrics;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> evaluateScenario(Map<String, Object> scenario, Map<String, Object> finalDecision) {
        Map<String, Object> targets = scenario == null
            ? new LinkedHashMap<String, Object>()
            : (Map<String, Object>) scenario.getOrDefault("evaluationTargets", new LinkedHashMap<String, Object>());
        List<String> expectedConflicts = scenario == null
            ? java.util.Collections.<String>emptyList()
            : (List<String>) scenario.getOrDefault("expectedConflicts", java.util.Collections.<String>emptyList());

        boolean hasDecision = finalDecision != null && finalDecision.get("finalRoute") != null;
        boolean replan = finalDecision != null && Boolean.TRUE.equals(finalDecision.get("whetherReplanningTriggered"));
        String conflictType = finalDecision == null ? "" : String.valueOf(finalDecision.getOrDefault("conflictType", ""));
        int matched = 0;
        for (String expected : expectedConflicts) {
            if (conflictType.contains(expected)) matched++;
        }
        int totalConflicts = Math.max(1, expectedConflicts.size());

        double risk = number(finalDecision, "riskScore", number(targets, "riskExposure", 0.35));
        double arrival = number(targets, "arrivalTimeMinutes", 24.0);
        int replanCount = replan ? Math.max(1, (int) number(targets, "replanCount", 1.0)) : 0;
        Map<String, Object> metrics = evaluate(1, hasDecision ? 1 : 0, arrival, risk, replanCount, matched, totalConflicts);
        metrics.put("congestionAvoidanceRate", congestionAvoidanceRate(scenario, finalDecision));
        metrics.put("explanationCompleteness", explanationCompleteness(finalDecision));
        metrics.put("messageDelayImpact", messageDelayImpact(scenario));
        metrics.put("packetLossImpact", packetLossImpact(scenario));
        metrics.put("averageReward", averageReward(metrics));
        metrics.put("targetSuccessRate", number(targets, "successRate", 0.0));
        metrics.put("targetConflictResolutionAccuracy", number(targets, "conflictResolutionAccuracy", 0.0));
        metrics.put("matchedExpectedConflicts", matched);
        metrics.put("totalExpectedConflicts", totalConflicts);
        metrics.put("policyOptimizationMode", "heuristic_evaluation_only_no_rl_training");
        return metrics;
    }

    public Map<String, Object> evaluateScenario(String scenarioId) {
        SimulationScenarioService simulation = new SimulationScenarioService();
        Map<String, Object> run = simulation.runScenario(scenarioId);
        Map<String, Object> scenario = ScenarioLibrary.getInstance().get(scenarioId);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("scenarioId", scenarioId);
        result.put("scenario", scenario);
        result.put("simulation", run);
        result.put("metrics", evaluateScenario(scenario, castMap(run.get("finalDecision"))));
        result.put("baselineComparison", baselineComparison.compareScenario(scenario));
        return result;
    }

    public Map<String, Object> evaluateScenarioLibrary() {
        List<Map<String, Object>> scenarioResults = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> scenario : ScenarioLibrary.getInstance().list()) {
            scenarioResults.add(evaluateScenario(String.valueOf(scenario.get("scenarioId"))));
        }
        Map<String, Object> aggregate = aggregateScenarioResults(scenarioResults);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("scenarioCount", scenarioResults.size());
        result.put("scenarioResults", scenarioResults);
        result.put("aggregateMetrics", aggregate);
        result.put("algorithmComparison", baselineComparison.compareScenarioLibrary());
        result.put("policyOptimizationMode", "heuristic_evaluation_only_no_rl_training");
        return result;
    }

    public Map<String, Object> compareBaselines(String scenarioId) {
        return baselineComparison.compareScenario(ScenarioLibrary.getInstance().get(scenarioId));
    }

    public Path exportExperimentLog(String outputDir) {
        Map<String, Object> report = evaluateScenarioLibrary();
        Path dir = Paths.get(outputDir == null || outputDir.isBlank()
            ? "artifacts/evaluation" : outputDir);
        try {
            Files.createDirectories(dir);
            Path file = dir.resolve("evaluation-log-" + System.currentTimeMillis() + ".json");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.write(file, gson.toJson(report).getBytes(StandardCharsets.UTF_8));
            return file;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export evaluation log: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> aggregateScenarioResults(List<Map<String, Object>> scenarioResults) {
        Map<String, Object> aggregate = new LinkedHashMap<String, Object>();
        if (scenarioResults == null || scenarioResults.isEmpty()) {
            return aggregate;
        }
        double successRate = 0.0;
        double travelTime = 0.0;
        double risk = 0.0;
        double congestionAvoidance = 0.0;
        double conflictAccuracy = 0.0;
        double explanation = 0.0;
        double delay = 0.0;
        double packetLoss = 0.0;
        double reward = 0.0;
        int replans = 0;
        for (Map<String, Object> item : scenarioResults) {
            Map<String, Object> metrics = castMap(item.get("metrics"));
            successRate += number(metrics, "successRate", 0.0);
            travelTime += number(metrics, "averageTravelTime", 0.0);
            risk += number(metrics, "averageSafetyRisk", 0.0);
            congestionAvoidance += number(metrics, "congestionAvoidanceRate", 0.0);
            conflictAccuracy += number(metrics, "conflictResolutionAccuracy", 0.0);
            explanation += number(metrics, "explanationCompleteness", 0.0);
            delay += number(metrics, "messageDelayImpact", 0.0);
            packetLoss += number(metrics, "packetLossImpact", 0.0);
            reward += number(metrics, "averageReward", 0.0);
            replans += (int) number(metrics, "replanningCount", 0.0);
        }
        int n = scenarioResults.size();
        aggregate.put("successRate", round(successRate / n));
        aggregate.put("averageTravelTime", round(travelTime / n));
        aggregate.put("averageSafetyRisk", round(risk / n));
        aggregate.put("congestionAvoidanceRate", round(congestionAvoidance / n));
        aggregate.put("replanningCount", replans);
        aggregate.put("conflictResolutionAccuracy", round(conflictAccuracy / n));
        aggregate.put("explanationCompleteness", round(explanation / n));
        aggregate.put("messageDelayImpact", round(delay / n));
        aggregate.put("packetLossImpact", round(packetLoss / n));
        aggregate.put("averageReward", round(reward / n));
        return aggregate;
    }

    private double congestionAvoidanceRate(Map<String, Object> scenario, Map<String, Object> finalDecision) {
        Map<String, Object> world = castMap(scenario == null ? null : scenario.get("initialWorldState"));
        double congestion = number(world, "congestionLevel", 0.30);
        String route = finalDecision == null ? "" : String.valueOf(finalDecision.getOrDefault("finalRoute", ""));
        if (congestion < 0.65) return 1.0;
        return route.contains("main_road") ? 0.35 : 0.92;
    }

    private double explanationCompleteness(Map<String, Object> finalDecision) {
        if (finalDecision == null) return 0.0;
        int present = 0;
        String[] keys = new String[] {
            "conflictType", "acceptedSuggestion", "rejectedSuggestions", "priorityOrder",
            "decisionReason", "confidence", "whetherReplanningTriggered", "timelineEvent"
        };
        for (String key : keys) {
            if (finalDecision.get(key) != null) present++;
        }
        return round(present / (double) keys.length);
    }

    @SuppressWarnings("unchecked")
    private double messageDelayImpact(Map<String, Object> scenario) {
        List<Map<String, Object>> messages = scenario == null
            ? Collections.<Map<String, Object>>emptyList()
            : (List<Map<String, Object>>) scenario.getOrDefault("ddsMessages", Collections.<Map<String, Object>>emptyList());
        if (messages.isEmpty()) return 0.0;
        double delay = 0.0;
        for (Map<String, Object> message : messages) {
            delay += number(message, "ddsDelayMs", 0.0);
        }
        return round(Math.min(1.0, (delay / messages.size()) / 2_000.0));
    }

    @SuppressWarnings("unchecked")
    private double packetLossImpact(Map<String, Object> scenario) {
        List<Map<String, Object>> messages = scenario == null
            ? Collections.<Map<String, Object>>emptyList()
            : (List<Map<String, Object>>) scenario.getOrDefault("ddsMessages", Collections.<Map<String, Object>>emptyList());
        if (messages.isEmpty()) return 0.0;
        double loss = 0.0;
        for (Map<String, Object> message : messages) {
            loss += number(message, "packetLossRate", 0.0);
        }
        return round(Math.min(1.0, loss / messages.size()));
    }

    private double averageReward(Map<String, Object> metrics) {
        double success = number(metrics, "successRate", 0.0);
        double risk = number(metrics, "averageSafetyRisk", 1.0);
        double conflict = number(metrics, "conflictResolutionAccuracy", 0.0);
        double explanation = number(metrics, "explanationCompleteness", 0.0);
        double delay = number(metrics, "messageDelayImpact", 0.0);
        double packetLoss = number(metrics, "packetLossImpact", 0.0);
        return round(success * 0.35 + (1.0 - risk) * 0.25 + conflict * 0.20
            + explanation * 0.10 - delay * 0.05 - packetLoss * 0.05);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private double number(Map<String, Object> map, String key, double fallback) {
        if (map == null) return fallback;
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : fallback;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        if (value instanceof Map<?, ?>) {
            return (Map<String, Object>) value;
        }
        return new LinkedHashMap<String, Object>();
    }
}
