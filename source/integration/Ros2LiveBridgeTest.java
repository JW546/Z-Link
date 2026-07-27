package integration;

import collaboration.ros2.Ros2CommandExecutor;
import collaboration.ros2.Ros2DdsAdapter;
import collaboration.ros2.Ros2LiveBridge;
import collaboration.runtime.LocalDDSBroker;
import collaboration.runtime.LocalDDSClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Ros2LiveBridgeTest {
    public static void main(String[] args) throws Exception {
        testLocalRuntimeMessagesAreForwardedToRos2Commands();
        System.out.println("[PASS] ROS2 live bridge test passed.");
    }

    private static void testLocalRuntimeMessagesAreForwardedToRos2Commands() throws Exception {
        LocalDDSBroker broker = new LocalDDSBroker(0);
        broker.start();
        CapturingExecutor executor = new CapturingExecutor();
        Ros2LiveBridge bridge = new Ros2LiveBridge("127.0.0.1", broker.port(), new Ros2DdsAdapter("ros2"), executor);
        LocalDDSClient publisher = new LocalDDSClient("127.0.0.1", broker.port(), "TEST-AGENT");
        try {
            bridge.start();
            publisher.start();
            Thread.sleep(100L);

            Map<String, Object> status = new LinkedHashMap<String, Object>();
            status.put("agentId", "CAR-CQ-001");
            status.put("role", "EGO_CAR");
            status.put("route", "route_A_main_road");
            publisher.publish(LocalDDSBroker.TOPIC_AGENT_STATUS, status);

            Map<String, Object> decision = new LinkedHashMap<String, Object>();
            decision.put("selectedStrategy", "cooperative_score_policy");
            decision.put("selectedRoute", "route_D_emergency_green_wave");
            publisher.publish(LocalDDSBroker.TOPIC_FINAL_DECISION, decision);

            require(bridge.awaitForwarded(2, 2_000L), "bridge forwards live local DDS messages");
            require(executor.containsTopic("/agent/status"), "bridge publishes agent status to ROS2 topic");
            require(executor.containsTopic("/coordination/final_decision"), "bridge publishes final decision to ROS2 topic");
            require(executor.containsArgument("--wait-matching-subscriptions"), "bridge does not block waiting for ROS2 subscribers");
            require(executor.containsArgument("--keep-alive"), "bridge keeps short-lived ROS2 publishers alive for discovery");
            require(executor.containsPayload("cooperative_score_policy"), "bridge preserves decision strategy in ROS2 payload");
        } finally {
            publisher.close();
            bridge.close();
            broker.close();
        }
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

        boolean containsArgument(String argument) {
            for (List<String> command : commands) {
                if (command.contains(argument)) return true;
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
