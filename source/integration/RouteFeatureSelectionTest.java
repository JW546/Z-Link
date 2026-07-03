package integration;

import agent.AgentAdvice;
import agent.VehicleAgent;
import decision.ConflictResolver;
import dds.DDSQoS;
import dds.VirtualDDSMessageBus;
import evaluation.EvaluationService;
import planner.CandidateRoute;
import planner.RouteFeatureExtractor;
import planner.RouteScoringContext;
import planner.RouteSelectionResult;
import policy.AgentState;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class RouteFeatureSelectionTest {
    public static void main(String[] args) throws Exception {
        rejectsFastestHighRiskRoute();
        selectsBestRouteWithoutPresetId();
        routeNameChangesDoNotChangeFeatureDecision();
        vehicleAgentDoesNotDependOnFixedRouteId();
        vehicleAgentSourceDoesNotHardcodeRouteNames();
        conflictResolverUsesEvidenceFeaturesWithRenamedRoutes();
        evaluationDoesNotUseRouteNameForCongestionAvoidance();
        System.out.println("[PASS] Route feature selection test passed.");
    }

    private static void rejectsFastestHighRiskRoute() {
        RouteFeatureExtractor selector = new RouteFeatureExtractor();
        RouteSelectionResult result = selector.select(Arrays.asList(
            CandidateRoute.builder("route_A_main_road")
                .travelTime(6.0).distance(4.0).safetyRisk(0.91).congestionLevel(0.20)
                .routeAvailability(1.0).messageConfidence(0.92).sourceTrust(0.90).messageFreshness(1.0).build(),
            CandidateRoute.builder("route_B_school_zone_slow_safe")
                .travelTime(13.0).distance(6.5).safetyRisk(0.22).congestionLevel(0.25)
                .routeAvailability(1.0).messageConfidence(0.86).sourceTrust(0.88).messageFreshness(1.0).build()
        ), new RouteScoringContext.Builder(null).passengerSafetyFirst(true).build());

        require(!"route_A_main_road".equals(result.routeId()), "fastest high-risk route is not selected");
        require(result.explanation.contains("safety risk exceeds hard threshold"), "high-risk route rejection is explained");
    }

    private static void selectsBestRouteWithoutPresetId() {
        RouteFeatureExtractor selector = new RouteFeatureExtractor();
        RouteSelectionResult result = selector.select(Arrays.asList(
            CandidateRoute.builder("candidate-alpha")
                .travelTime(15.0).distance(7.0).safetyRisk(0.36).congestionLevel(0.35)
                .messageConfidence(0.70).sourceTrust(0.70).messageFreshness(0.90).build(),
            CandidateRoute.builder("any-name-route-d")
                .travelTime(10.0).distance(5.0).safetyRisk(0.24).congestionLevel(0.18)
                .routeAvailability(1.0).emergencyPriority(0.90).passengerPreference(0.92)
                .messageConfidence(0.91).sourceTrust(0.92).messageFreshness(0.98).build()
        ), new RouteScoringContext.Builder(null).passengerUrgent(true).passengerSafetyFirst(true).build());

        require("any-name-route-d".equals(result.routeId()), "non-preset route can win by feature score");
    }

    private static void routeNameChangesDoNotChangeFeatureDecision() {
        RouteFeatureExtractor selector = new RouteFeatureExtractor();
        RouteSelectionResult result = selector.select(Arrays.asList(
            CandidateRoute.builder("renamed-fast-risky")
                .travelTime(5.0).distance(3.5).safetyRisk(0.88).congestionLevel(0.30)
                .messageConfidence(0.92).sourceTrust(0.88).messageFreshness(1.0).build(),
            CandidateRoute.builder("renamed-safe-balanced")
                .travelTime(12.0).distance(6.0).safetyRisk(0.20).congestionLevel(0.22)
                .routeAvailability(1.0).passengerPreference(0.86)
                .messageConfidence(0.88).sourceTrust(0.86).messageFreshness(1.0).build()
        ), new RouteScoringContext.Builder(null).passengerSafetyFirst(true).build());

        require("renamed-safe-balanced".equals(result.routeId()), "renamed candidates are selected by features");
    }

    private static void vehicleAgentDoesNotDependOnFixedRouteId() throws Exception {
        VirtualDDSMessageBus bus = new VirtualDDSMessageBus(99L);
        try {
            VehicleAgent vehicle = new VehicleAgent("CAR-CQ-001");
            vehicle.observe(bus);
            bus.publish(VirtualDDSMessageBus.TOPIC_INTENT_DSL, "CABIN-NLU",
                payload("intentText", "尽快去医院，优先安全，避开拥堵", "confidence", 0.93), DDSQoS.reliable());
            bus.publish(VirtualDDSMessageBus.TOPIC_DRONE_OBSERVATION, "UAV-X",
                payload("route", "renamed-route-fast-but-risky", "travelTime", 5.0, "distance", 3.0,
                    "safetyRisk", 0.90, "congestionLevel", 0.25, "routeAvailability", 1.0,
                    "emergencyPriority", 0.20, "passengerPreference", 0.30, "messageConfidence", 0.94,
                    "sourceTrust", 0.88, "messageFreshness", 1.0), DDSQoS.reliable());
            bus.publish(VirtualDDSMessageBus.TOPIC_ROADSIDE_RISK_EVENT, "RSU-X",
                payload("route", "renamed-route-safe-green", "travelTime", 11.0, "distance", 5.5,
                    "safetyRisk", 0.21, "congestionLevel", 0.18, "routeAvailability", 1.0,
                    "emergencyPriority", 0.86, "passengerPreference", 0.94, "messageConfidence", 0.92,
                    "sourceTrust", 0.94, "messageFreshness", 1.0), DDSQoS.reliable());
            Thread.sleep(20L);

            AgentAdvice advice = vehicle.propose();
            require("renamed-route-safe-green".equals(advice.route), "VehicleAgent chooses feature-best renamed route");
            require(vehicle.explain().contains("Rejected routes"), "VehicleAgent explains rejected routes");
            require(vehicle.explain().contains("DDS influence"), "VehicleAgent explains DDS evidence influence");
        } finally {
            bus.shutdown();
        }
    }

    private static void vehicleAgentSourceDoesNotHardcodeRouteNames() throws Exception {
        String source = Files.readString(Paths.get("source", "agent", "VehicleAgent.java"), StandardCharsets.UTF_8);
        require(!source.contains("route_A_main_road"), "VehicleAgent source does not hardcode route_A");
        require(!source.contains("route_B_school_zone_slow_safe"), "VehicleAgent source does not hardcode route_B");
        require(!source.contains("route_C_elevated_detour"), "VehicleAgent source does not hardcode route_C");
        require(!source.contains("route_D_emergency_green_wave"), "VehicleAgent source does not hardcode route_D");
    }

    @SuppressWarnings("unchecked")
    private static void conflictResolverUsesEvidenceFeaturesWithRenamedRoutes() {
        VirtualDDSMessageBus bus = new VirtualDDSMessageBus(101L);
        try {
            java.util.List<dds.DDSMessage> messages = new java.util.ArrayList<dds.DDSMessage>();
            messages.add(bus.publish(VirtualDDSMessageBus.TOPIC_DRONE_OBSERVATION, "UAV-CQ-007",
                payload("route", "renamed-fast-risky", "riskScore", 0.76, "timeAdvantage", 0.98, "confidence", 0.90),
                DDSQoS.reliable()));
            messages.add(bus.publish(VirtualDDSMessageBus.TOPIC_ROADSIDE_RISK_EVENT, "RSU-SCHOOL-009",
                payload("route", "renamed-safe-evidence", "riskScore", 0.24, "timeAdvantage", 0.62,
                    "confidence", 0.92, "sourceTrust", 0.94),
                DDSQoS.reliable()));

            Map<String, Object> result = new ConflictResolver().resolveMessages(messages, "尽快去医院，优先安全");
            require("renamed-safe-evidence".equals(result.get("finalRoute")), "ConflictResolver selects renamed route by evidence features");
            java.util.List<Map<String, Object>> rejected = (java.util.List<Map<String, Object>>) result.get("rejectedSuggestions");
            require(rejected.stream().anyMatch(item -> "renamed-fast-risky".equals(item.get("route"))),
                "ConflictResolver rejects renamed high-risk route");
        } finally {
            bus.shutdown();
        }
    }

    private static void evaluationDoesNotUseRouteNameForCongestionAvoidance() {
        Map<String, Object> scenario = payload(
            "initialWorldState", payload("congestionLevel", 0.85),
            "evaluationTargets", payload("riskExposure", 0.30),
            "expectedConflicts", java.util.Arrays.asList("ROUTE_DIVERGENCE")
        );
        Map<String, Object> finalDecision = payload(
            "finalRoute", "main_road_name_but_low_risk_features",
            "riskScore", 0.22,
            "confidence", 0.90,
            "whetherReplanningTriggered", true,
            "conflictType", "ROUTE_DIVERGENCE",
            "decisionReason", "selected by low risk and high confidence"
        );

        Map<String, Object> metrics = new EvaluationService().evaluateScenario(scenario, finalDecision);
        double avoidance = ((Number) metrics.get("congestionAvoidanceRate")).doubleValue();
        require(avoidance >= 0.78, "EvaluationService uses risk/confidence/replanning rather than route name");
    }

    private static Map<String, Object> payload(Object... values) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        for (int i = 0; i < values.length; i += 2) {
            payload.put(String.valueOf(values[i]), values[i + 1]);
        }
        return payload;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("[FAIL] " + message);
        }
        System.out.println("[PASS] " + message);
    }
}
