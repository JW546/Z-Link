package planner;

import java.util.List;

public class MultiObjectiveRoutePlanner extends RiskAwareRoutePlanner {
    public PlanningResult plan(List<RouteCandidate> candidates, PlanningContext context) {
        PlanningResult result = super.plan(candidates, context);
        result.algorithm = "MultiObjectiveRoutePlanner";
        result.explanation = "Multi-objective route selection balances travelTime, safetyRisk, congestion, emergency priority, DDS evidence quality, and conflict penalties.";
        return result;
    }
}
