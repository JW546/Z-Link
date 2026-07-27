package collaboration.ros2;

import collaboration.runtime.LocalDDSBroker;
import collaboration.runtime.LocalDDSClient;
import collaboration.runtime.RuntimeMessage;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Ros2InboundDemoMain {
    public static void main(String[] args) throws Exception {
        long timeoutMs = args.length > 0 ? Long.parseLong(args[0]) : 60_000L;
        LocalDDSBroker broker = new LocalDDSBroker(0);
        broker.start();
        CountDownLatch latch = new CountDownLatch(1);
        LocalDDSClient subscriber = new LocalDDSClient("127.0.0.1", broker.port(), "ROS2-INBOUND-DEMO-SUBSCRIBER");
        Ros2InboundBridge bridge = new Ros2InboundBridge("127.0.0.1", broker.port(), new Ros2DdsAdapter());
        try {
            subscriber.onMessage(message -> {
                System.out.println("[ROS2-INBOUND-DEMO] received topic=" + message.topic
                    + " source=" + message.sourceId + " payload=" + message.payload);
                latch.countDown();
            });
            subscriber.start();
            subscriber.subscribe(LocalDDSBroker.TOPIC_AGENT_STATUS);
            bridge.start();
            System.out.println("[ROS2-INBOUND-DEMO] brokerPort=" + broker.port());
            System.out.println("[ROS2-INBOUND-DEMO] waiting for ROS2 /agent/status -> local DDS runtime/agent/status");
            boolean ok = latch.await(timeoutMs, TimeUnit.MILLISECONDS);
            if (!ok) {
                throw new IllegalStateException("No ROS2 inbound /agent/status message received before timeout.");
            }
        } finally {
            bridge.close();
            subscriber.close();
            broker.close();
        }
    }
}
