package collaboration.ros2;

import collaboration.runtime.LocalDDSClient;
import collaboration.runtime.LocalDDSBroker;
import collaboration.runtime.DeviceProfile;
import collaboration.runtime.ProtocolAdapterRegistry;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Ros2InboundBridge implements Closeable {
    private final String host;
    private final int port;
    private final Ros2DdsAdapter adapter;
    private final ProtocolAdapterRegistry registry;
    private final List<Process> echoProcesses = new ArrayList<Process>();
    private volatile LocalDDSClient client;
    private volatile boolean running;

    public Ros2InboundBridge(String host, int port, Ros2DdsAdapter adapter) {
        this(host, port, adapter, ProtocolAdapterRegistry.loadDefault());
    }

    public Ros2InboundBridge(String host, int port, Ros2DdsAdapter adapter, ProtocolAdapterRegistry registry) {
        this.host = host;
        this.port = port;
        this.adapter = adapter == null ? new Ros2DdsAdapter() : adapter;
        this.registry = registry == null ? ProtocolAdapterRegistry.loadDefault() : registry;
    }

    public void start() throws IOException {
        startLocalOnly();
        for (Ros2TopicBinding binding : adapter.topicBindings()) {
            if (binding.direction.toLowerCase(Locale.ROOT).contains("sub")) {
                startEcho(binding);
            }
        }
    }

    public void startLocalOnly() throws IOException {
        client = new LocalDDSClient(host, port, "ROS2-INBOUND-BRIDGE");
        client.start();
        running = true;
    }

    public boolean ingestData(Ros2TopicBinding binding, String data) {
        if (binding == null || data == null || data.trim().isEmpty() || client == null) {
            return false;
        }
        Map<String, String> fields = parsePipePayload(data);
        if (fields.containsKey("logicalAgent")) {
            return ingestNativeStatusFields(binding, fields, data);
        }
        String semanticTopic = fields.getOrDefault("topic", binding.semanticTopic);
        String sourceId = fields.getOrDefault("source", fields.getOrDefault("agent", "ROS2-EXTERNAL-DEVICE"));
        Map<String, Object> payload = toRuntimePayload(binding, fields, data);
        client.publishFrom(semanticTopic, sourceId, payload);
        return true;
    }

    public void close() {
        running = false;
        for (Process process : echoProcesses) {
            process.destroy();
        }
        LocalDDSClient openClient = client;
        if (openClient != null) {
            openClient.close();
        }
    }

    private void startEcho(Ros2TopicBinding binding) throws IOException {
        List<String> command = new ArrayList<String>();
        command.add("python");
        command.add(subscriberScript(binding).getPath());
        command.add(binding.rosTopic);
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        echoProcesses.add(process);
        Thread reader = new Thread(() -> readEchoLoop(binding, process), "ros2-inbound-" + binding.rosTopic.replace("/", "-"));
        reader.setDaemon(true);
        reader.start();
    }

    private File subscriberScript(Ros2TopicBinding binding) {
        if ("urban_runtime_msgs/msg/RuntimeAgentStatus".equals(binding.messageType)) {
            return new File("tools", "ros2_runtime_agent_status_subscriber.py");
        }
        return new File("tools", "ros2_string_subscriber.py");
    }

    private void readEchoLoop(Ros2TopicBinding binding, Process process) {
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while (running && (line = reader.readLine()) != null) {
                if (line.startsWith("[ROS2-STRING-SUB]")) {
                    System.out.println("[ROS2-INBOUND] " + line);
                }
                String data = extractDataLine(line);
                if (data != null) {
                    ingestData(binding, data);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private String extractDataLine(String line) {
        if (line == null) return null;
        String trimmed = line.trim();
        if (!trimmed.startsWith("data:")) return null;
        String data = trimmed.substring("data:".length()).trim();
        if ((data.startsWith("'") && data.endsWith("'")) || (data.startsWith("\"") && data.endsWith("\""))) {
            data = data.substring(1, data.length() - 1);
        }
        return data;
    }

    private Map<String, String> parsePipePayload(String data) {
        Map<String, String> fields = new LinkedHashMap<String, String>();
        String[] parts = data.split("\\|");
        for (String part : parts) {
            int idx = part.indexOf('=');
            if (idx > 0) {
                fields.put(part.substring(0, idx).trim(), part.substring(idx + 1).trim());
            }
        }
        return fields;
    }

    private boolean ingestNativeStatusFields(Ros2TopicBinding binding, Map<String, String> fields, String rawData) {
        String logicalAgentId = fields.get("logicalAgent");
        DeviceProfile profile = registry.profileForAgent(logicalAgentId);
        String protocol = fields.getOrDefault("protocol", profile.protocol);
        Map<String, Object> nativeStatus = nativeStatus(protocol, fields);
        Map<String, Object> normalized = registry.normalizeStatus(protocol, nativeStatus);
        Object externalAgentId = normalized.get("agentId");

        normalized.put("agentId", profile.agentId);
        normalized.put("logicalAgentId", profile.agentId);
        normalized.put("externalAgentId", externalAgentId == null ? fields.getOrDefault("source", profile.agentId) : externalAgentId);
        normalized.put("role", profile.role.name());
        normalized.put("protocol", profile.protocol);
        normalized.put("rosTopic", binding.rosTopic);
        normalized.put("adapter", "ros2_inbound_native_adapter");

        Map<String, Object> frame = new LinkedHashMap<String, Object>();
        frame.put("logicalAgentId", profile.agentId);
        frame.put("externalAgentId", normalized.get("externalAgentId"));
        frame.put("role", profile.role.name());
        frame.put("protocol", protocol);
        frame.put("rosTopic", binding.rosTopic);
        frame.put("rawData", rawData);
        frame.put("unifiedTopic", LocalDDSBroker.TOPIC_AGENT_STATUS);
        frame.put("native", nativeStatus);
        frame.put("normalized", normalized);
        client.publishFrom(LocalDDSBroker.TOPIC_PROTOCOL_FRAME, profile.agentId, frame);
        client.publishFrom(LocalDDSBroker.TOPIC_AGENT_STATUS, profile.agentId, normalized);
        return true;
    }

    private Map<String, Object> nativeStatus(String protocol, Map<String, String> fields) {
        if ("DJI-STYLE".equals(protocol)) {
            Map<String, Object> status = new LinkedHashMap<String, Object>();
            status.put("uavId", fields.getOrDefault("source", fields.getOrDefault("agent", "UAV-ROS2-EXTERNAL")));
            status.put("flightMode", fields.getOrDefault("flightMode", "MISSION"));
            status.put("batteryPercent", number(fields.get("battery"), 0.0));
            status.put("position", point(fields, "x", "y", "z"));
            status.put("activeRoute", fields.getOrDefault("route", "unknown"));
            status.put("speedMps", number(fields.get("speed"), 0.0));
            status.put("sampleRateHz", number(fields.get("rateHz"), 0.0));
            return status;
        }
        if ("ROS2-DDS".equals(protocol)) {
            Map<String, Object> status = new LinkedHashMap<String, Object>();
            status.put("node", fields.getOrDefault("source", fields.getOrDefault("agent", "CAR-ROS2-EXTERNAL")));
            status.put("role", fields.getOrDefault("role", "EGO_CAR"));
            status.put("stamp", longNumber(fields.get("stamp"), System.currentTimeMillis()));
            status.put("pose", point(fields, "x", "y", "z"));
            status.put("speed_mps", number(fields.get("speed"), 0.0));
            status.put("battery_percent", number(fields.get("battery"), 0.0));
            status.put("route_id", fields.getOrDefault("route", "unknown"));
            status.put("rate_hz", number(fields.get("rateHz"), 0.0));
            return status;
        }
        Map<String, Object> vendor = new LinkedHashMap<String, Object>();
        vendor.put("agentId", fields.getOrDefault("source", fields.getOrDefault("agent", "PEER-ROS2-EXTERNAL")));
        vendor.put("role", fields.getOrDefault("role", "PEER_CAR"));
        vendor.put("timestamp", longNumber(fields.get("stamp"), System.currentTimeMillis()));
        vendor.put("protocol", protocol);
        vendor.put("route", fields.getOrDefault("route", "unknown"));
        vendor.put("x", number(fields.get("x"), 0.0));
        vendor.put("y", number(fields.get("y"), 0.0));
        vendor.put("z", number(fields.get("z"), 0.0));
        vendor.put("speed", number(fields.get("speed"), 0.0));
        vendor.put("battery", number(fields.get("battery"), 0.0));
        vendor.put("rateHz", number(fields.get("rateHz"), 0.0));
        vendor.put("missionStatus", fields.getOrDefault("missionStatus", "EXECUTING"));

        Map<String, Object> status = new LinkedHashMap<String, Object>();
        status.put("message", "peer_status_v1");
        status.put("device_id", vendor.get("agentId"));
        status.put("vendor_payload", vendor);
        return status;
    }

    private Map<String, Object> point(Map<String, String> fields, String xKey, String yKey, String zKey) {
        Map<String, Object> point = new LinkedHashMap<String, Object>();
        point.put("x", number(fields.get(xKey), 0.0));
        point.put("y", number(fields.get(yKey), 0.0));
        point.put("z", number(fields.get(zKey), 0.0));
        return point;
    }

    private double number(String value, double fallback) {
        try {
            return value == null ? fallback : Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private long longNumber(String value, long fallback) {
        try {
            return value == null ? fallback : Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private Map<String, Object> toRuntimePayload(Ros2TopicBinding binding, Map<String, String> fields, String rawData) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("rosTopic", binding.rosTopic);
        payload.put("messageType", binding.messageType);
        payload.put("rawData", rawData);
        payload.put("adapter", "ros2_inbound");
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            payload.put(entry.getKey(), entry.getValue());
        }
        if (fields.containsKey("agent")) payload.put("agentId", fields.get("agent"));
        if (fields.containsKey("role")) payload.put("role", fields.get("role"));
        if (fields.containsKey("route")) {
            payload.put("route", fields.get("route"));
            if (LocalDDSBroker.TOPIC_FINAL_DECISION.equals(fields.get("topic"))) {
                payload.put("selectedRoute", fields.get("route"));
            }
        }
        if (fields.containsKey("strategy")) payload.put("selectedStrategy", fields.get("strategy"));
        return payload;
    }
}
