package planner;

import agent.AdaptiveRoutePlanner;

/**
 * Planner seam. Dijkstra/Greedy remain baseline labels, not final intelligence.
 */
public class RoutePlanningService {
    private final AdaptiveRoutePlanner adaptivePlanner = new AdaptiveRoutePlanner();
    private final DijkstraPlanner dijkstraPlanner = new DijkstraPlanner();
    private final GreedyPlanner greedyPlanner = new GreedyPlanner();
    private final RiskAwareRoutePlanner riskAwareRoutePlanner = new RiskAwareRoutePlanner();
    private final MultiObjectiveRoutePlanner multiObjectiveRoutePlanner = new MultiObjectiveRoutePlanner();

    public AdaptiveRoutePlanner.RoutePlan planAdaptive(String destination, boolean urgent, boolean safetyFirst,
                                                       double congestionLevel) {
        AdaptiveRoutePlanner.RouteContext context = new AdaptiveRoutePlanner.RouteContext();
        context.destination = destination;
        context.urgent = urgent;
        context.safetyFirst = safetyFirst;
        context.congestionLevel = congestionLevel;
        context.ambulanceLaneClearance = urgent ? 0.88 : 0.62;
        return adaptivePlanner.plan(context);
    }

    public AdaptiveRoutePlanner.RoutePlan planDijkstraBaseline(String destination) {
        AdaptiveRoutePlanner.RoutePlan plan = planAdaptive(destination, false, false, 0.30);
        plan.algorithm = "Dijkstra baseline via zero-heuristic-compatible graph search";
        return plan;
    }

    public PlanningResult planDijkstraBaseline(java.util.List<RouteCandidate> candidates) {
        return dijkstraPlanner.plan(candidates);
    }

    public PlanningResult planGreedyBaseline(java.util.List<RouteCandidate> candidates) {
        return greedyPlanner.plan(candidates);
    }

    public PlanningResult planRiskAware(java.util.List<RouteCandidate> candidates, PlanningContext context) {
        return riskAwareRoutePlanner.plan(candidates, context);
    }

    public PlanningResult planMultiObjective(java.util.List<RouteCandidate> candidates, PlanningContext context) {
        return multiObjectiveRoutePlanner.plan(candidates, context);
    }
}
