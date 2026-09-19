package collaboration.runtime;

import java.io.Closeable;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class RuntimeDeviceAgent implements Runnable, Closeable {
    private final String host;
    private final int port;
    private final RuntimeAgentConfig config;
    private LocalDDSClient client;

    public RuntimeDeviceAgent(String host, int port, RuntimeAgentConfig config) {
        this.host = host;
        this.port = port;
        this.config = config;
    }

    public void run() {
        try {
            client = new LocalDDSClient(host, port, config.agentId);
            client.start();
            client.subscribe(LocalDDSBroker.TOPIC_INTENT_CONTRACT);
            client.subscribe(LocalDDSBroker.TOPIC_REPLAN_REQUEST);
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
        ProtocolAdapterRegistry registry = ProtocolAdapterRegistry.loadDefault();
        if (role == RuntimeAgentRole.UAV) {
            return registry.runtimeConfig("UAV-CQ-007", intervalMs, ticks);
        }
        if (role == RuntimeAgentRole.PEER_CAR) {
            return registry.runtimeConfig("PEER-CAR-031", intervalMs, ticks);
        }
        return registry.runtimeConfig("CAR-CQ-001", intervalMs, ticks);
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
