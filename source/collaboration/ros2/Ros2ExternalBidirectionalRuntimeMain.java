package collaboration.ros2;

import collaboration.runtime.CoordinatorAgent;
import collaboration.runtime.LocalDDSBroker;

public class Ros2ExternalBidirectionalRuntimeMain {
    public static void main(String[] args) throws Exception {
        long timeoutMs = args.length > 0 ? Long.parseLong(args[0]) : 120_000L;
        String mode = args.length > 1 ? args[1] : "string";
        if ("custom".equalsIgnoreCase(mode)) {
            System.setProperty("ROS2_MESSAGE_MODE", "custom");
        }

        LocalDDSBroker broker = new LocalDDSBroker(0);
        broker.start();
        Ros2DdsAdapter adapter = new Ros2DdsAdapter();
        Ros2InboundBridge inbound = new Ros2InboundBridge("127.0.0.1", broker.port(), adapter);
        Ros2LiveBridge outbound = new Ros2LiveBridge("127.0.0.1", broker.port(), adapter);
        CoordinatorAgent coordinator = new CoordinatorAgent("127.0.0.1", broker.port(), 3, timeoutMs);
        Thread coordinatorThread = thread("ros2-external-coordinator", coordinator);

        try {
            outbound.start();
            inbound.start();
            coordinatorThread.start();
            System.out.println("[ROS2-EXTERNAL-BIDIR] brokerPort=" + broker.port());
            System.out.println("[ROS2-EXTERNAL-BIDIR] waiting for external ROS2 /agent/status messages");
            System.out.println("[ROS2-EXTERNAL-BIDIR] publish with tools\\ros2_publish_external_fleet_status.ps1");
            coordinatorThread.join(timeoutMs + 2_000L);
            int localMessageCount = broker.history().size();
            outbound.awaitForwarded(localMessageCount, Math.max(4_000L, localMessageCount * 1_000L));
            System.out.println("[ROS2-EXTERNAL-BIDIR] localMessages=" + localMessageCount);
            System.out.println("[ROS2-EXTERNAL-BIDIR] forwardedToRos2=" + outbound.forwardedCount());
            System.out.println("[ROS2-EXTERNAL-BIDIR] finalDecision="
                + (coordinator.finalDecision() == null ? "{}" : coordinator.finalDecision().payload));
            if (coordinator.finalDecision() == null) {
                throw new IllegalStateException("No final decision was produced from external ROS2 messages.");
            }
        } finally {
            coordinator.close();
            inbound.close();
            outbound.close();
            broker.close();
        }
    }

    private static Thread thread(String name, Runnable runnable) {
        Thread thread = new Thread(runnable, name);
        thread.setDaemon(true);
        return thread;
    }
}
