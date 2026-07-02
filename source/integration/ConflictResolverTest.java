package integration;

import dds.DDSMessage;
import dds.DDSQoS;
import dds.VirtualDDSMessageBus;
import decision.ConflictResolver;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConflictResolverTest {
    public static void main(String[] args) {
        testRouteDivergenceConflict();
        testIntentTensionConflict();
        testSafetyRiskConflictRejectsFastRoute();
        testEvidenceQualityConflict();
        testEndToEndPassengerSafetyScenario();

        System.out.println("[PASS] ConflictResolver test passed.");
    }

    private static void testRouteDivergenceConflict() {
        Map<String, Object> result = new ConflictResolver().resolveMessages(Arrays.asList(
            message(VirtualDDSMessageBus.TOPIC_DRONE_OBSERVATION, "UAV-CQ-007",
                payload("route", "route_A_main_road", "riskScore", 0.30, "timeAdvantage", 0.92, "confidence", 0.88)),
            message(VirtualDDSMessageBus.TOPIC_ROADSIDE_RISK_EVENT, "RSU-SCHOOL-009",
                payload("route", "route_B_school_zone_slow_safe", "riskScore", 0.24, "confidence", 0.90))
        ), "尽快去医院");

        require(Boolean.TRUE.equals(result.get("hasConflict")), "route divergence conflict is detected");
        require(String.valueOf(result.get("conflictType")).contains("ROUTE_DIVERGENCE"), "conflictType includes route divergence");
        require(result.get("acceptedSuggestion") instanceof Map<?, ?>, "acceptedSuggestion is returned");
        require(result.get("priorityOrder") instanceof List<?>, "priorityOrder is returned");
    }

    private static void testIntentTensionConflict() {
        Map<String, Object> result = new ConflictResolver().resolveMessages(Arrays.asList(
            message(VirtualDDSMessageBus.TOPIC_DRONE_OBSERVATION, "UAV-CQ-007",
                payload("route", "route_A_main_road", "riskScore", 0.36, "timeAdvantage", 0.95, "confidence", 0.88))
        ), "尽快去医院，优先安全");

        require(String.valueOf(result.get("conflictType")).contains("INTENT_TENSION"), "conflictType includes intent tension");
        require(String.valueOf(result.get("decisionReason")).contains("safety"), "decision reason explains safety priority");
    }

    private static void testSafetyRiskConflictRejectsFastRoute() {
        Map<String, Object> result = new ConflictResolver().resolveMessages(Arrays.asList(
            message(VirtualDDSMessageBus.TOPIC_DRONE_OBSERVATION, "UAV-CQ-007",
                payload("route", "route_A_main_road", "riskScore", 0.72, "timeAdvantage", 0.98, "confidence", 0.88)),
            message(VirtualDDSMessageBus.TOPIC_ROADSIDE_RISK_EVENT, "RSU-SCHOOL-009",
                payload("route", "route_B_school_zone_slow_safe", "riskScore", 0.26, "timeAdvantage", 0.45, "confidence", 0.92))
        ), "尽快去医院，优先安全");

        require(String.valueOf(result.get("conflictType")).contains("SAFETY_RISK_CONFLICT"), "conflictType includes safety risk");
        require("route_B_school_zone_slow_safe".equals(result.get("finalRoute")), "high-risk fastest route is rejected");
        require(rejectedContains(result, "route_A_main_road"), "rejectedSuggestions includes unsafe route");
        require(Boolean.TRUE.equals(result.get("whetherReplanningTriggered")), "replanning is triggered by safety risk");
    }

    private static void testEvidenceQualityConflict() {
        DDSMessage stale = new DDSMessage(VirtualDDSMessageBus.TOPIC_NEIGHBOR_STATUS, "PEER-CAR-031", "ALL",
            payload("route", "route_C_elevated_detour", "riskScore", 0.25, "sourceTrust", 0.30,
                "confidence", 0.30, "packetLossRate", 0.70),
            DDSQoS.bestEffort().withTimeToLiveMs(10L), 0.30, System.currentTimeMillis() - 200L);
        Map<String, Object> result = new ConflictResolver().resolveMessages(Arrays.asList(
            stale,
            message(VirtualDDSMessageBus.TOPIC_ROADSIDE_RISK_EVENT, "RSU-SCHOOL-009",
                payload("route", "route_B_school_zone_slow_safe", "riskScore", 0.28, "confidence", 0.90))
        ), "优先安全");

        require(String.valueOf(result.get("conflictType")).contains("EVIDENCE_QUALITY_CONFLICT"), "conflictType includes evidence quality");
        require(rejectedContains(result, "route_C_elevated_detour"), "low-quality stale message is rejected");
    }

    private static void testEndToEndPassengerSafetyScenario() {
        List<DDSMessage> messages = Arrays.asList(
            message(VirtualDDSMessageBus.TOPIC_DRONE_OBSERVATION, "UAV-CQ-007",
                payload("role", "UAV", "route", "route_A_main_road", "riskScore", 0.34,
                    "timeAdvantage", 0.98, "confidence", 0.89, "explanation", "无人机观测到路线A更快")),
            message(VirtualDDSMessageBus.TOPIC_ROADSIDE_RISK_EVENT, "RSU-SCHOOL-009",
                payload("role", "RSU", "route", "route_B_school_zone_slow_safe", "riskScore", 0.23,
                    "affectedRoute", "route_A_main_road", "riskRoute", "route_A_main_road",
                    "affectedRiskScore", 0.82,
                    "confidence", 0.93, "explanation", "路线A前方施工，建议路线B")),
            message(VirtualDDSMessageBus.TOPIC_NEIGHBOR_STATUS, "PEER-CAR-031",
                payload("role", "PEER_VEHICLE", "route", "route_C_elevated_detour", "riskScore", 0.33,
                    "confidence", 0.76, "explanation", "路线B有跟驰风险，建议路线C"))
        );
        Map<String, Object> result = new ConflictResolver().resolveMessages(messages, "尽快去医院，优先安全，避开拥堵");

        require(Boolean.TRUE.equals(result.get("hasConflict")), "E2E detects conflict");
        require(!"route_A_main_road".equals(result.get("finalRoute")), "E2E rejects high-risk fastest route");
        require(String.valueOf(result.get("decisionReason")).contains("Fastest routes are rejected"), "E2E explains why fastest route was not selected");
        require(result.get("timelineEvent") instanceof Map<?, ?>, "E2E outputs timeline event");
    }

    private static DDSMessage message(String topic, String sourceId, Map<String, Object> payload) {
        return new DDSMessage(topic, sourceId, "ALL", payload, DDSQoS.reliable());
    }

    private static Map<String, Object> payload(Object... values) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        for (int i = 0; i < values.length; i += 2) {
            payload.put(String.valueOf(values[i]), values[i + 1]);
        }
        return payload;
    }

    private static boolean rejectedContains(Map<String, Object> result, String route) {
        Object rejected = result.get("rejectedSuggestions");
        if (!(rejected instanceof List<?>)) return false;
        for (Object item : (List<?>) rejected) {
            if (item instanceof Map<?, ?> && route.equals(String.valueOf(((Map<?, ?>) item).get("route")))) {
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
