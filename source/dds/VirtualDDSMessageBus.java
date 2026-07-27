package dds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * In-memory DDS publish/subscribe adapter for tests and simulations.
 */
public class VirtualDDSMessageBus {
    public static final String TOPIC_INTENT_DSL = "cabin/passenger/intent.dsl";
    public static final String TOPIC_VEHICLE_STATUS = "vehicle/status/telemetry";
    public static final String TOPIC_ROUTE_SUGGESTION = "vehicle/route/suggestion";
    public static final String TOPIC_DRONE_OBSERVATION = "drone/traffic/observation";
    public static final String TOPIC_ROADSIDE_RISK_EVENT = "roadside/risk/event";
    public static final String TOPIC_NEIGHBOR_STATUS = "neighbor/vehicle/status";
    public static final String TOPIC_FINAL_DECISION = "coordination/final/decision";
    public static final String TOPIC_REPLANNING_REQUEST = "coordination/replanning/request";
    public static final String TOPIC_DEVICE_COORDINATION_ADVICE = "coordination/device/advice";

    private final Map<String, DDSTopic> topics = new ConcurrentHashMap<String, DDSTopic>();
    private final Map<String, List<Consumer<DDSMessage>>> subscribers =
        new ConcurrentHashMap<String, List<Consumer<DDSMessage>>>();
    private final List<DDSMessage> history = Collections.synchronizedList(new ArrayList<DDSMessage>());
    private final List<DDSMessage> droppedMessages = Collections.synchronizedList(new ArrayList<DDSMessage>());
    private final ScheduledExecutorService scheduler;
    private final Random random;

    public VirtualDDSMessageBus() {
        this(new Random());
    }

    public VirtualDDSMessageBus(long randomSeed) {
        this(new Random(randomSeed));
    }

    private VirtualDDSMessageBus(Random random) {
        this.random = random;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "VirtualDDSMessageBus");
                thread.setDaemon(true);
                return thread;
            }
        });
        registerDefaultTopics();
    }

    public DDSTopic registerTopic(String topicName, String idlType, DDSQoS qos) {
        return registerTopic(new DDSTopic(topicName, topicName, idlType, qos));
    }

    public DDSTopic registerTopic(DDSTopic topic) {
        if (topic == null || topic.name == null || topic.name.isBlank()) {
            throw new IllegalArgumentException("DDS topic name is required.");
        }
        DDSTopic normalized = new DDSTopic(topic.name, topic.ddsTopic, topic.idlType, topic.qos);
        topics.put(normalized.name, normalized);
        return normalized;
    }

    public DDSTopic topic(String topicName) {
        return topics.get(topicName);
    }

    public List<Map<String, Object>> registeredTopics() {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (DDSTopic topic : topics.values()) {
            result.add(new LinkedHashMap<String, Object>(topic.toMap()));
        }
        return result;
    }

    public void subscribe(String topic, Consumer<DDSMessage> consumer) {
        ensureTopic(topic);
        subscribers.computeIfAbsent(topic, ignored -> new CopyOnWriteArrayList<Consumer<DDSMessage>>())
            .add(consumer);
    }

    public DDSMessage publish(String topic, String sourceId, Map<String, Object> payload, DDSQoS qos) {
        return publishTo(topic, sourceId, "ALL", payload, qos);
    }

    public DDSMessage publishTo(String topic, String sourceId, String targetId, Map<String, Object> payload, DDSQoS qos) {
        DDSTopic registered = ensureTopic(topic);
        DDSQoS effectiveQos = qos == null ? registered.qos : qos;
        DDSMessage message = new DDSMessage(topic, sourceId, targetId, payload, effectiveQos);
        history.add(message);

        if (shouldDrop(effectiveQos)) {
            droppedMessages.add(message);
            return message;
        }

        List<Consumer<DDSMessage>> handlers = subscribers.get(topic);
        if (handlers != null) {
            Runnable delivery = () -> deliver(message, handlers);
            if (effectiveQos.deliveryDelayMs == 0L) {
                delivery.run();
            } else {
                scheduler.schedule(delivery, effectiveQos.deliveryDelayMs, TimeUnit.MILLISECONDS);
            }
        }
        return message;
    }

    public List<Map<String, Object>> droppedMessages() {
        return copyMessages(droppedMessages);
    }

    public List<Map<String, Object>> history() {
        return copyMessages(history);
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }

    private void registerDefaultTopics() {
        registerTopic(TOPIC_INTENT_DSL, "IntentTaskContract", DDSQoS.reliable());
        registerTopic(TOPIC_VEHICLE_STATUS, "VirtualVehicleStatus", DDSQoS.bestEffort());
        registerTopic(TOPIC_ROUTE_SUGGESTION, "RouteSuggestion", DDSQoS.reliable());
        registerTopic(TOPIC_DRONE_OBSERVATION, "TrafficObservation", DDSQoS.bestEffort());
        registerTopic(TOPIC_ROADSIDE_RISK_EVENT, "RoadsideRiskEvent", DDSQoS.reliable());
        registerTopic(TOPIC_NEIGHBOR_STATUS, "NeighborVehicleStatus", DDSQoS.bestEffort());
        registerTopic(TOPIC_FINAL_DECISION, "FinalCoordinationDecision", DDSQoS.reliable());
        registerTopic(TOPIC_REPLANNING_REQUEST, "ReplanningRequest", DDSQoS.reliable());
        registerTopic(TOPIC_DEVICE_COORDINATION_ADVICE, "DeviceCoordinationAdvice", DDSQoS.reliable());
    }

    private DDSTopic ensureTopic(String topicName) {
        if (topicName == null || topicName.isBlank()) {
            throw new IllegalArgumentException("DDS topic name is required.");
        }
        return topics.computeIfAbsent(topicName,
            name -> new DDSTopic(name, name, "GenericVirtualDDSMessage", DDSQoS.reliable()));
    }

    private boolean shouldDrop(DDSQoS qos) {
        if (qos.dropProbability <= 0.0) {
            return false;
        }
        if (qos.dropProbability >= 1.0) {
            return true;
        }
        synchronized (random) {
            return random.nextDouble() < qos.dropProbability;
        }
    }

    private void deliver(DDSMessage message, List<Consumer<DDSMessage>> handlers) {
        message.markDelivered(System.currentTimeMillis());
        for (Consumer<DDSMessage> handler : handlers) {
            handler.accept(message);
        }
    }

    private List<Map<String, Object>> copyMessages(List<DDSMessage> messages) {
        List<Map<String, Object>> copy = new ArrayList<Map<String, Object>>();
        synchronized (messages) {
            for (DDSMessage message : messages) {
                copy.add(new LinkedHashMap<String, Object>(message.toMap()));
            }
        }
        return copy;
    }
}
