package integration;

import agent.AgentAdvice;
import agent.AgentRuntime;
import agent.AutonomousAgent;
import agent.MessageAssessment;
import dds.DDSMessage;
import dds.DDSQoS;
import dds.VirtualDDSMessageBus;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AgentRuntimePolicyTest {
    public static void main(String[] args) throws Exception {
        VirtualDDSMessageBus bus = new VirtualDDSMessageBus(23L);
        try {
            AgentRuntime runtime = new AgentRuntime();
            for (AutonomousAgent agent : runtime.agents()) {
                agent.observe(bus);
            }

            Map<String, Object> intent = payload("intentText", "尽快去医院，优先安全，避开拥堵", "confidence", 0.93);
            DDSMessage intentMessage = bus.publish(VirtualDDSMessageBus.TOPIC_INTENT_DSL, "CABIN-NLU",
                intent, DDSQoS.reliable());
            DDSMessage riskMessage = bus.publish(VirtualDDSMessageBus.TOPIC_ROADSIDE_RISK_EVENT, "RSU-SCHOOL-009",
                payload("road_event", "main_road_accident_and_congestion", "riskScore", 0.72,
                    "congestionLevel", 0.86, "route", "route_A_main_road", "confidence", 0.91),
                DDSQoS.reliable());

            Thread.sleep(20L);
            require(runtime.agents().get(0).observedCount() > 0, "Agent subscribes to DDS messages");

            Set<String> routes = new HashSet<String>();
            for (AutonomousAgent agent : runtime.agents()) {
                AgentAdvice advice = agent.propose();
                routes.add(advice.route);
                require(advice.route != null && !advice.route.isBlank(), agent.agentId() + " generates advice");
                require(agent.explain().contains("weighted policy"), agent.agentId() + " outputs explanation");
                MessageAssessment assessment = agent.evaluate(riskMessage);
                require(assessment.valueScore > 0.0, agent.agentId() + " evaluates message value");
                agent.act(bus);
            }

            require(routes.size() >= 2, "multiple Agents produce different suggestions in one scene");
            require(bus.history().size() >= runtime.agents().size() + 2, "Agents publish advice or replanning requests");
            require(!runtime.agents().get(0).explain().contains("if"), "Agent policy is explainable weighted scoring, not fixed if-else script");
            require(intentMessage.confidence > 0.90, "intent message confidence is available to Agents");
        } finally {
            bus.shutdown();
        }

        System.out.println("[PASS] Agent runtime policy test passed.");
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
