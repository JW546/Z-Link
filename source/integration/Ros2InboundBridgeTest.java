package integration;

import collaboration.ros2.Ros2DdsAdapter;
import collaboration.ros2.Ros2InboundBridge;
import collaboration.ros2.Ros2TopicBinding;
import collaboration.runtime.LocalDDSBroker;
import collaboration.runtime.LocalDDSClient;
import collaboration.runtime.RuntimeMessage;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Ros2InboundBridgeTest {
    public static void main(String[] args) throws Exception {
        testRos2StringPayloadIsRepublishedIntoLocalRuntime();
        testCustomRuntimeAgentStatusPayloadUsesProtocolAdapter();
        System.out.println("[PASS] ROS2 inbound bridge test passed.");
    }

    private static void testRos2StringPayloadIsRepublishedIntoLocalRuntime() throws Exception {
        LocalDDSBroker broker = new LocalDDSBroker(0);
        broker.start();
        List<RuntimeMessage> received = new ArrayList<RuntimeMessage>();
        LocalDDSClient subscriber = new LocalDDSClient("127.0.0.1", broker.port(), "LOCAL-TEST-SUBSCRIBER");
        Ros2InboundBridge bridge = new Ros2InboundBridge("127.0.0.1", broker.port(), new Ros2DdsAdapter("ros2"));
        try {
            subscriber.onMessage(received::add);
            subscriber.start();
            subscriber.subscribe(LocalDDSBroker.TOPIC_AGENT_STATUS);
            bridge.startLocalOnly();
            Thread.sleep(120L);

            Ros2TopicBinding binding = new Ros2TopicBinding(LocalDDSBroker.TOPIC_AGENT_STATUS, "/agent/status",
                "std_msgs/msg/String", "urban::RuntimeAgentStatus", "BEST_EFFORT_SENSOR_STREAM", "pub/sub",
                "Periodic UAV, ego-car, and peer-car state");
            boolean accepted = bridge.ingestData(binding,
                "topic=runtime/agent/status|source=UAV-REAL-001|agent=UAV-REAL-001|role=UAV|route=route_C_aerial_scout");

            require(accepted, "bridge accepts ROS2 String data payload");
            require(await(received, 1, 2_000L), "local DDS subscriber receives ROS2 inbound message");
            RuntimeMessage message = received.get(0);
            require(LocalDDSBroker.TOPIC_AGENT_STATUS.equals(message.topic), "ROS2 topic maps to local runtime topic");
            require("UAV-REAL-001".equals(message.sourceId), "external ROS2 source id is preserved");
            require("UAV-REAL-001".equals(message.payload.get("agentId")), "agent id is parsed into runtime payload");
            require("route_C_aerial_scout".equals(message.payload.get("route")), "route is parsed into runtime payload");
            require("ros2_inbound".equals(message.payload.get("adapter")), "payload records ROS2 inbound adapter");
        } finally {
            bridge.close();
            subscriber.close();
            broker.close();
        }
    }

    private static void testCustomRuntimeAgentStatusPayloadUsesProtocolAdapter() throws Exception {
        LocalDDSBroker broker = new LocalDDSBroker(0);
        broker.start();
        List<RuntimeMessage> received = new ArrayList<RuntimeMessage>();
        LocalDDSClient subscriber = new LocalDDSClient("127.0.0.1", broker.port(), "CUSTOM-INBOUND-SUBSCRIBER");
        Ros2InboundBridge bridge = new Ros2InboundBridge("127.0.0.1", broker.port(), new Ros2DdsAdapter("ros2", true));
        try {
            subscriber.onMessage(received::add);
            subscriber.start();
            subscriber.subscribe(LocalDDSBroker.TOPIC_AGENT_STATUS);
            subscriber.subscribe(LocalDDSBroker.TOPIC_PROTOCOL_FRAME);
            bridge.startLocalOnly();
            Thread.sleep(120L);

            Ros2TopicBinding binding = new Ros2TopicBinding(LocalDDSBroker.TOPIC_AGENT_STATUS, "/agent/status",
                "urban_runtime_msgs/msg/RuntimeAgentStatus", "urban::RuntimeAgentStatus",
                "BEST_EFFORT_SENSOR_STREAM", "pub/sub", "Periodic UAV, ego-car, and peer-car state");
            boolean accepted = bridge.ingestData(binding,
                "topic=runtime/agent/status|source=CAR-ROS2-EXT-002|logicalAgent=CAR-CQ-001|protocol=ROS2-DDS|role=EGO_CAR|route=route_A_main_road|x=10.0|y=6.0|z=0.0|speed=8.5|battery=84.0|rateHz=2.0|missionStatus=EXECUTING");

            require(accepted, "bridge accepts custom RuntimeAgentStatus payload");
            require(await(received, 2, 2_000L), "custom inbound publishes native frame and normalized status");
            RuntimeMessage status = messageFor(received, LocalDDSBroker.TOPIC_AGENT_STATUS);
            require(status != null, "custom inbound status is received");
            require("CAR-CQ-001".equals(status.sourceId), "custom inbound maps source to logical ego car");
            require("CAR-CQ-001".equals(status.payload.get("agentId")), "custom inbound payload carries logical agent id");
            require("CAR-ROS2-EXT-002".equals(status.payload.get("externalAgentId")), "custom inbound preserves external ROS2 source");
            require("ROS2-DDS".equals(status.payload.get("protocol")), "custom inbound preserves protocol for adapter evidence");
            require(protocolFrames(received).contains("ROS2-DDS"), "custom inbound records ROS2-DDS native frame");
        } finally {
            bridge.close();
            subscriber.close();
            broker.close();
        }
    }

    @SuppressWarnings("unchecked")
    private static Set<String> protocolFrames(List<RuntimeMessage> messages) {
        Set<String> protocols = new LinkedHashSet<String>();
        for (RuntimeMessage message : messages) {
            if (LocalDDSBroker.TOPIC_PROTOCOL_FRAME.equals(message.topic)) {
                protocols.add(String.valueOf(message.payload.get("protocol")));
            }
        }
        return protocols;
    }

    private static RuntimeMessage messageFor(List<RuntimeMessage> messages, String topic) {
        for (RuntimeMessage message : messages) {
            if (topic.equals(message.topic)) return message;
        }
        return null;
    }

    private static boolean await(List<RuntimeMessage> messages, int expected, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (messages.size() >= expected) return true;
            Thread.sleep(25L);
        }
        return messages.size() >= expected;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("[FAIL] " + message);
        }
        System.out.println("[PASS] " + message);
    }
}
