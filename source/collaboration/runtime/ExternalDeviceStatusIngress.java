package collaboration.runtime;

import java.io.Closeable;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Accepts vendor-native device status frames and publishes normalized runtime
 * status into the DDS-compatible collaboration fabric.
 */
public class ExternalDeviceStatusIngress implements Closeable {
    private final ProtocolAdapterRegistry registry;
    private final LocalDDSClient client;

    public ExternalDeviceStatusIngress(String host, int port, ProtocolAdapterRegistry registry) throws IOException {
        this.registry = registry == null ? ProtocolAdapterRegistry.loadDefault() : registry;
        this.client = new LocalDDSClient(host, port, "EXTERNAL-DEVICE-INGRESS");
        this.client.start();
    }

    public void publishNativeStatus(String logicalAgentId, Map<String, Object> nativeStatus) {
        DeviceProfile profile = registry.profileForAgent(logicalAgentId);
        Map<String, Object> normalized = registry.normalizeStatus(profile.protocol, nativeStatus);
        Object externalAgentId = normalized.get("agentId");

        normalized.put("agentId", profile.agentId);
        normalized.put("logicalAgentId", profile.agentId);
        normalized.put("externalAgentId", externalAgentId == null ? profile.agentId : externalAgentId);
        normalized.put("role", profile.role.name());
        normalized.put("protocol", profile.protocol);
        if (!normalized.containsKey("route") || normalized.get("route") == null || "unknown".equals(String.valueOf(normalized.get("route")))) {
            normalized.put("route", profile.initialRoute);
        }

        Map<String, Object> frame = new LinkedHashMap<String, Object>();
        frame.put("logicalAgentId", profile.agentId);
        frame.put("externalAgentId", normalized.get("externalAgentId"));
        frame.put("role", profile.role.name());
        frame.put("protocol", profile.protocol);
        frame.put("unifiedTopic", LocalDDSBroker.TOPIC_AGENT_STATUS);
        frame.put("native", nativeStatus);
        frame.put("normalized", normalized);
        client.publishFrom(LocalDDSBroker.TOPIC_PROTOCOL_FRAME, profile.agentId, frame);
        client.publishFrom(LocalDDSBroker.TOPIC_AGENT_STATUS, profile.agentId, normalized);
    }

    public void close() {
        client.close();
    }
}
