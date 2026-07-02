package integration;

import dds.DDSMessage;
import dds.DDSQoS;
import dds.VirtualDDSMessageBus;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Focused tests for the virtual DDS message bus semantics.
 */
public class DDSMessageBusTest {
    public static void main(String[] args) throws Exception {
        testDefaultTopicsAreRegistered();
        testSubscriberReceivesPublishedMessage();
        testAgentsReceiveOnlySubscribedTopics();
        testDelayAffectsFreshness();
        testExpiredMessageWeightIsLower();
        testDroppedMessageIsRecorded();

        System.out.println("[PASS] DDS message bus test passed.");
    }

    private static void testDefaultTopicsAreRegistered() {
        VirtualDDSMessageBus bus = new VirtualDDSMessageBus(7L);
        try {
            require(bus.topic(VirtualDDSMessageBus.TOPIC_INTENT_DSL) != null, "intent DSL topic registered");
            require(bus.topic(VirtualDDSMessageBus.TOPIC_ROUTE_SUGGESTION) != null, "route suggestion topic registered");
            require(bus.topic(VirtualDDSMessageBus.TOPIC_DRONE_OBSERVATION) != null, "drone observation topic registered");
            require(bus.topic(VirtualDDSMessageBus.TOPIC_ROADSIDE_RISK_EVENT) != null, "roadside risk topic registered");
            require(bus.topic(VirtualDDSMessageBus.TOPIC_NEIGHBOR_STATUS) != null, "neighbor vehicle topic registered");
            require(bus.topic(VirtualDDSMessageBus.TOPIC_FINAL_DECISION) != null, "final decision topic registered");
            require(bus.topic(VirtualDDSMessageBus.TOPIC_REPLANNING_REQUEST) != null, "replanning request topic registered");
        } finally {
            bus.shutdown();
        }
    }

    private static void testSubscriberReceivesPublishedMessage() {
        VirtualDDSMessageBus bus = new VirtualDDSMessageBus(11L);
        try {
            AtomicInteger received = new AtomicInteger();
            bus.subscribe(VirtualDDSMessageBus.TOPIC_ROUTE_SUGGESTION, message -> {
                if ("CAR-CQ-001".equals(message.sourceId)) {
                    received.incrementAndGet();
                }
            });

            bus.publish(VirtualDDSMessageBus.TOPIC_ROUTE_SUGGESTION, "CAR-CQ-001",
                payload("route", "route_D", "confidence", 0.92), DDSQoS.reliable());

            require(received.get() == 1, "subscriber receives published message");
        } finally {
            bus.shutdown();
        }
    }

    private static void testAgentsReceiveOnlySubscribedTopics() {
        VirtualDDSMessageBus bus = new VirtualDDSMessageBus(13L);
        try {
            AtomicInteger carAgent = new AtomicInteger();
            AtomicInteger droneAgent = new AtomicInteger();
            bus.subscribe(VirtualDDSMessageBus.TOPIC_ROUTE_SUGGESTION, message -> carAgent.incrementAndGet());
            bus.subscribe(VirtualDDSMessageBus.TOPIC_DRONE_OBSERVATION, message -> droneAgent.incrementAndGet());

            bus.publish(VirtualDDSMessageBus.TOPIC_ROUTE_SUGGESTION, "CAR-CQ-001",
                payload("route", "route_D", "confidence", 0.9), DDSQoS.reliable());

            require(carAgent.get() == 1, "route subscriber receives route topic");
            require(droneAgent.get() == 0, "drone subscriber does not receive route topic");
        } finally {
            bus.shutdown();
        }
    }

    private static void testDelayAffectsFreshness() throws Exception {
        VirtualDDSMessageBus bus = new VirtualDDSMessageBus(17L);
        try {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<DDSMessage> delivered = new AtomicReference<DDSMessage>();
            bus.subscribe(VirtualDDSMessageBus.TOPIC_DRONE_OBSERVATION, message -> {
                delivered.set(message);
                latch.countDown();
            });

            DDSQoS delayedQos = DDSQoS.bestEffort()
                .withDeliveryDelayMs(120L)
                .withTimeToLiveMs(1_000L);
            bus.publish(VirtualDDSMessageBus.TOPIC_DRONE_OBSERVATION, "UAV-CQ-007",
                payload("traffic", "congested", "confidence", 0.85), delayedQos);

            require(latch.await(2, TimeUnit.SECONDS), "delayed message is eventually delivered");
            DDSMessage message = delivered.get();
            require(message.deliveredAt() - message.timestamp >= 100L, "delivery timestamp records simulated delay");
            require(message.messageFreshness(message.deliveredAt()) < 1.0, "delay lowers message freshness");
        } finally {
            bus.shutdown();
        }
    }

    private static void testExpiredMessageWeightIsLower() throws Exception {
        DDSQoS shortTtl = DDSQoS.reliable().withTimeToLiveMs(20L);
        DDSMessage message = new DDSMessage(VirtualDDSMessageBus.TOPIC_ROADSIDE_RISK_EVENT, "RSU-SCHOOL-009",
            "ALL", payload("risk", "accident", "confidence", 0.8), shortTtl);

        double freshWeight = message.effectiveWeight(message.timestamp);
        Thread.sleep(35L);
        double expiredWeight = message.effectiveWeight(System.currentTimeMillis());

        require(message.isExpired(System.currentTimeMillis()), "message expires after TTL");
        require(expiredWeight < freshWeight, "expired message weight is lower");
    }

    private static void testDroppedMessageIsRecorded() {
        VirtualDDSMessageBus bus = new VirtualDDSMessageBus(19L);
        try {
            AtomicInteger received = new AtomicInteger();
            bus.subscribe(VirtualDDSMessageBus.TOPIC_NEIGHBOR_STATUS, message -> received.incrementAndGet());

            DDSQoS dropAll = DDSQoS.bestEffort().withDropProbability(1.0);
            bus.publish(VirtualDDSMessageBus.TOPIC_NEIGHBOR_STATUS, "PEER-CAR-031",
                payload("speed", 12.5, "confidence", 0.74), dropAll);

            require(received.get() == 0, "dropped message is not delivered");
            require(bus.droppedMessages().size() == 1, "dropped message is recorded");
        } finally {
            bus.shutdown();
        }
    }

    private static Map<String, Object> payload(Object... values) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        for (int i = 0; i < values.length; i += 2) {
            payload.put(String.valueOf(values[i]), values[i + 1]);
        }
        return payload;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("[FAIL] " + message);
        }
        System.out.println("[PASS] " + message);
    }
}
