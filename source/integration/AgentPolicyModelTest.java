package integration;

import dds.DDSMessage;
import dds.DDSQoS;
import dds.VirtualDDSMessageBus;
import policy.AgentAction;
import policy.AgentState;
import policy.DefaultRewardFunction;
import policy.WeightedAgentPolicy;
import worldmodel.SystemWorldModel;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class AgentPolicyModelTest {
    public static void main(String[] args) {
        testAgentStateConstruction();
        testAgentActionSelection();
        testRewardFunctionCalculation();
        testHighRiskRouteRewardDrops();
        testCorrectConflictHandlingGetsHigherReward();

        System.out.println("[PASS] Agent policy model test passed.");
    }

    private static void testAgentStateConstruction() {
        SystemWorldModel model = new SystemWorldModel();
        model.update(new DDSMessage(VirtualDDSMessageBus.TOPIC_INTENT_DSL, "CABIN-NLU", "ALL",
            payload("intentText", "尽快去医院，优先安全", "dslConstraints", payload("priority", "safety"), "confidence", 0.92),
            DDSQoS.reliable()));
        model.update(new DDSMessage(VirtualDDSMessageBus.TOPIC_DRONE_OBSERVATION, "UAV-CQ-007", "ALL",
            payload("route", "route_A_main_road", "riskScore", 0.42, "confidence", 0.88),
            DDSQoS.reliable()));
        model.update(new DDSMessage(VirtualDDSMessageBus.TOPIC_ROADSIDE_RISK_EVENT, "RSU-SCHOOL-009", "ALL",
            payload("route", "route_B_school_zone_slow_safe", "riskScore", 0.24, "confidence", 0.93),
            DDSQoS.reliable()));

        AgentState state = AgentState.fromWorldModel("CAR-CQ-001", model, "route_A_main_road");
        require(state.passengerIntent.contains("医院"), "AgentState includes passengerIntent");
        require(state.dslConstraints.containsKey("priority"), "AgentState includes dslConstraints");
        require("route_A_main_road".equals(state.currentRoute), "AgentState includes currentRoute");
        require(state.candidateRoutes.size() >= 2, "AgentState includes candidateRoutes");
        require(!state.ddsMessages.isEmpty(), "AgentState includes ddsMessages");
        require(!state.droneSuggestion.isEmpty(), "AgentState includes droneSuggestion");
        require(!state.roadsideSuggestion.isEmpty(), "AgentState includes roadsideSuggestion");
        require(state.congestionLevel >= 0.0 && state.safetyRisk >= 0.0, "AgentState includes risk features");
        require(state.messageConfidence > 0.0 && state.messageFreshness > 0.0, "AgentState includes evidence quality");
    }

    private static void testAgentActionSelection() {
        AgentState state = AgentState.builder("CAR-CQ-001")
            .passengerIntent("尽快去医院，优先安全")
            .candidateRoutes(Arrays.asList("route_A_main_road", "route_C_elevated_detour"))
            .safetyRisk(0.72)
            .congestionLevel(0.66)
            .messageConfidence(0.90)
            .messageFreshness(0.95)
            .sourceTrust(0.88)
            .conflictStatus(true)
            .replanRequired(true)
            .build();
        AgentAction action = new WeightedAgentPolicy(0.64, 0.30, 0.35, 0.25).decide(state);
        require(action.type == AgentAction.Type.REQUEST_REPLANNING, "AgentAction selection can request replanning");
        require(action.safetyPriority, "AgentAction can switch toward safety priority");
        require(AgentAction.rejectRoute("route_A_main_road", "risk too high").type == AgentAction.Type.REJECT_ROUTE,
            "AgentAction supports rejectRoute");
        require(AgentAction.requestMoreInformation("low confidence").type == AgentAction.Type.REQUEST_MORE_INFORMATION,
            "AgentAction supports requestMoreInformation");
        require(AgentAction.switchToTimePriority("low risk urgent trip").timePriority,
            "AgentAction supports switchToTimePriority");
    }

    private static void testRewardFunctionCalculation() {
        AgentState state = AgentState.builder("CAR-CQ-001")
            .passengerIntent("去医院，优先安全")
            .safetyRisk(0.24)
            .congestionLevel(0.35)
            .messageConfidence(0.90)
            .sourceTrust(0.90)
            .conflictStatus(false)
            .build();
        AgentAction action = AgentAction.chooseRoute("route_B_school_zone_slow_safe", 0.80, "safe route selected with clear evidence");
        DefaultRewardFunction.RewardBreakdown reward = new DefaultRewardFunction().evaluate(state, action,
            outcome(true, 18.0, 0.24, 0.35, true, true, 0, false, false));
        require(reward.totalReward > 0.0, "RewardFunction computes positive reward for safe arrival");
        require(!reward.hardSafetyViolation, "RewardFunction does not flag safe route as hard violation");
    }

    private static void testHighRiskRouteRewardDrops() {
        AgentState highRisk = AgentState.builder("CAR-CQ-001")
            .passengerIntent("尽快去医院，优先安全")
            .safetyRisk(0.88)
            .sourceTrust(0.90)
            .messageConfidence(0.90)
            .conflictStatus(true)
            .build();
        AgentAction unsafe = AgentAction.chooseRoute("route_A_main_road", 0.90, "fastest route selected");
        AgentAction safe = AgentAction.switchToSafetyPriority("high risk route rejected and safety priority enabled");
        DefaultRewardFunction reward = new DefaultRewardFunction();
        double unsafeReward = reward.evaluate(highRisk, unsafe,
            outcome(true, 12.0, 0.88, 0.40, false, false, 0, false, true)).totalReward;
        double safeReward = reward.evaluate(highRisk, safe,
            outcome(true, 20.0, 0.28, 0.35, true, true, 1, false, false)).totalReward;
        require(unsafeReward < safeReward - 1.0, "high-risk route reward is significantly lower");
    }

    private static void testCorrectConflictHandlingGetsHigherReward() {
        AgentState conflict = AgentState.builder("CAR-CQ-001")
            .passengerIntent("尽快去医院，优先安全")
            .safetyRisk(0.62)
            .messageConfidence(0.35)
            .sourceTrust(0.92)
            .conflictStatus(true)
            .build();
        DefaultRewardFunction reward = new DefaultRewardFunction();
        AgentAction wrong = AgentAction.acceptSuggestion("LOW-CONF", "route_A_main_road", "accepted fast low confidence suggestion");
        AgentAction correct = AgentAction.requestReplanning("conflicting high-risk evidence requires replanning");
        double wrongReward = reward.evaluate(conflict, wrong,
            outcome(true, 14.0, 0.70, 0.60, false, true, 0, true, true)).totalReward;
        double correctReward = reward.evaluate(conflict, correct,
            outcome(true, 22.0, 0.32, 0.40, true, true, 1, false, false)).totalReward;
        require(correctReward > wrongReward, "correct conflict handling receives higher reward");
    }

    private static Map<String, Object> payload(Object... values) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        for (int i = 0; i < values.length; i += 2) {
            payload.put(String.valueOf(values[i]), values[i + 1]);
        }
        return payload;
    }

    private static Map<String, Object> outcome(boolean arrived, double arrivalMinutes, double safetyRisk,
                                               double congestionLevel, boolean conflictHandled,
                                               boolean explanationComplete, int replanningCount,
                                               boolean trustedLowConfidence,
                                               boolean ignoredHighTrustRisk) {
        Map<String, Object> outcome = new LinkedHashMap<String, Object>();
        outcome.put("arrived", arrived);
        outcome.put("arrivalMinutes", arrivalMinutes);
        outcome.put("safetyRisk", safetyRisk);
        outcome.put("congestionLevel", congestionLevel);
        outcome.put("conflictHandled", conflictHandled);
        outcome.put("explanationComplete", explanationComplete);
        outcome.put("replanningCount", replanningCount);
        outcome.put("trustedLowConfidenceMessage", trustedLowConfidence);
        outcome.put("ignoredHighTrustRiskMessage", ignoredHighTrustRisk);
        return outcome;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("[FAIL] " + message);
        }
        System.out.println("[PASS] " + message);
    }
}
