package collaboration.ros2;

import collaboration.runtime.CoordinatorAgent;
import collaboration.runtime.LocalDDSBroker;
import collaboration.runtime.RuntimeAgentRole;
import collaboration.runtime.RuntimeDeviceAgent;

import java.util.ArrayList;
import java.util.List;

public class Ros2LiveRuntimeMain {
    public static void main(String[] args) throws Exception {
        long intervalMs = args.length > 0 ? Long.parseLong(args[0]) : 500L;
        int ticks = args.length > 1 ? Integer.parseInt(args[1]) : 8;

        LocalDDSBroker broker = new LocalDDSBroker(0);
        broker.start();
        Ros2LiveBridge bridge = new Ros2LiveBridge("127.0.0.1", broker.port(), new Ros2DdsAdapter());
        bridge.start();

        CoordinatorAgent coordinator = new CoordinatorAgent("127.0.0.1", broker.port(), 3,
            Math.max(8_000L, intervalMs * ticks + 4_000L));

        List<Thread> threads = new ArrayList<Thread>();
        threads.add(thread("runtime-coordinator", coordinator));
        threads.add(thread("runtime-uav", new RuntimeDeviceAgent("127.0.0.1", broker.port(),
            RuntimeDeviceAgent.defaultConfig(RuntimeAgentRole.UAV, intervalMs, ticks))));
        threads.add(thread("runtime-ego-car", new RuntimeDeviceAgent("127.0.0.1", broker.port(),
            RuntimeDeviceAgent.defaultConfig(RuntimeAgentRole.EGO_CAR, intervalMs, ticks))));
        threads.add(thread("runtime-peer-car", new RuntimeDeviceAgent("127.0.0.1", broker.port(),
            RuntimeDeviceAgent.defaultConfig(RuntimeAgentRole.PEER_CAR, intervalMs, ticks))));

        try {
            for (Thread thread : threads) thread.start();
            for (Thread thread : threads) thread.join(Math.max(10_000L, intervalMs * ticks + 8_000L));
            int localMessageCount = broker.history().size();
            bridge.awaitForwarded(localMessageCount, Math.max(10_000L, localMessageCount * 2_000L));
            System.out.println("[ROS2-LIVE] brokerPort=" + broker.port());
            System.out.println("[ROS2-LIVE] localMessages=" + localMessageCount);
            System.out.println("[ROS2-LIVE] forwardedToRos2=" + bridge.forwardedCount());
            if (coordinator.finalDecision() == null) {
                throw new IllegalStateException("No final decision was produced by coordinator.");
            }
            if (bridge.forwardedCount() < localMessageCount) {
                throw new IllegalStateException("Not all local DDS messages were forwarded to ROS2.");
            }
        } finally {
            coordinator.close();
            bridge.close();
            broker.close();
        }
    }

    private static Thread thread(String name, Runnable runnable) {
        Thread thread = new Thread(runnable, name);
        thread.setDaemon(true);
        return thread;
    }
}
