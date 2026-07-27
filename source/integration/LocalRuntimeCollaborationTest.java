package integration;

import collaboration.runtime.LocalDDSBroker;
import collaboration.runtime.LocalRuntimeScenario;
import collaboration.runtime.RuntimeScenarioResult;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class LocalRuntimeCollaborationTest {
    public static void main(String[] args) throws Exception {
        RuntimeScenarioResult result = new LocalRuntimeScenario().run(80L, 4);

        require(result.history.size() >= 14, "local DDS broker records multi-agent socket messages");
        require(result.conflictEvent != null, "coordinator publishes conflict event");
        require(result.finalDecision != null, "coordinator publishes final decision");
        require("cooperative_score_policy".equals(result.finalDecision.payload.get("selectedStrategy")),
            "final decision uses cooperative score policy instead of only baselines");
        require("route_D_emergency_green_wave".equals(result.finalDecision.payload.get("selectedRoute")),
            "conflict triggers dynamic replanning route");

        Set<String> statusAgents = sourcesFor(result, LocalDDSBroker.TOPIC_AGENT_STATUS);
        require(statusAgents.contains("UAV-CQ-007"), "UAV process publishes status");
        require(statusAgents.contains("CAR-CQ-001"), "ego car process publishes status");
        require(statusAgents.contains("PEER-CAR-031"), "peer car process publishes status");

        Set<String> protocolAgents = sourcesFor(result, LocalDDSBroker.TOPIC_PROTOCOL_FRAME);
        require(protocolAgents.size() == 3, "three device protocol adapters publish native frames");
        require(hasTopic(result, LocalDDSBroker.TOPIC_REPLAN_REQUEST), "replanning request is published");
        require(hasTopic(result, LocalDDSBroker.TOPIC_POLICY_EVALUATION), "policy evaluation report is published");

        System.out.println("[PASS] Local runtime collaboration test passed.");
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
