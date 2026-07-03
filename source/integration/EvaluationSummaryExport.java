package integration;

import agent.ScenarioLibrary;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import evaluation.BaselineComparison;
import evaluation.EvaluationService;
import policy.PolicyEvaluator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Command-line exporter for ScenarioLibrary experiment summaries.
 */
public class EvaluationSummaryExport {
    private static final List<String> METRICS = Arrays.asList(
        "successRate",
        "averageTravelTime",
        "averageSafetyRisk",
        "conflictResolutionAccuracy",
        "replanningCount",
        "explanationCompleteness",
        "averageReward",
        "messageDelayImpact",
        "packetLossImpact"
    );

    public static void main(String[] args) throws IOException {
        Path outputDir = Paths.get(args.length > 0 ? args[0] : "results");
        Files.createDirectories(outputDir);

        EvaluationService evaluationService = new EvaluationService();
        BaselineComparison baselineComparison = new BaselineComparison();
        PolicyEvaluator policyEvaluator = new PolicyEvaluator();

        Map<String, Object> libraryEvaluation = evaluationService.evaluateScenarioLibrary();
        Map<String, Object> plannerComparison = baselineComparison.compareScenarioLibrary();
        Map<String, Object> policyComparison = policyEvaluator.evaluateDefaultPolicies();

        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        rows.addAll(plannerRows(castList(plannerComparison.get("comparisons"))));
        rows.addAll(policyRows(castList(policyComparison.get("policyResults")),
            castMap(libraryEvaluation.get("aggregateMetrics"))));

        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("schemaVersion", "evaluation-summary/v1");
        summary.put("scenarioCount", libraryEvaluation.get("scenarioCount"));
        summary.put("metrics", METRICS);
        summary.put("zeroValuePolicy", zeroValuePolicy());
        summary.put("aggregateScenarioMetrics", libraryEvaluation.get("aggregateMetrics"));
        summary.put("comparisons", rows);
        summary.put("plannerComparisonRaw", plannerComparison);
        summary.put("policyComparisonRaw", policyComparison);
        summary.put("scenarioEvaluationRaw", libraryEvaluation);
        summary.put("trainingStatus", "heuristic_evaluation_and_weight_search_only_no_rl_or_marl_training");

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Files.write(outputDir.resolve("evaluation_summary.json"),
            gson.toJson(summary).getBytes(StandardCharsets.UTF_8));
        Files.write(outputDir.resolve("evaluation_summary.csv"),
            toCsv(rows).getBytes(StandardCharsets.UTF_8));

        System.out.println("[PASS] exported " + outputDir.resolve("evaluation_summary.json"));
        System.out.println("[PASS] exported " + outputDir.resolve("evaluation_summary.csv"));
        System.out.println(gson.toJson(summary.get("comparisons")));
    }

    private static List<Map<String, Object>> plannerRows(List<Map<String, Object>> comparisons) {
        Map<String, List<Map<String, Object>>> byPlanner = new LinkedHashMap<String, List<Map<String, Object>>>();
        for (Map<String, Object> comparison : comparisons) {
            for (Map<String, Object> algorithm : castList(comparison.get("algorithms"))) {
                String name = string(algorithm.get("algorithm"));
                if (!byPlanner.containsKey(name)) byPlanner.put(name, new ArrayList<Map<String, Object>>());
                Map<String, Object> item = new LinkedHashMap<String, Object>(algorithm);
                item.put("scenarioId", comparison.get("scenarioId"));
                byPlanner.get(name).add(item);
            }
        }

        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : byPlanner.entrySet()) {
            List<Map<String, Object>> items = entry.getValue();
            double travel = avg(items, "averageTravelTime");
            double risk = avg(items, "averageSafetyRisk");
            double success = avgSafetySuccess(items);
            double explanation = avgExplanationPresence(items);
            double reward = reward(success, risk, 0.0, explanation, 0.0, 0.0);

            Map<String, Object> row = baseRow("planner", entry.getKey());
            putMetric(row, "successRate", round(success), "derived_from_route_risk_threshold");
            putMetric(row, "averageTravelTime", round(travel), "measured_by_planner_comparison");
            putMetric(row, "averageSafetyRisk", round(risk), "measured_by_planner_comparison");
            putMetric(row, "conflictResolutionAccuracy", null, "not_available_planner_comparison_does_not_emit_conflict_outcome");
            putMetric(row, "replanningCount", null, "not_available_planner_comparison_does_not_emit_replanning_count");
            putMetric(row, "explanationCompleteness", round(explanation), "derived_from_explanation_presence");
            putMetric(row, "averageReward", round(reward), "derived_from_available_planner_metrics");
            putMetric(row, "messageDelayImpact", null, "not_available_planner_comparison_does_not_emit_message_quality_metric");
            putMetric(row, "packetLossImpact", null, "not_available_planner_comparison_does_not_emit_message_quality_metric");
            row.put("sampleCount", items.size());
            rows.add(row);
        }
        return rows;
    }

    private static List<Map<String, Object>> policyRows(List<Map<String, Object>> policyResults,
                                                        Map<String, Object> scenarioAggregate) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> policy : policyResults) {
            Map<String, Object> metrics = castMap(policy.get("metrics"));
            Map<String, Object> row = baseRow("policy", string(policy.get("policyName")));
            for (String metric : METRICS) {
                Object value = metrics.get(metric);
                if (value instanceof Number) {
                    putMetric(row, metric, round(((Number) value).doubleValue()), "measured_by_policy_evaluator");
                } else if ("messageDelayImpact".equals(metric) || "packetLossImpact".equals(metric)) {
                    putMetric(row, metric, number(scenarioAggregate, metric, 0.0),
                        "contextual_scenario_metric_not_policy_specific");
                } else {
                    putMetric(row, metric, null, "not_available_policy_evaluator_does_not_emit_metric");
                }
            }
            row.put("sampleCount", castList(policy.get("scenarioResults")).size());
            row.put("trainingStatus", policy.get("trainingStatus"));
            rows.add(row);
        }
        return rows;
    }

    private static Map<String, Object> baseRow(String category, String name) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("category", category);
        row.put("name", name);
        row.put("metrics", new LinkedHashMap<String, Object>());
        return row;
    }

    @SuppressWarnings("unchecked")
    private static void putMetric(Map<String, Object> row, String name, Object value, String status) {
        Map<String, Object> metrics = (Map<String, Object>) row.get("metrics");
        Map<String, Object> metric = new LinkedHashMap<String, Object>();
        metric.put("value", value);
        metric.put("status", status);
        if (value instanceof Number && ((Number) value).doubleValue() == 0.0) {
            metric.put("zeroMeaning", status.startsWith("measured") || status.startsWith("derived")
                ? "true_zero_or_threshold_result" : "no_data");
        }
        metrics.put(name, metric);
    }

    private static Map<String, Object> zeroValuePolicy() {
        Map<String, Object> policy = new LinkedHashMap<String, Object>();
        policy.put("trueZeroStatuses", Arrays.asList(
            "measured_by_policy_evaluator",
            "measured_by_planner_comparison",
            "derived_from_route_risk_threshold",
            "derived_from_explanation_presence",
            "derived_from_available_planner_metrics",
            "contextual_scenario_metric_not_policy_specific"
        ));
        policy.put("noDataStatuses", Arrays.asList(
            "not_available_planner_comparison_does_not_emit_conflict_outcome",
            "not_available_planner_comparison_does_not_emit_replanning_count",
            "not_available_planner_comparison_does_not_emit_message_quality_metric",
            "not_available_policy_evaluator_does_not_emit_metric"
        ));
        return policy;
    }

    @SuppressWarnings("unchecked")
    private static String toCsv(List<Map<String, Object>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("category,name,sampleCount");
        for (String metric : METRICS) {
            sb.append(",").append(metric).append(",").append(metric).append("_status");
        }
        sb.append("\n");
        for (Map<String, Object> row : rows) {
            sb.append(csv(row.get("category"))).append(",")
                .append(csv(row.get("name"))).append(",")
                .append(csv(row.get("sampleCount")));
            Map<String, Object> metrics = (Map<String, Object>) row.get("metrics");
            for (String metric : METRICS) {
                Map<String, Object> item = castMap(metrics.get(metric));
                sb.append(",").append(csv(item.get("value")))
                    .append(",").append(csv(item.get("status")));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private static String csv(Object value) {
        if (value == null) return "";
        String text = String.valueOf(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private static double avg(List<Map<String, Object>> items, String key) {
        if (items.isEmpty()) return 0.0;
        double total = 0.0;
        for (Map<String, Object> item : items) total += number(item, key, 0.0);
        return total / items.size();
    }

    private static double avgSafetySuccess(List<Map<String, Object>> items) {
        if (items.isEmpty()) return 0.0;
        double success = 0.0;
        for (Map<String, Object> item : items) {
            success += number(item, "averageSafetyRisk", 1.0) <= 0.62 ? 1.0 : 0.0;
        }
        return success / items.size();
    }

    private static double avgExplanationPresence(List<Map<String, Object>> items) {
        if (items.isEmpty()) return 0.0;
        double present = 0.0;
        for (Map<String, Object> item : items) {
            String explanation = string(item.get("explanation"));
            present += explanation.length() >= 12 ? 1.0 : 0.0;
        }
        return present / items.size();
    }

    private static double reward(double success, double risk, double conflict, double explanation,
                                 double delay, double packetLoss) {
        return success * 0.35 + (1.0 - risk) * 0.25 + conflict * 0.20
            + explanation * 0.10 - delay * 0.05 - packetLoss * 0.05;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castList(Object value) {
        if (!(value instanceof List<?>)) return new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object item : (List<?>) value) {
            if (item instanceof Map<?, ?>) result.add((Map<String, Object>) item);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : new LinkedHashMap<String, Object>();
    }

    private static double number(Map<String, Object> map, String key, double fallback) {
        Object value = map == null ? null : map.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : fallback;
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
