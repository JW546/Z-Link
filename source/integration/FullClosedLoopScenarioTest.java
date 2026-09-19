package integration;

import collaboration.runtime.FullClosedLoopScenario;
import collaboration.runtime.LocalDDSBroker;
import collaboration.runtime.RuntimeScenarioResult;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class FullClosedLoopScenarioTest {
    public static void main(String[] args) throws Exception {
        RuntimeScenarioResult result = new FullClosedLoopScenario().run(5_000L);

        require(result.finalDecision != null, "closed loop produces a final route decision");
        require(result.conflictEvent != null, "closed loop detects route conflict from external device statuses");
        require("route_D_emergency_green_wave".equals(result.finalDecision.payload.get("selectedRoute")),
            "closed loop triggers dynamic replanning route");
        require("cooperative_score_policy".equals(result.finalDecision.payload.get("selectedStrategy")),
            "closed loop uses explainable cooperative strategy");
        require(hasTopic(result, LocalDDSBroker.TOPIC_REPLAN_REQUEST), "closed loop publishes replanning request");
        require(hasTopic(result, LocalDDSBroker.TOPIC_POLICY_EVALUATION), "closed loop publishes policy evaluation");

        Set<String> statusSources = sourcesFor(result, LocalDDSBroker.TOPIC_AGENT_STATUS);
        require(statusSources.contains("UAV-CQ-007"), "external DJI UAV status maps to logical UAV agent");
        require(statusSources.contains("CAR-CQ-001"), "external ROS2 vehicle status maps to logical ego car agent");
        require(statusSources.contains("PEER-CAR-031"), "external private JSON status maps to logical peer car agent");
        require(statusSources.contains("UAV-AEP84-001"), "external AEP84 UAV status maps to logical AEP84 UAV agent");

        Set<String> protocols = protocolsFor(result);
        require(protocols.contains("DJI-STYLE"), "closed loop records DJI native protocol frame");
        require(protocols.contains("ROS2-DDS"), "closed loop records ROS2-DDS native protocol frame");
        require(protocols.contains("GENERIC-PRIVATE-JSON"), "closed loop records generic private JSON native protocol frame");
        require(protocols.contains("AEP84-STANAG4586"), "closed loop records AEP84/STANAG4586 native protocol frame");

        System.out.println("[PASS] Full closed-loop scenario test passed.");
    }

    @SuppressWarnings("unchecked")
    private static Set<String> protocolsFor(RuntimeScenarioResult result) {
        Set<String> protocols = new LinkedHashSet<String>();
        for (Map<String, Object> item : result.history) {
            if (LocalDDSBroker.TOPIC_PROTOCOL_FRAME.equals(item.get("topic"))) {
                Object payload = item.get("payload");
                if (payload instanceof Map<?, ?>) {
                    protocols.add(String.valueOf(((Map<String, Object>) payload).get("protocol")));
                }
            }
        }
        return protocols;
    }

    private static Set<String> sourcesFor(RuntimeScenarioResult result, String topic) {
        Set<String> sources = new LinkedHashSet<String>();
        for (Map<String, Object> item : result.history) {
            if (topic.equals(item.get("topic"))) {
                sources.add(String.valueOf(item.get("sourceId")));
            }
        }
        return sources;
    }

    private static boolean hasTopic(RuntimeScenarioResult result, String topic) {
        for (Map<String, Object> item : result.history) {
            if (topic.equals(item.get("topic"))) {
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
