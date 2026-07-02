package integration;

import dds.DDSMessage;
import dds.DDSQoS;
import dds.VirtualDDSMessageBus;
import policy.AgentState;
import worldmodel.SystemWorldModel;

import java.util.LinkedHashMap;
import java.util.Map;

public class WorldModelTest {
    public static void main(String[] args) throws Exception {
        testNewDdsMessageUpdatesWorldModel();
        testExpiredOrOlderMessageDoesNotOverrideNewMessage();
        testHighTrustRiskOutranksLowTrustNormalAdvice();
        testWorldModelBuildsAgentState();

        System.out.println("[PASS] WorldModel test passed.");
    }

    private static void testNewDdsMessageUpdatesWorldModel() {
        SystemWorldModel model = new SystemWorldModel();
        DDSMessage message = new DDSMessage(VirtualDDSMessageBus.TOPIC_INTENT_DSL, "CABIN-NLU", "ALL",
            payload("intentText", "尽快去医院，优先安全，避开拥堵", "confidence", 0.92),
            DDSQoS.reliable());
        require(model.update(message), "DDS new message updates WorldModel");
        require(model.passengerIntent.contains("医院"), "WorldModel records passenger intent");
        require(model.timeline.size() == 1, "WorldModel records timeline");
    }

    private static void testExpiredOrOlderMessageDoesNotOverrideNewMessage() {
        SystemWorldModel model = new SystemWorldModel();
        long now = System.currentTimeMillis();
        DDSMessage fresh = new DDSMessage(VirtualDDSMessageBus.TOPIC_ROADSIDE_RISK_EVENT, "RSU-SCHOOL-009", "ALL",
            payload("riskScore", 0.75, "congestionLevel", 0.82, "confidence", 0.92),
            DDSQoS.reliable(), 0.92, now);
        DDSMessage older = new DDSMessage(VirtualDDSMessageBus.TOPIC_ROADSIDE_RISK_EVENT, "LOW-TRUST-SOURCE", "ALL",
            payload("riskScore", 0.10, "congestionLevel", 0.10, "confidence", 0.30),
            DDSQoS.reliable().withTimeToLiveMs(10L), 0.30, now - 1_000L);
        model.update(fresh);
        double riskAfterFresh = model.roadRisk;
        require(!model.update(older), "older expired message cannot override new message");
        require(model.roadRisk == riskAfterFresh, "WorldModel keeps newer risk state");
    }

    private static void testHighTrustRiskOutranksLowTrustNormalAdvice() {
        SystemWorldModel model = new SystemWorldModel();
        DDSMessage lowTrust = new DDSMessage(VirtualDDSMessageBus.TOPIC_ROUTE_SUGGESTION, "UNKNOWN-DEVICE", "ALL",
            payload("route", "route_A_main_road", "riskScore", 0.18, "confidence", 0.40),
            DDSQoS.reliable());
        DDSMessage highTrust = new DDSMessage(VirtualDDSMessageBus.TOPIC_ROADSIDE_RISK_EVENT, "RSU-SCHOOL-009", "ALL",
            payload("route", "route_A_main_road", "riskScore", 0.78, "congestionLevel", 0.84, "confidence", 0.93),
            DDSQoS.reliable());
        model.update(lowTrust);
        model.update(highTrust);
        require(model.roadRisk > 0.60, "high-trust risk message has higher priority than low-trust normal advice");
        require(model.replanRequired, "WorldModel requests replan for high risk");
    }

    private static void testWorldModelBuildsAgentState() {
        SystemWorldModel model = new SystemWorldModel();
        model.update(new DDSMessage(VirtualDDSMessageBus.TOPIC_INTENT_DSL, "CABIN-NLU", "ALL",
            payload("intentText", "尽快去医院，优先安全", "confidence", 0.90), DDSQoS.reliable()));
        model.update(new DDSMessage(VirtualDDSMessageBus.TOPIC_DRONE_OBSERVATION, "UAV-CQ-007", "ALL",
            payload("route", "route_C_elevated_detour", "riskScore", 0.42, "confidence", 0.88), DDSQoS.reliable()));
        AgentState state = model.toAgentState("UAV-CQ-007", "UAV");
        require(state.passengerSafetyFirst, "AgentState carries passenger safety priority");
        require(state.passengerUrgent, "AgentState carries passenger urgency");
        require(state.messageConfidence > 0.0, "AgentState carries message confidence");
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
