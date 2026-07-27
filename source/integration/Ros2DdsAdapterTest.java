package integration;

import collaboration.ros2.Ros2BridgePlan;
import collaboration.ros2.Ros2DdsAdapter;
import collaboration.runtime.LocalDDSBroker;
import collaboration.runtime.LocalRuntimeScenario;
import collaboration.runtime.RuntimeScenarioResult;

import java.util.Map;

public class Ros2DdsAdapterTest {
    public static void main(String[] args) throws Exception {
        testTopicBindingsAreComplete();
        testRuntimeHistoryCanBeMappedToRos2PublishCommands();
        testCustomRos2MessagesCanBeSelected();
        System.out.println("[PASS] ROS2 DDS adapter test passed.");
    }

    private static void testTopicBindingsAreComplete() {
        Ros2DdsAdapter adapter = new Ros2DdsAdapter("");
        Ros2BridgePlan plan = adapter.planBridge(null);
        require(!plan.ros2Available, "adapter supports dry-run when ROS2 is not installed");
        require(plan.topicBindings.size() >= 7, "adapter exposes runtime Topic to ROS2 bindings");
        require(hasBinding(plan, LocalDDSBroker.TOPIC_AGENT_STATUS, "/agent/status", "urban::RuntimeAgentStatus"),
            "agent status is bound to ROS2 topic and IDL type");
        require(hasBinding(plan, LocalDDSBroker.TOPIC_FINAL_DECISION, "/coordination/final_decision", "urban::RouteDecision"),
            "final decision is bound to ROS2 topic and IDL type");
    }

    private static void testRuntimeHistoryCanBeMappedToRos2PublishCommands() throws Exception {
        RuntimeScenarioResult runtime = new LocalRuntimeScenario().run(80L, 4);
        Ros2DdsAdapter adapter = new Ros2DdsAdapter("ros2");
        Ros2BridgePlan plan = adapter.planBridge(runtime.history);

        require(plan.publishCommands.size() == runtime.history.size(), "every runtime message gets a ROS2 publish command");
        require(hasCommand(plan, "/agent/status", "std_msgs/msg/String"), "status messages map to ROS2 String publish commands");
        require(hasCommand(plan, "/protocol/native_frame", "std_msgs/msg/String"), "native frames map to ROS2 publish commands");
        require(hasCommand(plan, "/coordination/replan_request", "std_msgs/msg/String"), "replanning request maps to ROS2 publish command");
        require(hasCommand(plan, "/coordination/final_decision", "std_msgs/msg/String"), "final decision maps to ROS2 publish command");
    }

    private static void testCustomRos2MessagesCanBeSelected() throws Exception {
        RuntimeScenarioResult runtime = new LocalRuntimeScenario().run(80L, 4);
        Ros2DdsAdapter adapter = new Ros2DdsAdapter("ros2", true);
        Ros2BridgePlan plan = adapter.planBridge(runtime.history);

        require(hasCommand(plan, "/agent/status", "urban_runtime_msgs/msg/RuntimeAgentStatus"),
            "custom mode maps status to RuntimeAgentStatus message");
        require(hasCommand(plan, "/coordination/final_decision", "urban_runtime_msgs/msg/RouteDecision"),
            "custom mode maps final decision to RouteDecision message");
        require(hasCommandPayload(plan, "/coordination/final_decision", "selected_route"),
            "custom final decision command uses structured fields");
        require(hasMode(plan, "custom_ros2_messages"), "custom commands declare custom ROS2 message mode");
    }

    private static boolean hasBinding(Ros2BridgePlan plan, String semanticTopic, String rosTopic, String idlType) {
        for (Map<String, Object> binding : plan.topicBindings) {
            if (semanticTopic.equals(binding.get("semanticTopic"))
                && rosTopic.equals(binding.get("rosTopic"))
                && idlType.equals(binding.get("idlType"))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasCommand(Ros2BridgePlan plan, String rosTopic, String messageType) {
        for (Map<String, Object> command : plan.publishCommands) {
            if (rosTopic.equals(command.get("rosTopic")) && messageType.equals(command.get("messageType"))) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static boolean hasCommandPayload(Ros2BridgePlan plan, String rosTopic, String payloadPart) {
        for (Map<String, Object> command : plan.publishCommands) {
            if (!rosTopic.equals(command.get("rosTopic"))) continue;
            Object raw = command.get("command");
            if (raw instanceof java.util.List<?>) {
                for (Object part : (java.util.List<Object>) raw) {
                    if (String.valueOf(part).contains(payloadPart)) return true;
                }
            }
        }
        return false;
    }

    private static boolean hasMode(Ros2BridgePlan plan, String mode) {
        for (Map<String, Object> command : plan.publishCommands) {
            if (mode.equals(command.get("messageMode"))) return true;
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
