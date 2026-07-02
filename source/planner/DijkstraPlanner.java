package planner;

import java.util.List;

public class DijkstraPlanner {
    public PlanningResult plan(List<RouteCandidate> candidates) {
        RouteCandidate best = null;
        for (RouteCandidate candidate : candidates) {
            if (best == null || candidate.travelTime < best.travelTime) {
                best = candidate;
            }
        }
        PlanningResult result = new PlanningResult();
        result.algorithm = "Dijkstra baseline";
        result.routeId = best == null ? null : best.routeId;
        result.score = best == null ? Double.POSITIVE_INFINITY : best.travelTime;
        result.explanation = "Baseline only minimizes travel time and ignores risk semantics.";
        return result;
    }
}
