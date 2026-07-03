package integration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import policy.AdaptiveAgentPolicy;
import policy.PolicyEvaluator;
import policy.PolicyOptimizationService;
import policy.WeightedUtilityAgentPolicy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Exports heuristic policy optimization reports. This is not RL training.
 */
public class PolicyOptimizationExport {
    public static void main(String[] args) throws IOException {
        Path outputDir = Paths.get(args.length > 0 ? args[0] : "results");
        Files.createDirectories(outputDir);

        PolicyOptimizationService optimizer = new PolicyOptimizationService();
        PolicyEvaluator evaluator = new PolicyEvaluator();
        Map<String, Object> weightedOptimization = optimizer.optimize();
        Map<String, Object> bestConfig = castMap(weightedOptimization.get("bestPolicyConfiguration"));

        AdaptiveAgentPolicy adaptiveBefore = new AdaptiveAgentPolicy();
        Map<String, Object> adaptiveBeforeConfig = adaptiveBefore.configuration();
        Map<String, Object> adaptiveBeforeEvaluation = evaluator.evaluatePolicy(adaptiveBefore);

        AdaptiveAgentPolicy adaptiveAfter = new AdaptiveAgentPolicy();
        adaptiveAfter.configure(bestConfig);
        Map<String, Object> adaptiveSeedConfig = adaptiveAfter.configuration();
        Map<String, Object> adaptiveAfterEvaluation = evaluator.evaluatePolicy(adaptiveAfter);

        Map<String, Object> report = new LinkedHashMap<String, Object>();
        report.put("schemaVersion", "policy-optimization/v1");
        report.put("optimizationType", "heuristic policy optimization");
        report.put("trainingStatus", "heuristic_policy_optimization_no_rl_training");
        report.put("weightedUtility", weightedSummary(weightedOptimization));
        report.put("adaptivePolicy", adaptiveSummary(
            adaptiveBeforeConfig, adaptiveBeforeEvaluation,
            adaptiveSeedConfig, adaptiveAfterEvaluation));
        report.put("safetyCheck", safetyCheck(report));
        report.put("rawWeightedOptimization", weightedOptimization);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Files.write(outputDir.resolve("policy_optimization.json"),
            gson.toJson(report).getBytes(StandardCharsets.UTF_8));
        Files.write(outputDir.resolve("policy_optimization.csv"),
            csv(report).getBytes(StandardCharsets.UTF_8));
        Files.write(outputDir.resolve("policy_optimization_report.md"),
            markdown(report).getBytes(StandardCharsets.UTF_8));

        System.out.println("[PASS] exported " + outputDir.resolve("policy_optimization.json"));
        System.out.println("[PASS] exported " + outputDir.resolve("policy_optimization.csv"));
        System.out.println("[PASS] exported " + outputDir.resolve("policy_optimization_report.md"));
        System.out.println(gson.toJson(compactConsole(report)));
    }

    private static Map<String, Object> weightedSummary(Map<String, Object> optimized) {
        Map<String, Object> before = castMap(optimized.get("before"));
        Map<String, Object> after = castMap(optimized.get("after"));
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("policyName", "WeightedUtilityAgentPolicy");
        summary.put("optimizationType", "grid_search_plus_random_search");
        summary.put("beforeWeights", before.get("configuration"));
        summary.put("afterWeights", after.get("configuration"));
        summary.put("beforeMetrics", before.get("metrics"));
        summary.put("afterMetrics", after.get("metrics"));
        summary.put("improvement", optimized.get("improvement"));
        summary.put("bestPolicyConfiguration", optimized.get("bestPolicyConfiguration"));
        summary.put("rewardImprovedButSafetyRiskIncreased", rewardUpRiskUp(before, after));
        summary.put("trainingStatus", "heuristic_parameter_search_no_rl_training");
        return summary;
    }

    private static Map<String, Object> adaptiveSummary(Map<String, Object> beforeConfig,
                                                       Map<String, Object> beforeEvaluation,
                                                       Map<String, Object> seedConfig,
                                                       Map<String, Object> afterEvaluation) {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("policyName", "AdaptiveAgentPolicy");
        summary.put("optimizationType", "heuristic_adaptive_update_seeded_by_best_weight_search");
        summary.put("beforeWeights", beforeConfig);
        summary.put("afterWeights", castMap(afterEvaluation).get("configuration"));
        summary.put("seedWeightsBeforeAdaptiveEvaluation", seedConfig);
        summary.put("beforeMetrics", castMap(beforeEvaluation.get("metrics")));
        summary.put("afterMetrics", castMap(afterEvaluation.get("metrics")));
        summary.put("improvement", improvement(castMap(beforeEvaluation.get("metrics")),
            castMap(afterEvaluation.get("metrics"))));
        summary.put("bestPolicyConfiguration", castMap(afterEvaluation).get("configuration"));
        summary.put("rewardImprovedButSafetyRiskIncreased", rewardUpRiskUp(
            metricBox(beforeEvaluation), metricBox(afterEvaluation)));
        summary.put("trainingStatus", "adaptive_heuristic_weight_update_no_rl_training");
        return summary;
    }

    private static Map<String, Object> safetyCheck(Map<String, Object> report) {
        Map<String, Object> weighted = castMap(report.get("weightedUtility"));
        Map<String, Object> adaptive = castMap(report.get("adaptivePolicy"));
        boolean weightedRiskRegression = Boolean.TRUE.equals(weighted.get("rewardImprovedButSafetyRiskIncreased"));
        boolean adaptiveRiskRegression = Boolean.TRUE.equals(adaptive.get("rewardImprovedButSafetyRiskIncreased"));
        Map<String, Object> check = new LinkedHashMap<String, Object>();
        check.put("anyRewardImprovedButSafetyRiskIncreased", weightedRiskRegression || adaptiveRiskRegression);
        check.put("weightedUtilityRewardImprovedButSafetyRiskIncreased", weightedRiskRegression);
        check.put("adaptiveRewardImprovedButSafetyRiskIncreased", adaptiveRiskRegression);
        check.put("interpretation", weightedRiskRegression || adaptiveRiskRegression
            ? "reward improved with safety-risk regression; do not accept without safety review"
            : "no reward improvement with safety-risk regression detected");
        return check;
    }

    private static Map<String, Object> improvement(Map<String, Object> before, Map<String, Object> after) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("averageRewardDelta", round(number(after, "averageReward") - number(before, "averageReward")));
        result.put("averageSafetyRiskDelta", round(number(after, "averageSafetyRisk") - number(before, "averageSafetyRisk")));
        result.put("conflictResolutionAccuracyDelta", round(number(after, "conflictResolutionAccuracy") - number(before, "conflictResolutionAccuracy")));
        result.put("averageTravelTimeDelta", round(number(after, "averageTravelTime") - number(before, "averageTravelTime")));
        return result;
    }

    private static boolean rewardUpRiskUp(Map<String, Object> beforeBox, Map<String, Object> afterBox) {
        Map<String, Object> before = castMap(beforeBox.get("metrics"));
        Map<String, Object> after = castMap(afterBox.get("metrics"));
        return number(after, "averageReward") > number(before, "averageReward")
            && number(after, "averageSafetyRisk") > number(before, "averageSafetyRisk");
    }

    private static Map<String, Object> metricBox(Map<String, Object> evaluation) {
        Map<String, Object> box = new LinkedHashMap<String, Object>();
        box.put("metrics", castMap(evaluation.get("metrics")));
        return box;
    }

    private static Map<String, Object> compactConsole(Map<String, Object> report) {
        Map<String, Object> compact = new LinkedHashMap<String, Object>();
        compact.put("weightedUtility", compactPolicy(castMap(report.get("weightedUtility"))));
        compact.put("adaptivePolicy", compactPolicy(castMap(report.get("adaptivePolicy"))));
        compact.put("safetyCheck", report.get("safetyCheck"));
        compact.put("trainingStatus", report.get("trainingStatus"));
        return compact;
    }

    private static Map<String, Object> compactPolicy(Map<String, Object> policy) {
        Map<String, Object> compact = new LinkedHashMap<String, Object>();
        compact.put("policyName", policy.get("policyName"));
        compact.put("beforeWeights", policy.get("beforeWeights"));
        compact.put("afterWeights", policy.get("afterWeights"));
        compact.put("beforeMetrics", policy.get("beforeMetrics"));
        compact.put("afterMetrics", policy.get("afterMetrics"));
        compact.put("improvement", policy.get("improvement"));
        compact.put("rewardImprovedButSafetyRiskIncreased", policy.get("rewardImprovedButSafetyRiskIncreased"));
        compact.put("trainingStatus", policy.get("trainingStatus"));
        return compact;
    }

    private static String csv(Map<String, Object> report) {
        StringBuilder sb = new StringBuilder();
        sb.append("policy,optimizationType,beforeWeights,afterWeights,beforeAverageReward,afterAverageReward,")
            .append("beforeAverageSafetyRisk,afterAverageSafetyRisk,beforeConflictResolutionAccuracy,")
            .append("afterConflictResolutionAccuracy,rewardDelta,safetyRiskDelta,conflictAccuracyDelta,")
            .append("rewardImprovedButSafetyRiskIncreased,trainingStatus\n");
        appendPolicyCsv(sb, castMap(report.get("weightedUtility")));
        appendPolicyCsv(sb, castMap(report.get("adaptivePolicy")));
        return sb.toString();
    }

    private static void appendPolicyCsv(StringBuilder sb, Map<String, Object> policy) {
        Map<String, Object> before = castMap(policy.get("beforeMetrics"));
        Map<String, Object> after = castMap(policy.get("afterMetrics"));
        Map<String, Object> improvement = castMap(policy.get("improvement"));
        sb.append(csvValue(policy.get("policyName"))).append(",")
            .append(csvValue(policy.get("optimizationType"))).append(",")
            .append(csvValue(policy.get("beforeWeights"))).append(",")
            .append(csvValue(policy.get("afterWeights"))).append(",")
            .append(number(before, "averageReward")).append(",")
            .append(number(after, "averageReward")).append(",")
            .append(number(before, "averageSafetyRisk")).append(",")
            .append(number(after, "averageSafetyRisk")).append(",")
            .append(number(before, "conflictResolutionAccuracy")).append(",")
            .append(number(after, "conflictResolutionAccuracy")).append(",")
            .append(number(improvement, "averageRewardDelta")).append(",")
            .append(number(improvement, "averageSafetyRiskDelta")).append(",")
            .append(number(improvement, "conflictResolutionAccuracyDelta")).append(",")
            .append(policy.get("rewardImprovedButSafetyRiskIncreased")).append(",")
            .append(csvValue(policy.get("trainingStatus"))).append("\n");
    }

    private static String markdown(Map<String, Object> report) {
        Map<String, Object> weighted = castMap(report.get("weightedUtility"));
        Map<String, Object> adaptive = castMap(report.get("adaptivePolicy"));
        Map<String, Object> safety = castMap(report.get("safetyCheck"));
        StringBuilder sb = new StringBuilder();
        sb.append("# Policy Optimization Report\n\n");
        sb.append("Optimization type: heuristic policy optimization.\n\n");
        sb.append("Training status: heuristic policy optimization only; no RL / MARL training, no neural network training, no checkpoint.\n\n");
        sb.append("## Summary\n\n");
        sb.append("| Policy | Before reward | After reward | Before safetyRisk | After safetyRisk | Before conflictAccuracy | After conflictAccuracy | Reward up with safety risk up |\n");
        sb.append("| --- | ---: | ---: | ---: | ---: | ---: | ---: | --- |\n");
        appendPolicyMdRow(sb, weighted);
        appendPolicyMdRow(sb, adaptive);
        sb.append("\n## WeightedUtilityAgentPolicy\n\n");
        appendDetails(sb, weighted);
        sb.append("\n## AdaptiveAgentPolicy\n\n");
        appendDetails(sb, adaptive);
        sb.append("\n## Safety Check\n\n");
        sb.append("- Any reward improvement with safety-risk increase: ")
            .append(safety.get("anyRewardImprovedButSafetyRiskIncreased")).append("\n");
        sb.append("- Interpretation: ").append(safety.get("interpretation")).append("\n");
        return sb.toString();
    }

    private static void appendPolicyMdRow(StringBuilder sb, Map<String, Object> policy) {
        Map<String, Object> before = castMap(policy.get("beforeMetrics"));
        Map<String, Object> after = castMap(policy.get("afterMetrics"));
        sb.append("| ").append(policy.get("policyName"))
            .append(" | ").append(number(before, "averageReward"))
            .append(" | ").append(number(after, "averageReward"))
            .append(" | ").append(number(before, "averageSafetyRisk"))
            .append(" | ").append(number(after, "averageSafetyRisk"))
            .append(" | ").append(number(before, "conflictResolutionAccuracy"))
            .append(" | ").append(number(after, "conflictResolutionAccuracy"))
            .append(" | ").append(policy.get("rewardImprovedButSafetyRiskIncreased"))
            .append(" |\n");
    }

    private static void appendDetails(StringBuilder sb, Map<String, Object> policy) {
        sb.append("- Optimization type: ").append(policy.get("optimizationType")).append("\n");
        sb.append("- Before weights: `").append(policy.get("beforeWeights")).append("`\n");
        sb.append("- After weights: `").append(policy.get("afterWeights")).append("`\n");
        sb.append("- Best policy parameters: `").append(policy.get("bestPolicyConfiguration")).append("`\n");
        sb.append("- Improvement: `").append(policy.get("improvement")).append("`\n");
        sb.append("- Training status: `").append(policy.get("trainingStatus")).append("`\n");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : new LinkedHashMap<String, Object>();
    }

    private static double number(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0;
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private static String csvValue(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
