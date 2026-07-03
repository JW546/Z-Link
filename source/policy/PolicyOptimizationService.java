package policy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Heuristic policy parameter optimizer. This searches weights using
 * ScenarioLibrary evaluation; it is not RL/MARL training.
 */
public class PolicyOptimizationService {
    private static final double MIN_SAFETY_WEIGHT = 0.34;
    private static final double MAX_ALLOWED_SAFETY_RISK_INCREASE = 0.0001;

    public AgentPolicy chooseBest(List<AgentPolicy> candidates, RewardFunction rewardFunction,
                                  List<AgentState> validationStates) {
        if (candidates == null || candidates.isEmpty()) {
            return state -> new AgentAction("HOLD", "");
        }
        PolicyEvaluator evaluator = new PolicyEvaluator();
        AgentPolicy best = candidates.get(0);
        double bestScore = Double.NEGATIVE_INFINITY;
        for (AgentPolicy candidate : candidates) {
            double score = evaluator.evaluate(candidate, rewardFunction, validationStates);
            if (score > bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    public Map<String, Object> optimize() {
        Map<String, Object> grid = gridSearch();
        Map<String, Object> random = randomSearch(48, 20260630L);
        Map<String, Object> best = betterCandidate(grid, random);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        Map<String, Object> before = evaluate(defaultConfig());
        Map<String, Object> after = evaluate(castMap(best.get("bestConfig")));
        result.put("method", "grid_search_plus_random_search");
        result.put("before", before);
        result.put("after", after);
        result.put("gridSearch", grid);
        result.put("randomSearch", random);
        result.put("bestPolicyConfiguration", best.get("bestConfig"));
        result.put("optimizationLog", best.get("optimizationLog"));
        result.put("improvement", improvement(before, after));
        result.put("constraints", constraints());
        result.put("trainingStatus", "heuristic_parameter_search_no_rl_training");
        return result;
    }

    public Map<String, Object> gridSearch() {
        List<Map<String, Object>> log = new ArrayList<Map<String, Object>>();
        Map<String, Object> bestConfig = defaultConfig();
        Map<String, Object> bestEvaluation = evaluate(bestConfig);
        double bestScore = objective(bestEvaluation, bestEvaluation);

        double[] time = new double[] {0.14, 0.20, 0.26};
        double[] safety = new double[] {0.34, 0.42, 0.50};
        double[] congestion = new double[] {0.10, 0.16};
        double[] conflict = new double[] {0.12, 0.20};
        double[] confidence = new double[] {0.10, 0.18};
        double[] freshness = new double[] {0.08, 0.14};
        double[] replan = new double[] {0.06, 0.18};

        Map<String, Object> baseline = bestEvaluation;
        for (double tw : time) {
            for (double sw : safety) {
                for (double cw : congestion) {
                    for (double fw : conflict) {
                        for (double confw : confidence) {
                            for (double freshw : freshness) {
                                for (double rw : replan) {
                                    Map<String, Object> config = config(tw, sw, cw, fw, confw, freshw, rw);
                                    Map<String, Object> evaluation = evaluate(config);
                                    double score = objective(evaluation, baseline);
                                    log.add(logEntry("grid", config, evaluation, score));
                                    if (score > bestScore && safeAgainstBaseline(evaluation, baseline)) {
                                        bestScore = score;
                                        bestConfig = config;
                                        bestEvaluation = evaluation;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return searchResult("grid", bestConfig, bestEvaluation, bestScore, log);
    }

    public Map<String, Object> randomSearch(int iterations, long seed) {
        Random random = new Random(seed);
        List<Map<String, Object>> log = new ArrayList<Map<String, Object>>();
        Map<String, Object> baseline = evaluate(defaultConfig());
        Map<String, Object> bestConfig = defaultConfig();
        Map<String, Object> bestEvaluation = baseline;
        double bestScore = objective(bestEvaluation, baseline);

        for (int i = 0; i < Math.max(1, iterations); i++) {
            Map<String, Object> config = config(
                range(random, 0.10, 0.30),
                range(random, MIN_SAFETY_WEIGHT, 0.58),
                range(random, 0.08, 0.22),
                range(random, 0.08, 0.26),
                range(random, 0.08, 0.24),
                range(random, 0.06, 0.20),
                range(random, 0.04, 0.24)
            );
            Map<String, Object> evaluation = evaluate(config);
            double score = objective(evaluation, baseline);
            log.add(logEntry("random", config, evaluation, score));
            if (score > bestScore && safeAgainstBaseline(evaluation, baseline)) {
                bestScore = score;
                bestConfig = config;
                bestEvaluation = evaluation;
            }
        }
        return searchResult("random", bestConfig, bestEvaluation, bestScore, log);
    }

    public Map<String, Object> evaluate(Map<String, Object> config) {
        WeightedUtilityAgentPolicy policy = new WeightedUtilityAgentPolicy();
        policy.configure(enforceConstraints(config));
        Map<String, Object> result = new PolicyEvaluator().evaluatePolicies(Arrays.asList(policy));
        Map<String, Object> policyResult = castMap(((List<?>) result.get("policyResults")).get(0));
        Map<String, Object> metrics = castMap(policyResult.get("metrics"));
        Map<String, Object> output = new LinkedHashMap<String, Object>();
        output.put("configuration", policy.configuration());
        output.put("metrics", metrics);
        output.put("policyResult", policyResult);
        return output;
    }

    private Map<String, Object> betterCandidate(Map<String, Object> left, Map<String, Object> right) {
        double leftScore = number(left, "bestScore", Double.NEGATIVE_INFINITY);
        double rightScore = number(right, "bestScore", Double.NEGATIVE_INFINITY);
        return rightScore > leftScore ? right : left;
    }

    private double objective(Map<String, Object> evaluation, Map<String, Object> baseline) {
        Map<String, Object> metrics = castMap(evaluation.get("metrics"));
        Map<String, Object> baselineMetrics = castMap(baseline.get("metrics"));
        double reward = number(metrics, "averageReward", 0.0);
        double risk = number(metrics, "averageSafetyRisk", 1.0);
        double baselineRisk = number(baselineMetrics, "averageSafetyRisk", risk);
        double conflict = number(metrics, "conflictResolutionAccuracy", 0.0);
        double time = number(metrics, "averageTravelTime", 30.0);
        double safetyPenalty = risk > baselineRisk + MAX_ALLOWED_SAFETY_RISK_INCREASE ? 10.0 : 0.0;
        return reward + conflict * 0.15 - risk * 0.35 - time / 250.0 - safetyPenalty;
    }

    private boolean safeAgainstBaseline(Map<String, Object> evaluation, Map<String, Object> baseline) {
        Map<String, Object> metrics = castMap(evaluation.get("metrics"));
        Map<String, Object> baselineMetrics = castMap(baseline.get("metrics"));
        return number(metrics, "averageSafetyRisk", 1.0)
            <= number(baselineMetrics, "averageSafetyRisk", 1.0) + MAX_ALLOWED_SAFETY_RISK_INCREASE;
    }

    private Map<String, Object> improvement(Map<String, Object> before, Map<String, Object> after) {
        Map<String, Object> b = castMap(before.get("metrics"));
        Map<String, Object> a = castMap(after.get("metrics"));
        Map<String, Object> improvement = new LinkedHashMap<String, Object>();
        improvement.put("averageRewardDelta", round(number(a, "averageReward", 0.0) - number(b, "averageReward", 0.0)));
        improvement.put("averageSafetyRiskDelta", round(number(a, "averageSafetyRisk", 0.0) - number(b, "averageSafetyRisk", 0.0)));
        improvement.put("averageTravelTimeDelta", round(number(a, "averageTravelTime", 0.0) - number(b, "averageTravelTime", 0.0)));
        improvement.put("conflictResolutionAccuracyDelta", round(number(a, "conflictResolutionAccuracy", 0.0) - number(b, "conflictResolutionAccuracy", 0.0)));
        return improvement;
    }

    private Map<String, Object> defaultConfig() {
        return config(0.22, 0.34, 0.14, 0.12, 0.10, 0.08, 0.08);
    }

    private Map<String, Object> config(double timeWeight, double safetyWeight, double congestionWeight,
                                       double conflictWeight, double confidenceWeight, double freshnessWeight,
                                       double replanningPenaltyWeight) {
        Map<String, Object> config = new LinkedHashMap<String, Object>();
        config.put("timeWeight", round(timeWeight));
        config.put("safetyWeight", round(Math.max(MIN_SAFETY_WEIGHT, safetyWeight)));
        config.put("congestionWeight", round(congestionWeight));
        config.put("conflictWeight", round(conflictWeight));
        config.put("confidenceWeight", round(confidenceWeight));
        config.put("freshnessWeight", round(freshnessWeight));
        config.put("replanningPenaltyWeight", round(replanningPenaltyWeight));
        return config;
    }

    private Map<String, Object> enforceConstraints(Map<String, Object> input) {
        Map<String, Object> config = input == null ? defaultConfig() : new LinkedHashMap<String, Object>(input);
        config.put("safetyWeight", Math.max(MIN_SAFETY_WEIGHT, number(config, "safetyWeight", MIN_SAFETY_WEIGHT)));
        return config;
    }

    private Map<String, Object> searchResult(String method, Map<String, Object> bestConfig,
                                             Map<String, Object> bestEvaluation, double bestScore,
                                             List<Map<String, Object>> log) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("method", method);
        result.put("bestConfig", bestConfig);
        result.put("bestEvaluation", bestEvaluation);
        result.put("bestScore", round(bestScore));
        result.put("optimizationLog", log);
        return result;
    }

    private Map<String, Object> logEntry(String method, Map<String, Object> config,
                                         Map<String, Object> evaluation, double score) {
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put("method", method);
        entry.put("config", config);
        entry.put("metrics", evaluation.get("metrics"));
        entry.put("score", round(score));
        return entry;
    }

    private Map<String, Object> constraints() {
        Map<String, Object> constraints = new LinkedHashMap<String, Object>();
        constraints.put("minSafetyWeight", MIN_SAFETY_WEIGHT);
        constraints.put("highRiskRoutes", "strongly penalized by DefaultRewardFunction and objective safety penalty");
        constraints.put("timeCannotOverrideSafety", true);
        constraints.put("trainingStatus", "heuristic_search_no_rl_training");
        return constraints;
    }

    private double range(Random random, double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : new LinkedHashMap<String, Object>();
    }

    private double number(Map<String, Object> map, String key, double fallback) {
        Object value = map == null ? null : map.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : fallback;
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
