package integration;

import collaboration.runtime.RuntimeCoordinationPolicy;
import collaboration.runtime.RuntimeMessage;

import java.util.LinkedHashMap;
import java.util.Map;

public class RuntimeCoordinationPolicyTest {
    public static void main(String[] args) {
        RuntimeCoordinationPolicy policy = new RuntimeCoordinationPolicy();
        RuntimeCoordinationPolicy.RuntimeCoordinationDecision decision = policy.decide(
            status("CAR-CQ-001", "EGO_CAR", "route_A_main_road"),
            status("PEER-CAR-031", "PEER_CAR", "route_A_main_road"),
            status("UAV-CQ-007", "UAV", "route_C_aerial_scout"),
            status("UAV-AEP84-001", "UAV", "route_B_emergency_bypass")
        );

        require(decision.occupancyConflict, "policy detects route occupancy conflict");
        require(decision.conflictType().contains("ROUTE_OCCUPANCY_CONFLICT"), "policy labels occupancy conflict");
        require("route_D_emergency_green_wave".equals(decision.selectedRoute()),
            "policy selects resolver-backed replanning route");
        require("cooperative_score_policy".equals(decision.selectedStrategy()),
            "policy keeps stable public strategy name");
        require(decision.resolution.containsKey("priorityOrder"), "policy exposes resolver priority order");
        require(decision.explanation().contains("Accepted"), "policy exposes resolver explanation");
        require(priorityContains(decision, "UAV-AEP84-001"), "AEP84 status participates in runtime final route selection");

        RuntimeCoordinationPolicy.RuntimeCoordinationDecision aep84RiskDecision = policy.decide(
            status("CAR-CQ-001", "EGO_CAR", "route_A_main_road"),
            status("PEER-CAR-031", "PEER_CAR", "route_B_school_zone_slow_safe"),
            status("UAV-CQ-007", "UAV", "route_C_aerial_scout"),
            status("UAV-AEP84-001", "UAV", "route_A_main_road", 0.78)
        );
        require(aep84RiskDecision.replanningTriggered(), "high-risk AEP84 status triggers dynamic replanning");
        require(String.valueOf(aep84RiskDecision.resolution.get("conflictType")).contains("SAFETY_RISK_CONFLICT"),
            "AEP84 high risk is reflected as safety risk conflict");

        System.out.println("[PASS] Runtime coordination policy test passed.");
    }

    private static RuntimeMessage status(String sourceId, String role, String route) {
        return status(sourceId, role, route, 0.28);
    }

    private static RuntimeMessage status(String sourceId, String role, String route, double riskScore) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("agentId", sourceId);
        payload.put("role", role);
        payload.put("route", route);
        payload.put("confidence", 0.88);
        payload.put("riskScore", riskScore);
        return RuntimeMessage.publish("runtime/agent/status", sourceId, payload);
    }

    private static boolean priorityContains(RuntimeCoordinationPolicy.RuntimeCoordinationDecision decision, String sourceId) {
        Object priority = decision.resolution.get("priorityOrder");
        if (!(priority instanceof java.util.List<?>)) return false;
        for (Object item : (java.util.List<?>) priority) {
            if (item instanceof Map<?, ?> && sourceId.equals(String.valueOf(((Map<?, ?>) item).get("sourceId")))) {
                return true;
            }
        }
        return false;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("[FAIL] " + message);
        }
        System.out.println("[PASS] " + message);
    }
}
