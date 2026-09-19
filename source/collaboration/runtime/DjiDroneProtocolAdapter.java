package collaboration.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

public class DjiDroneProtocolAdapter implements DeviceProtocolAdapter {
    public String protocolName() {
        return "DJI-STYLE";
    }

    public Map<String, Object> toNativeStatus(Map<String, Object> unifiedStatus) {
        Map<String, Object> frame = new LinkedHashMap<String, Object>();
        frame.put("protocol", protocolName());
        frame.put("frameType", "DJI_OSDK_TELEMETRY");
        frame.put("uavId", unifiedStatus.get("agentId"));
        frame.put("flightMode", "MISSION");
        frame.put("batteryPercent", unifiedStatus.get("battery"));
        frame.put("position", vector(unifiedStatus.get("x"), unifiedStatus.get("y"), unifiedStatus.get("z")));
        frame.put("activeRoute", unifiedStatus.get("route"));
        frame.put("sampleRateHz", unifiedStatus.get("rateHz"));
        return frame;
    }

    public Map<String, Object> toNativeCommand(Map<String, Object> unifiedCommand) {
        Map<String, Object> frame = new LinkedHashMap<String, Object>();
        frame.put("protocol", protocolName());
        frame.put("frameType", "DJI_WAYPOINT_MISSION");
        frame.put("missionId", unifiedCommand.get("missionId"));
        frame.put("action", unifiedCommand.get("action"));
        frame.put("targetArea", unifiedCommand.get("targetArea"));
        return frame;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> fromNativeStatus(Map<String, Object> nativeStatus) {
        Map<String, Object> unified = new LinkedHashMap<String, Object>();
        Map<String, Object> position = nativeStatus.get("position") instanceof Map<?, ?>
            ? (Map<String, Object>) nativeStatus.get("position") : new LinkedHashMap<String, Object>();
        unified.put("agentId", value(nativeStatus, "uavId", "UNKNOWN-UAV"));
        unified.put("role", "UAV");
        unified.put("timestamp", System.currentTimeMillis());
        unified.put("protocol", protocolName());
        unified.put("route", value(nativeStatus, "activeRoute", "unknown"));
        unified.put("x", number(position.get("x"), 0.0));
        unified.put("y", number(position.get("y"), 0.0));
        unified.put("z", number(position.get("z"), 0.0));
        unified.put("speed", number(nativeStatus.get("speedMps"), 0.0));
        unified.put("battery", number(nativeStatus.get("batteryPercent"), 0.0));
        unified.put("rateHz", number(nativeStatus.get("sampleRateHz"), 0.0));
        unified.put("missionStatus", value(nativeStatus, "flightMode", "UNKNOWN"));
        return unified;
    }

    public Map<String, Object> fromNativeCommand(Map<String, Object> nativeCommand) {
        Map<String, Object> unified = new LinkedHashMap<String, Object>();
        unified.put("missionId", nativeCommand.get("missionId"));
        unified.put("action", nativeCommand.get("action"));
        unified.put("targetArea", nativeCommand.get("targetArea"));
        unified.put("protocol", protocolName());
        return unified;
    }

    private Map<String, Object> vector(Object x, Object y, Object z) {
        Map<String, Object> point = new LinkedHashMap<String, Object>();
        point.put("x", x);
        point.put("y", y);
        point.put("z", z);
        return point;
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
