package collaboration.ros2;

import collaboration.runtime.CoordinatorAgent;
import collaboration.runtime.LocalDDSBroker;
import collaboration.runtime.RuntimeScenarioResult;

import java.util.LinkedHashMap;
import java.util.Map;

public class Ros2BidirectionalClosedLoopScenario {
    private final Ros2CommandExecutor executor;

    public Ros2BidirectionalClosedLoopScenario(Ros2CommandExecutor executor) {
        this.executor = executor;
    }

    public RuntimeScenarioResult run(long timeoutMs) throws Exception {
        LocalDDSBroker broker = new LocalDDSBroker(0);
        broker.start();
        Ros2DdsAdapter adapter = new Ros2DdsAdapter("ros2");
        Ros2InboundBridge inbound = new Ros2InboundBridge("127.0.0.1", broker.port(), adapter);
        Ros2LiveBridge outbound = new Ros2LiveBridge("127.0.0.1", broker.port(), adapter, executor);
        CoordinatorAgent coordinator = new CoordinatorAgent("127.0.0.1", broker.port(), 3, timeoutMs);
        Thread coordinatorThread = thread("ros2-bidirectional-coordinator", coordinator);

        try {
            outbound.start();
            inbound.startLocalOnly();
            coordinatorThread.start();
            Thread.sleep(180L);
            Ros2TopicBinding agentStatus = bindingFor(adapter, LocalDDSBroker.TOPIC_AGENT_STATUS);
            inbound.ingestData(agentStatus, uavPayload());
            inbound.ingestData(agentStatus, egoCarPayload());
            inbound.ingestData(agentStatus, peerCarPayload());
            coordinatorThread.join(Math.max(2_000L, timeoutMs + 500L));

            int localMessageCount = broker.history().size();
            outbound.awaitForwarded(localMessageCount, Math.max(4_000L, localMessageCount * 1_000L));
            return new RuntimeScenarioResult(broker.history(), coordinator.conflictEvent(), coordinator.finalDecision());
        } finally {
            coordinator.close();
            inbound.close();
            outbound.close();
            broker.close();
        }
    }

    private Ros2TopicBinding bindingFor(Ros2DdsAdapter adapter, String semanticTopic) {
        for (Ros2TopicBinding binding : adapter.topicBindings()) {
            if (semanticTopic.equals(binding.semanticTopic)) {
                return binding;
            }
        }
        throw new IllegalArgumentException("No ROS2 binding for " + semanticTopic);
    }

    private Thread thread(String name, Runnable runnable) {
        Thread thread = new Thread(runnable, name);
        thread.setDaemon(true);
        return thread;
    }

    private String uavPayload() {
        return pipe(new String[][] {
            {"topic", LocalDDSBroker.TOPIC_AGENT_STATUS},
            {"source", "UAV-ROS2-EXT-001"},
            {"logicalAgent", "UAV-CQ-007"},
            {"protocol", "DJI-STYLE"},
            {"role", "UAV"},
            {"route", "route_C_aerial_scout"},
            {"x", "13.0"},
            {"y", "9.0"},
            {"z", "80.0"},
            {"speed", "16.0"},
            {"battery", "77.0"},
            {"rateHz", "2.0"}
        });
    }

    private String egoCarPayload() {
        return pipe(new String[][] {
            {"topic", LocalDDSBroker.TOPIC_AGENT_STATUS},
            {"source", "CAR-ROS2-EXT-002"},
            {"logicalAgent", "CAR-CQ-001"},
            {"protocol", "ROS2-DDS"},
            {"role", "EGO_CAR"},
            {"route", "route_A_main_road"},
            {"x", "10.0"},
            {"y", "6.0"},
            {"z", "0.0"},
            {"speed", "8.5"},
            {"battery", "84.0"},
            {"rateHz", "2.0"}
        });
    }

    private String peerCarPayload() {
        return pipe(new String[][] {
            {"topic", LocalDDSBroker.TOPIC_AGENT_STATUS},
            {"source", "PEER-ROS2-EXT-003"},
            {"logicalAgent", "PEER-CAR-031"},
            {"protocol", "GENERIC-PRIVATE-JSON"},
            {"role", "PEER_CAR"},
            {"route", "route_A_main_road"},
            {"x", "11.0"},
            {"y", "7.0"},
            {"z", "0.0"},
            {"speed", "7.0"},
            {"battery", "91.0"},
            {"rateHz", "2.0"}
        });
    }

    private String pipe(String[][] pairs) {
        Map<String, String> ordered = new LinkedHashMap<String, String>();
        for (String[] pair : pairs) {
            ordered.put(pair[0], pair[1]);
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : ordered.entrySet()) {
            if (builder.length() > 0) builder.append("|");
            builder.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return builder.toString();
    }
}
