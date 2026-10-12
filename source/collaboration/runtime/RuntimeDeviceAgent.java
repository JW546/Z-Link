package collaboration.runtime;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class RuntimeDeviceAgent implements Runnable, Closeable {
    /** How long a device waits for the task contract before falling back to its configured set. */
    public static final long CONTRACT_WAIT_MS = 1_200L;

    private final String host;
    private final int port;
    private final RuntimeAgentConfig config;
    private final CountDownLatch contractLatch = new CountDownLatch(1);
    private LocalDDSClient client;
    private volatile List<String> contractSubscriptions;
    private volatile List<String> resolvedSubscriptions;

    public RuntimeDeviceAgent(String host, int port, RuntimeAgentConfig config) {
        this.host = host;
        this.port = port;
        this.config = config;
    }

    public void run() {
        try {
            client = new LocalDDSClient(host, port, config.agentId);
            client.onMessage(this::onContractMessage);
            client.start();
            // Bootstrap: listen for the task contract before subscribing to anything else, so the
            // subscription set can come from the contract the coordinator publishes at startup.
            client.subscribe(LocalDDSBroker.TOPIC_INTENT_CONTRACT);
            resolvedSubscriptions = resolveSubscriptions();
            for (String topic : resolvedSubscriptions) {
                if (!LocalDDSBroker.TOPIC_INTENT_CONTRACT.equals(topic)) {
                    client.subscribe(topic);
                }
            }
            for (int tick = 0; tick < config.ticks; tick++) {
                publishStatus(tick);
                Thread.sleep(config.intervalMs);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Runtime device agent failed: " + config.agentId, e);
        } finally {
            close();
        }
    }

    public void close() {
        if (client != null) {
            client.close();
        }
    }

    /**
     * Topics this agent actually subscribes to. A contract-resolved list is used when present;
     * otherwise the agent keeps the original subscription default so existing runs are unchanged.
     */
    public List<String> effectiveSubscriptions() {
        if (resolvedSubscriptions != null) {
            return new ArrayList<String>(resolvedSubscriptions);
        }
        if (config.subscriptions == null || config.subscriptions.isEmpty()) {
            return new ArrayList<String>(ContractSubscriptionResolver.DEFAULT_SUBSCRIPTIONS);
        }
        return new ArrayList<String>(config.subscriptions);
    }

    /**
     * Handles the task contract published on the bus. The contract carries the runtime topics each
     * role should listen on, so a device subscribes according to the contract rather than to a
     * hard-coded list.
     */
    @SuppressWarnings("unchecked")
    private void onContractMessage(RuntimeMessage message) {
        if (message == null || !LocalDDSBroker.TOPIC_INTENT_CONTRACT.equals(message.topic)) {
            return;
        }
        Map<String, Object> payload = message.payload == null
            ? new LinkedHashMap<String, Object>() : message.payload;
        Object byRole = payload.get("runtimeSubscriptionsByRole");
        if (!(byRole instanceof Map<?, ?>)) {
            return;
        }
        Object mine = ((Map<String, Object>) byRole).get(config.role.name());
        if (!(mine instanceof List<?>)) {
            return;
        }
        List<String> topics = new ArrayList<String>();
        for (Object item : (List<Object>) mine) {
            if (item != null && !String.valueOf(item).isBlank()) {
                topics.add(String.valueOf(item));
            }
        }
        if (!topics.isEmpty()) {
            contractSubscriptions = topics;
            contractLatch.countDown();
        }
    }

    /**
     * Subscription resolution order:
     * 1. the contract published on the bus, when it arrives in time;
     * 2. the subscription set resolved from the mission text by the caller;
     * 3. the compatibility default, so a standalone device still works.
     */
    private List<String> resolveSubscriptions() {
        try {
            contractLatch.await(CONTRACT_WAIT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        List<String> fromContract = contractSubscriptions;
        if (fromContract != null && !fromContract.isEmpty()) {
            return new ArrayList<String>(fromContract);
        }
        return effectiveSubscriptions();
    }

    private void publishStatus(int tick) {
        Map<String, Object> status = unifiedStatus(tick);
        client.publish(LocalDDSBroker.TOPIC_AGENT_STATUS, status);

        Map<String, Object> nativeFrame = new LinkedHashMap<String, Object>();
        nativeFrame.put("agentId", config.agentId);
        nativeFrame.put("role", config.role.name());
        nativeFrame.put("unifiedTopic", LocalDDSBroker.TOPIC_AGENT_STATUS);
        nativeFrame.put("native", config.adapter.toNativeStatus(status));
        client.publish(LocalDDSBroker.TOPIC_PROTOCOL_FRAME, nativeFrame);
    }

    private Map<String, Object> unifiedStatus(int tick) {
        Map<String, Object> status = new LinkedHashMap<String, Object>();
        status.put("agentId", config.agentId);
        status.put("role", config.role.name());
        status.put("timestamp", System.currentTimeMillis());
        status.put("protocol", config.adapter.protocolName());
        status.put("route", routeFor(tick));
        status.put("x", 10.0 + tick * 2.0 + roleOffset());
        status.put("y", 6.0 + tick * 1.5 + roleOffset());
        status.put("z", config.role == RuntimeAgentRole.UAV ? 80.0 : 0.0);
        status.put("speed", config.role == RuntimeAgentRole.UAV ? 16.0 : 8.5);
        status.put("battery", Math.max(20.0, 88.0 - tick * 1.8 - roleOffset()));
        status.put("rateHz", 1000.0 / config.intervalMs);
        status.put("missionStatus", "EXECUTING");
        return status;
    }

    private String routeFor(int tick) {
        if (config.role == RuntimeAgentRole.UAV && tick >= 2) {
            return "route_C_aerial_scout";
        }
        return config.initialRoute;
    }

    private double roleOffset() {
        if (config.role == RuntimeAgentRole.UAV) return 3.0;
        if (config.role == RuntimeAgentRole.PEER_CAR) return 1.0;
        return 0.0;
    }

    public static RuntimeAgentConfig defaultConfig(RuntimeAgentRole role, long intervalMs, int ticks) {
        return defaultConfig(role, intervalMs, ticks, null);
    }

    /**
     * Builds a device config whose subscriptions come from the mission contract.
     *
     * @param missionText natural-language mission text; when null or blank the compatibility
     *                    default subscriptions are used instead.
     */
    public static RuntimeAgentConfig defaultConfig(RuntimeAgentRole role, long intervalMs, int ticks,
                                                   String missionText) {
        ProtocolAdapterRegistry registry = ProtocolAdapterRegistry.loadDefault();
        List<String> subscriptions = missionText == null || missionText.isBlank()
            ? new ArrayList<String>()
            : ContractSubscriptionResolver.runtimeSubscriptions(missionText, role);
        return registry.runtimeConfig(agentIdFor(role), subscriptions, intervalMs, ticks);
    }

    private static String agentIdFor(RuntimeAgentRole role) {
        if (role == RuntimeAgentRole.UAV) {
            return "UAV-CQ-007";
        }
        if (role == RuntimeAgentRole.PEER_CAR) {
            return "PEER-CAR-031";
        }
        return "CAR-CQ-001";
    }

    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 19090;
        RuntimeAgentRole role = args.length > 2 ? RuntimeAgentRole.valueOf(args[2]) : RuntimeAgentRole.EGO_CAR;
        long intervalMs = args.length > 3 ? Long.parseLong(args[3]) : 500L;
        int ticks = args.length > 4 ? Integer.parseInt(args[4]) : 120;
        new RuntimeDeviceAgent(host, port, defaultConfig(role, intervalMs, ticks)).run();
    }
}
