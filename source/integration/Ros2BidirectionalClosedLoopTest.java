package integration;

import collaboration.ros2.Ros2BidirectionalClosedLoopScenario;
import collaboration.ros2.Ros2CommandExecutor;
import collaboration.runtime.LocalDDSBroker;
import collaboration.runtime.RuntimeScenarioResult;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Ros2BidirectionalClosedLoopTest {
    public static void main(String[] args) throws Exception {
        CapturingExecutor executor = new CapturingExecutor();
        RuntimeScenarioResult result = new Ros2BidirectionalClosedLoopScenario(executor).run(5_000L);

        require(result.finalDecision != null, "ROS2 bidirectional loop produces final decision");
        require(result.conflictEvent != null, "ROS2 inbound statuses trigger conflict detection");
        require("route_D_emergency_green_wave".equals(result.finalDecision.payload.get("selectedRoute")),
            "ROS2 bidirectional loop triggers dynamic replanning");
        require(hasTopic(result, LocalDDSBroker.TOPIC_PROTOCOL_FRAME), "ROS2 inbound publishes adapter native frames");
        require(hasTopic(result, LocalDDSBroker.TOPIC_POLICY_EVALUATION), "ROS2 bidirectional loop publishes policy evaluation");
        require(executor.containsTopic("/agent/status"), "ROS2 outbound bridge republishes normalized statuses");
        require(executor.containsTopic("/coordination/final_decision"), "ROS2 outbound bridge publishes final decision");
        require(executor.containsPayload("cooperative_score_policy"), "ROS2 final decision preserves strategy");

        Set<String> sources = sourcesFor(result, LocalDDSBroker.TOPIC_AGENT_STATUS);
        require(sources.contains("UAV-CQ-007"), "ROS2 DJI payload maps to logical UAV");
        require(sources.contains("CAR-CQ-001"), "ROS2 vehicle payload maps to logical ego car");
        require(sources.contains("PEER-CAR-031"), "ROS2 private JSON payload maps to logical peer car");

        System.out.println("[PASS] ROS2 bidirectional closed-loop test passed.");
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

    private static class CapturingExecutor implements Ros2CommandExecutor {
        private final List<List<String>> commands = new ArrayList<List<String>>();

        public int execute(List<String> command) {
            commands.add(new ArrayList<String>(command));
            return 0;
        }

        boolean containsTopic(String topic) {
            for (List<String> command : commands) {
                if (command.contains(topic)) return true;
            }
            return false;
        }

        boolean containsPayload(String payloadPart) {
            for (List<String> command : commands) {
                for (String part : command) {
                    if (part.contains(payloadPart)) return true;
                }
            }
            return false;
        }
    }
}
