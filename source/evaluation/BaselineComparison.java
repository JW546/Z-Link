package evaluation;

import agent.ScenarioLibrary;
import planner.DijkstraPlanner;
import planner.GreedyPlanner;
import planner.MultiObjectiveRoutePlanner;
import planner.PlanningContext;
import planner.PlanningResult;
import planner.RiskAwareRoutePlanner;
import planner.RouteCandidate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BaselineComparison {
    private final DijkstraPlanner dijkstraPlanner = new DijkstraPlanner();
    private final GreedyPlanner greedyPlanner = new GreedyPlanner();
    private final RiskAwareRoutePlanner riskAwareRoutePlanner = new RiskAwareRoutePlanner();
    private final MultiObjectiveRoutePlanner multiObjectiveRoutePlanner = new MultiObjectiveRoutePlanner();

    public Map<String, Object> compareScenario(Map<String, Object> scenario) {
        List<RouteCandidate> candidates = buildCandidates(scenario);
        PlanningContext context = buildContext(scenario);
        List<Map<String, Object>> algorithms = new ArrayList<Map<String, Object>>();
        algorithms.add(result("DijkstraPlanner", dijkstraPlanner.plan(candidates), candidates));
        algorithms.add(result("GreedyPlanner", greedyPlanner.plan(candidates), candidates));
        algorithms.add(result("RiskAwareRoutePlanner", riskAwareRoutePlanner.plan(candidates, context), candidates));
        algorithms.add(result("MultiObjectiveRoutePlanner", multiObjectiveRoutePlanner.plan(candidates, context), candidates));

        Map<String, Object> comparison = new LinkedHashMap<String, Object>();
        comparison.put("scenarioId", scenario == null ? "" : scenario.get("scenarioId"));
        comparison.put("algorithms", algorithms);
        comparison.put("bestAlgorithm", bestAlgorithm(algorithms));
        comparison.put("outputFormat", "json_ready_map");
        comparison.put("rlTrainingStatus", "not_trained_heuristic_evaluation_only");
        return comparison;
    }

    public Map<String, Object> compareScenarioLibrary() {
        List<Map<String, Object>> comparisons = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> scenario : ScenarioLibrary.getInstance().list()) {
            comparisons.add(compareScenario(scenario));
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("scenarioCount", comparisons.size());
        result.put("comparisons", comparisons);
        result.put("summary", summarize(comparisons));
        result.put("rlTrainingStatus", "not_trained_heuristic_evaluation_only");
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<RouteCandidate> buildCandidates(Map<String, Object> scenario) {
        List<Map<String, Object>> routes = scenario == null
            ? java.util.Collections.<Map<String, Object>>emptyList()
            : (List<Map<String, Object>>) scenario.getOrDefault("candidateRoutes", java.util.Collections.<Map<String, Object>>emptyList());
        List<RouteCandidate> candidates = new ArrayList<RouteCandidate>();
        Map<String, Object> world = scenario == null
            ? new LinkedHashMap<String, Object>()
            : castMap(scenario.get("initialWorldState"));
        for (Map<String, Object> route : routes) {
            String routeId = String.valueOf(route.get("routeId"));
            candidates.add(candidate(routeId, world, scenario));
        }
        if (candidates.isEmpty()) {
            candidates.add(candidate("route_A_main_road", world, scenario));
            candidates.add(candidate("route_B_school_zone_slow_safe", world, scenario));
            candidates.add(candidate("route_C_elevated_detour", world, scenario));
        }
        return candidates;
    }

    private RouteCandidate candidate(String routeId, Map<String, Object> world, Map<String, Object> scenario) {
        double congestion = number(world, "congestionLevel", 0.35);
        double baseRisk = routeId.contains("A") ? 0.58 : routeId.contains("B") ? 0.24 : routeId.contains("C") ? 0.32 : 0.28;
        double time = routeId.contains("A") ? 10.0 : routeId.contains("D") ? 11.5 : routeId.contains("C") ? 15.0 : 17.0;
        double evidenceRisk = affectedRiskFor(routeId, scenario);
        double risk = Math.max(baseRisk, evidenceRisk);
        double confidence = averageMessageValue(scenario, "confidence", 0.82);
        double sourceTrust = routeId.contains("B") || routeId.contains("D") ? 0.90 : 0.76;
        double freshness = 1.0 - Math.min(0.8, averageMessageValue(scenario, "ddsDelayMs", 0.0) / 2_000.0);
        double packetLoss = averageMessageValue(scenario, "packetLossRate", 0.0);
        double conflictPenalty = expectedConflictCount(scenario) > 1 ? 0.18 : 0.08;
        double emergency = routeId.contains("D") ? 0.90 : 0.20;
        return new RouteCandidate(routeId, time, risk, congestion, emergency, confidence,
            sourceTrust, freshness, conflictPenalty, averageMessageValue(scenario, "ddsDelayMs", 0.0), packetLoss);
    }

    @SuppressWarnings("unchecked")
    private double affectedRiskFor(String routeId, Map<String, Object> scenario) {
        if (scenario == null) return 0.0;
        List<Map<String, Object>> messages = (List<Map<String, Object>>) scenario.getOrDefault("ddsMessages",
            java.util.Collections.<Map<String, Object>>emptyList());
        double risk = 0.0;
        for (Map<String, Object> message : messages) {
            if (routeId.equals(String.valueOf(message.getOrDefault("affectedRoute", "")))) {
                risk = Math.max(risk, number(message, "affectedRiskScore", 0.0));
            }
            if (routeId.equals(String.valueOf(message.getOrDefault("route", "")))) {
                risk = Math.max(risk, number(message, "riskScore", 0.0));
            }
        }
        return risk;
    }

    @SuppressWarnings("unchecked")
    private double averageMessageValue(Map<String, Object> scenario, String key, double fallback) {
        if (scenario == null) return fallback;
        List<Map<String, Object>> messages = (List<Map<String, Object>>) scenario.getOrDefault("ddsMessages",
            java.util.Collections.<Map<String, Object>>emptyList());
        if (messages.isEmpty()) return fallback;
        double total = 0.0;
        for (Map<String, Object> message : messages) {
            total += number(message, key, fallback);
        }
        return total / messages.size();
    }

    @SuppressWarnings("unchecked")
    private int expectedConflictCount(Map<String, Object> scenario) {
        if (scenario == null) return 0;
        List<String> conflicts = (List<String>) scenario.getOrDefault("expectedConflicts", java.util.Collections.<String>emptyList());
        return conflicts.size();
    }

    private PlanningContext buildContext(Map<String, Object> scenario) {
        String intent = scenario == null ? "" : String.valueOf(scenario.getOrDefault("passengerIntent", ""));
        PlanningContext context = intent.contains("安全") ? PlanningContext.safetyFirst() : PlanningContext.fastest();
        context.passengerUrgent = intent.contains("尽快") || intent.contains("快");
        context.passengerSafetyFirst = intent.contains("安全") || intent.contains("避开");
        context.safetyRiskThreshold = context.passengerSafetyFirst ? 0.62 : 0.75;
        return context;
    }

    private Map<String, Object> result(String name, PlanningResult planningResult, List<RouteCandidate> candidates) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        RouteCandidate selected = find(planningResult.routeId, candidates);
        result.put("algorithm", name);
        result.put("routeId", planningResult.routeId);
        result.put("score", round(planningResult.score));
        result.put("averageTravelTime", selected == null ? 0.0 : round(selected.travelTime));
        result.put("averageSafetyRisk", selected == null ? 1.0 : round(selected.safetyRisk));
        result.put("congestionAvoidanceRate", selected == null ? 0.0 : (selected.routeId.contains("main_road") ? 0.35 : 0.90));
        result.put("explanation", planningResult.explanation);
        result.put("baseline", name.contains("Dijkstra") || name.contains("Greedy"));
        return result;
    }

    private RouteCandidate find(String routeId, List<RouteCandidate> candidates) {
        for (RouteCandidate candidate : candidates) {
            if (candidate.routeId.equals(routeId)) return candidate;
        }
        return null;
    }

    private String bestAlgorithm(List<Map<String, Object>> algorithms) {
        String best = "";
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Map<String, Object> algorithm : algorithms) {
            double reward = (1.0 - number(algorithm, "averageSafetyRisk", 1.0)) * 0.50
                + number(algorithm, "congestionAvoidanceRate", 0.0) * 0.30
                - number(algorithm, "averageTravelTime", 30.0) / 100.0;
            if (reward > bestScore) {
                best = String.valueOf(algorithm.get("algorithm"));
                bestScore = reward;
            }
        }
        return best;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> summarize(List<Map<String, Object>> comparisons) {
        Map<String, Integer> wins = new LinkedHashMap<String, Integer>();
        for (Map<String, Object> comparison : comparisons) {
            String best = String.valueOf(comparison.get("bestAlgorithm"));
            wins.put(best, wins.getOrDefault(best, 0) + 1);
        }
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("winsByAlgorithm", wins);
        summary.put("note", "DijkstraPlanner and GreedyPlanner are baselines; no RL/MARL training is claimed.");
        return summary;
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
        return Math.round(value * 100.0) / 100.0;
    }
}
