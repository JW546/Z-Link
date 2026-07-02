package planner;

import java.util.List;

public class GreedyPlanner {
    public PlanningResult plan(List<RouteCandidate> candidates) {
        RouteCandidate best = null;
        double bestScore = Double.POSITIVE_INFINITY;
        for (RouteCandidate candidate : candidates) {
            double score = candidate.travelTime + candidate.congestionLevel * 12.0;
            if (score < bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        PlanningResult result = new PlanningResult();
        result.algorithm = "Greedy baseline";
        result.routeId = best == null ? null : best.routeId;
        result.score = bestScore;
        result.explanation = "Baseline greedily uses current time and congestion only.";
        return result;
    }
}
