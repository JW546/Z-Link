package collaboration.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

public class Ros2VehicleProtocolAdapter implements DeviceProtocolAdapter {
    public String protocolName() {
        return "ROS2-DDS";
    }

    public Map<String, Object> toNativeStatus(Map<String, Object> unifiedStatus) {
        Map<String, Object> frame = new LinkedHashMap<String, Object>();
        frame.put("protocol", protocolName());
        frame.put("topic", "/urban/vehicle/telemetry");
        frame.put("idlType", "urban::VehicleTelemetry");
        frame.put("node", unifiedStatus.get("agentId"));
        frame.put("stamp", unifiedStatus.get("timestamp"));
        frame.put("pose", pose(unifiedStatus));
        frame.put("speed_mps", unifiedStatus.get("speed"));
        frame.put("battery_percent", unifiedStatus.get("battery"));
        frame.put("route_id", unifiedStatus.get("route"));
        frame.put("qos", "BEST_EFFORT_SENSOR_STREAM");
        return frame;
    }

    public Map<String, Object> toNativeCommand(Map<String, Object> unifiedCommand) {
        Map<String, Object> frame = new LinkedHashMap<String, Object>();
        frame.put("protocol", protocolName());
        frame.put("action", "/urban/mission/execute");
        frame.put("idlType", "urban::MissionCommand");
        frame.put("mission_id", unifiedCommand.get("missionId"));
        frame.put("command", unifiedCommand.get("action"));
        frame.put("route_id", unifiedCommand.get("route"));
        return frame;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> fromNativeStatus(Map<String, Object> nativeStatus) {
        Map<String, Object> unified = new LinkedHashMap<String, Object>();
        Map<String, Object> pose = nativeStatus.get("pose") instanceof Map<?, ?>
            ? (Map<String, Object>) nativeStatus.get("pose") : new LinkedHashMap<String, Object>();
        unified.put("agentId", value(nativeStatus, "node", "UNKNOWN-VEHICLE"));
        unified.put("role", value(nativeStatus, "role", "EGO_CAR"));
        unified.put("timestamp", longValue(nativeStatus.get("stamp"), System.currentTimeMillis()));
        unified.put("protocol", protocolName());
        unified.put("route", value(nativeStatus, "route_id", "unknown"));
        unified.put("x", number(pose.get("x"), 0.0));
        unified.put("y", number(pose.get("y"), 0.0));
        unified.put("z", number(pose.get("z"), 0.0));
        unified.put("speed", number(nativeStatus.get("speed_mps"), 0.0));
        unified.put("battery", number(nativeStatus.get("battery_percent"), 0.0));
        unified.put("rateHz", number(nativeStatus.get("rate_hz"), 0.0));
        unified.put("missionStatus", value(nativeStatus, "mission_status", "EXECUTING"));
        return unified;
    }

    public Map<String, Object> fromNativeCommand(Map<String, Object> nativeCommand) {
        Map<String, Object> unified = new LinkedHashMap<String, Object>();
        unified.put("missionId", nativeCommand.get("mission_id"));
        unified.put("action", nativeCommand.get("command"));
        unified.put("route", nativeCommand.get("route_id"));
        unified.put("protocol", protocolName());
        return unified;
    }

    private Map<String, Object> pose(Map<String, Object> unifiedStatus) {
        Map<String, Object> pose = new LinkedHashMap<String, Object>();
        pose.put("x", unifiedStatus.get("x"));
        pose.put("y", unifiedStatus.get("y"));
        pose.put("z", unifiedStatus.get("z"));
        return pose;
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

    private long longValue(Object value, long fallback) {
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return value == null ? fallback : Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
