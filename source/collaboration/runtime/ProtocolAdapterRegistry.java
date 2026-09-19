package collaboration.runtime;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProtocolAdapterRegistry {
    private static final Gson GSON = new Gson();

    private final Map<String, DeviceProtocolAdapter> adaptersByProtocol = new LinkedHashMap<String, DeviceProtocolAdapter>();
    private final Map<String, DeviceProfile> profilesByAgent = new LinkedHashMap<String, DeviceProfile>();

    public ProtocolAdapterRegistry() {
        register(new Aep84DroneProtocolAdapter());
        register(new DjiDroneProtocolAdapter());
        register(new Ros2VehicleProtocolAdapter());
        register(new GenericPeerProtocolAdapter());
    }

    public static ProtocolAdapterRegistry loadDefault() {
        ProtocolAdapterRegistry registry = new ProtocolAdapterRegistry();
        File config = new File("config/devices.json");
        if (config.exists()) {
            try {
                registry.loadProfiles(config);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load device protocol config: " + config.getAbsolutePath(), e);
            }
        } else {
            registry.registerProfile(new DeviceProfile("UAV-CQ-007", RuntimeAgentRole.UAV, "DJI-STYLE", "route_A_main_road", "DJI-compatible"));
            registry.registerProfile(new DeviceProfile("CAR-CQ-001", RuntimeAgentRole.EGO_CAR, "ROS2-DDS", "route_A_main_road", "ROS2 vehicle"));
            registry.registerProfile(new DeviceProfile("PEER-CAR-031", RuntimeAgentRole.PEER_CAR, "GENERIC-PRIVATE-JSON", "route_A_main_road", "generic peer car"));
        }
        return registry;
    }

    public void register(DeviceProtocolAdapter adapter) {
        adaptersByProtocol.put(adapter.protocolName(), adapter);
    }

    public void registerProfile(DeviceProfile profile) {
        profilesByAgent.put(profile.agentId, profile);
    }

    public DeviceProtocolAdapter adapterForProtocol(String protocol) {
        DeviceProtocolAdapter adapter = adaptersByProtocol.get(protocol);
        if (adapter == null) {
            throw new IllegalArgumentException("Unsupported device protocol: " + protocol);
        }
        return adapter;
    }

    public DeviceProtocolAdapter adapterForAgent(String agentId) {
        DeviceProfile profile = profileForAgent(agentId);
        return adapterForProtocol(profile.protocol);
    }

    public DeviceProfile profileForAgent(String agentId) {
        DeviceProfile profile = profilesByAgent.get(agentId);
        if (profile == null) {
            throw new IllegalArgumentException("Unknown configured device: " + agentId);
        }
        return profile;
    }

    public RuntimeAgentConfig runtimeConfig(String agentId, long intervalMs, int ticks) {
        DeviceProfile profile = profileForAgent(agentId);
        return new RuntimeAgentConfig(profile.agentId, profile.role, adapterForProtocol(profile.protocol),
            profile.initialRoute, intervalMs, ticks);
    }

    public Map<String, Object> normalizeStatus(String protocol, Map<String, Object> nativeStatus) {
        return adapterForProtocol(protocol).fromNativeStatus(nativeStatus);
    }

    public Map<String, Object> nativeStatus(String agentId, Map<String, Object> unifiedStatus) {
        return adapterForAgent(agentId).toNativeStatus(unifiedStatus);
    }

    public List<Map<String, Object>> configuredDevices() {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (DeviceProfile profile : profilesByAgent.values()) {
            result.add(profile.toMap());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private void loadProfiles(File file) throws IOException {
        Map<String, Object> root;
        try (FileReader reader = new FileReader(file)) {
            root = GSON.fromJson(reader, Map.class);
        }
        Object devices = root == null ? null : root.get("devices");
        if (!(devices instanceof List<?>)) {
            throw new IllegalArgumentException("devices.json must contain devices array.");
        }
        for (Object item : (List<Object>) devices) {
            if (!(item instanceof Map<?, ?>)) continue;
            Map<String, Object> map = (Map<String, Object>) item;
            String agentId = string(map.get("agentId"));
            RuntimeAgentRole role = RuntimeAgentRole.valueOf(string(map.get("role")));
            String protocol = string(map.get("protocol"));
            adapterForProtocol(protocol);
            registerProfile(new DeviceProfile(agentId, role, protocol, string(map.get("initialRoute")), string(map.get("vendor"))));
        }
    }

    private String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
