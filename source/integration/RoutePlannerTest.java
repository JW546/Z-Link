package integration;

import planner.DijkstraPlanner;
import planner.PlanningContext;
import planner.PlanningResult;
import planner.RiskAwareRoutePlanner;
import planner.RouteCandidate;

import java.util.Arrays;
import java.util.List;

public class RoutePlannerTest {
    public static void main(String[] args) {
        testRejectsShortestRouteWhenRiskTooHigh();
        testSafetyPriorityOutweighsTime();
        testUrgentWithoutSafetyCanFavorTime();
        testExpiredMessageHasLowerInfluence();
        testRiskAwareDiffersFromDijkstraBaseline();

        System.out.println("[PASS] Route planner test passed.");
    }

    private static void testRejectsShortestRouteWhenRiskTooHigh() {
        List<RouteCandidate> candidates = candidates(1.0);
        PlanningResult result = new RiskAwareRoutePlanner().plan(candidates, PlanningContext.safetyFirst());
        require(!"route_A_main_road".equals(result.routeId), "high-risk shortest route is rejected");
        require(result.rejectedShortestRoute, "planner records shortest-route rejection");
    }

    private static void testSafetyPriorityOutweighsTime() {
        PlanningResult result = new RiskAwareRoutePlanner().plan(candidates(1.0), PlanningContext.safetyFirst());
        require("route_B_school_zone_slow_safe".equals(result.routeId), "safety-first intent gives safety higher weight than time");
    }

    private static void testUrgentWithoutSafetyCanFavorTime() {
        PlanningContext context = PlanningContext.fastest();
        context.safetyRiskThreshold = 0.95;
        PlanningResult result = new RiskAwareRoutePlanner().plan(candidates(1.0), context);
        require("route_D_emergency_green_wave".equals(result.routeId) || "route_A_main_road".equals(result.routeId),
            "urgent intent without safety constraint can favor faster route");
    }

    private static void testExpiredMessageHasLowerInfluence() {
        List<RouteCandidate> staleEvidence = candidates(0.10);
        PlanningResult result = new RiskAwareRoutePlanner().plan(staleEvidence, PlanningContext.safetyFirst());
        require(!"route_A_main_road".equals(result.routeId), "expired or stale DDS evidence has reduced influence");
    }

    private static void testRiskAwareDiffersFromDijkstraBaseline() {
        List<RouteCandidate> candidates = candidates(1.0);
        PlanningResult baseline = new DijkstraPlanner().plan(candidates);
        PlanningResult riskAware = new RiskAwareRoutePlanner().plan(candidates, PlanningContext.safetyFirst());
        require("route_A_main_road".equals(baseline.routeId), "Dijkstra baseline chooses shortest route");
        require(!baseline.routeId.equals(riskAware.routeId), "RiskAwareRoutePlanner contrasts with Dijkstra baseline");
    }

    private static List<RouteCandidate> candidates(double freshness) {
        return Arrays.asList(
            new RouteCandidate("route_A_main_road", 10.0, 0.86, 0.80, 0.10, 0.86, 0.70, freshness, 0.20, 40.0, 0.05),
            new RouteCandidate("route_B_school_zone_slow_safe", 16.0, 0.22, 0.28, 0.00, 0.82, 0.88, 0.95, 0.05, 35.0, 0.02),
            new RouteCandidate("route_D_emergency_green_wave", 11.5, 0.42, 0.24, 0.90, 0.90, 0.92, 0.90, 0.10, 20.0, 0.01)
        );
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("[FAIL] " + message);
        }
        System.out.println("[PASS] " + message);
    }
}
