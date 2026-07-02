package planner;

import worldmodel.SystemWorldModel;

import java.util.List;

public class DynamicReplanningService {
    private final MultiObjectiveRoutePlanner planner = new MultiObjectiveRoutePlanner();

    public boolean shouldReplan(SystemWorldModel worldModel) {
        return worldModel != null && (worldModel.replanRequired || worldModel.hasConflict
            || worldModel.roadRisk >= 0.70 || worldModel.congestionLevel >= 0.82);
    }

    public PlanningResult replanIfNeeded(SystemWorldModel worldModel, List<RouteCandidate> candidates,
                                         PlanningContext context) {
        if (!shouldReplan(worldModel)) {
            PlanningResult hold = new PlanningResult();
            hold.algorithm = "DynamicReplanningService";
            hold.routeId = candidates == null || candidates.isEmpty() ? null : candidates.get(0).routeId;
            hold.explanation = "WorldModel does not require replanning.";
            return hold;
        }
        return planner.plan(candidates, context);
    }
}
