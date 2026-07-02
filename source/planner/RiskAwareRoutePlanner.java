package planner;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RiskAwareRoutePlanner {
    public PlanningResult plan(List<RouteCandidate> candidates, PlanningContext context) {
        PlanningContext ctx = context == null ? new PlanningContext() : context;
        RouteCandidate shortest = shortest(candidates);
        RouteCandidate best = null;
        double bestScore = Double.POSITIVE_INFINITY;
        PlanningResult result = new PlanningResult();
        result.algorithm = "RiskAwareRoutePlanner";

        for (RouteCandidate candidate : candidates) {
            boolean rejected = candidate.safetyRisk > ctx.safetyRiskThreshold && !ctx.emergencyOverrideAllowed;
            double score = score(candidate, ctx);
            Map<String, Object> item = new LinkedHashMap<String, Object>(candidate.toMap());
            item.put("score", score);
            item.put("rejectedBySafetyConstraint", rejected);
            result.candidateScores.add(item);
            if (rejected) {
                continue;
            }
            if (score < bestScore) {
                best = candidate;
                bestScore = score;
            }
        }

        result.routeId = best == null ? null : best.routeId;
        result.score = bestScore;
        result.rejectedShortestRoute = shortest != null && best != null && !shortest.routeId.equals(best.routeId);
        result.explanation = "score = time + risk + congestion + conflict + DDS penalties - confidence/trust/freshness rewards; safety risk is a hard constraint.";
        return result;
    }

    protected double score(RouteCandidate candidate, PlanningContext ctx) {
        double timeWeight = ctx.passengerUrgent && !ctx.passengerSafetyFirst ? 1.25 : 0.78;
        double riskWeight = ctx.passengerSafetyFirst ? 82.0 : 48.0;
        double congestionWeight = ctx.passengerSafetyFirst ? 18.0 : 12.0;
        double emergencyReward = ctx.passengerUrgent ? candidate.emergencyLevel * ctx.passengerPriority * 8.0 : 0.0;
        double ddsPenalty = candidate.ddsDelayMs / 120.0 + candidate.packetLossRate * 16.0;
        return candidate.travelTime * timeWeight
            + candidate.safetyRisk * riskWeight
            + candidate.congestionLevel * congestionWeight
            + candidate.conflictPenalty * 20.0
            + ddsPenalty
            - candidate.messageConfidence * 8.0
            - candidate.sourceTrust * 7.0
            - candidate.messageFreshness * 9.0
            - emergencyReward;
    }

    private RouteCandidate shortest(List<RouteCandidate> candidates) {
        RouteCandidate best = null;
        if (candidates == null) return null;
        for (RouteCandidate candidate : candidates) {
            if (best == null || candidate.travelTime < best.travelTime) {
                best = candidate;
            }
        }
        return best;
    }
}
