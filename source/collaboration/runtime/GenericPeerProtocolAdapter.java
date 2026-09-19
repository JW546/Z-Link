package collaboration.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

public class GenericPeerProtocolAdapter implements DeviceProtocolAdapter {
    public String protocolName() {
        return "GENERIC-PRIVATE-JSON";
    }

    public Map<String, Object> toNativeStatus(Map<String, Object> unifiedStatus) {
        Map<String, Object> frame = new LinkedHashMap<String, Object>();
        frame.put("protocol", protocolName());
        frame.put("message", "peer_status_v1");
        frame.put("device_id", unifiedStatus.get("agentId"));
        frame.put("vendor_payload", unifiedStatus);
        frame.put("compatibility_note", "Mapped from private peer-car JSON into unified DDS status.");
        return frame;
    }

    public Map<String, Object> toNativeCommand(Map<String, Object> unifiedCommand) {
        Map<String, Object> frame = new LinkedHashMap<String, Object>();
        frame.put("protocol", protocolName());
        frame.put("message", "peer_command_v1");
        frame.put("vendor_payload", unifiedCommand);
        return frame;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> fromNativeStatus(Map<String, Object> nativeStatus) {
        if (nativeStatus.get("vendor_payload") instanceof Map<?, ?>) {
            return new LinkedHashMap<String, Object>((Map<String, Object>) nativeStatus.get("vendor_payload"));
        }
        Map<String, Object> unified = new LinkedHashMap<String, Object>();
        unified.put("agentId", value(nativeStatus, "device_id", "UNKNOWN-PEER"));
        unified.put("role", value(nativeStatus, "role", "PEER_CAR"));
        unified.put("timestamp", System.currentTimeMillis());
        unified.put("protocol", protocolName());
        unified.put("route", value(nativeStatus, "route", "unknown"));
        unified.put("x", number(nativeStatus.get("x"), 0.0));
        unified.put("y", number(nativeStatus.get("y"), 0.0));
        unified.put("z", number(nativeStatus.get("z"), 0.0));
        unified.put("speed", number(nativeStatus.get("speed"), 0.0));
        unified.put("battery", number(nativeStatus.get("battery"), 0.0));
        unified.put("rateHz", number(nativeStatus.get("rateHz"), 0.0));
        unified.put("missionStatus", value(nativeStatus, "missionStatus", "EXECUTING"));
        return unified;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> fromNativeCommand(Map<String, Object> nativeCommand) {
        if (nativeCommand.get("vendor_payload") instanceof Map<?, ?>) {
            return new LinkedHashMap<String, Object>((Map<String, Object>) nativeCommand.get("vendor_payload"));
        }
        return new LinkedHashMap<String, Object>(nativeCommand);
    }

    private Object value(Map<String, Object> map, String key, Object fallback) {
        Object value = map.get(key);
        return value == null ? fallback : value;
    }

    private double number(Object value, double fallback) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return value == null ? fallback : Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
