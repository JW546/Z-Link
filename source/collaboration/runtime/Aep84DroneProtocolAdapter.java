package collaboration.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapter for a representative AEP-84 / STANAG 4586 Data Link Interface message.
 *
 * It maps a subset of AEP-84 DLI fields, such as Vehicle ID and UA state data,
 * into the unified runtime status used by the coordination layer.
 */
public class Aep84DroneProtocolAdapter implements DeviceProtocolAdapter {
    public static final String PROTOCOL_NAME = "AEP84-STANAG4586";

    public String protocolName() {
        return PROTOCOL_NAME;
    }

    public Map<String, Object> toNativeStatus(Map<String, Object> unifiedStatus) {
        Map<String, Object> frame = new LinkedHashMap<String, Object>();
        Map<String, Object> position = point(
            unifiedStatus.get("x"),
            unifiedStatus.get("y"),
            unifiedStatus.get("z")
        );

        frame.put("protocol", PROTOCOL_NAME);
        frame.put("messageType", value(unifiedStatus, "messageType", "20"));
        frame.put("messageInstanceId", longValue(unifiedStatus.get("messageInstanceId"), 1L));
        frame.put("streamId", longValue(unifiedStatus.get("streamId"), 1L));
        frame.put("ackRequired", booleanValue(unifiedStatus.get("ackRequired"), true));
        frame.put("validTimeMs", longValue(unifiedStatus.get("validTimeMs"), 1000L));
        frame.put("latencyMs", longValue(unifiedStatus.get("latencyMs"), 80L));
        frame.put("vehicleId", value(unifiedStatus, "agentId", "UAV-AEP84-UNKNOWN"));
        frame.put("cucsId", "CUCS-CQ-001");
        frame.put("vsmId", "VSM-UAV-001");
        frame.put("vehicleType", value(unifiedStatus, "role", "UAV"));
        frame.put("position", position);
        frame.put("speedMps", number(unifiedStatus.get("speed"), 0.0));
        frame.put("batteryPercent", number(unifiedStatus.get("battery"), 0.0));
        frame.put("publishRateHz", number(unifiedStatus.get("rateHz"), 0.0));
        frame.put("missionStatus", value(unifiedStatus, "missionStatus", "UNKNOWN"));
        frame.put("activeRoute", value(unifiedStatus, "route", "unknown"));
        frame.put("checksum", checksum(unifiedStatus));
        return frame;
    }

    public Map<String, Object> toNativeCommand(Map<String, Object> unifiedCommand) {
        Map<String, Object> frame = new LinkedHashMap<String, Object>();
        frame.put("protocol", PROTOCOL_NAME);
        frame.put("messageType", "49");
        frame.put("missionId", value(unifiedCommand, "missionId", "MSN-UNKNOWN"));
        frame.put("action", value(unifiedCommand, "action", "EXECUTE"));
        frame.put("route", value(unifiedCommand, "route", "route_A_main_road"));
        return frame;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> fromNativeStatus(Map<String, Object> nativeStatus) {
        Map<String, Object> unified = new LinkedHashMap<String, Object>();
        Map<String, Object> position = nativeStatus.get("position") instanceof Map<?, ?>
            ? (Map<String, Object>) nativeStatus.get("position")
            : new LinkedHashMap<String, Object>();

        unified.put("agentId", value(nativeStatus, "vehicleId", "UAV-AEP84-UNKNOWN"));
        unified.put("role", normalizeRole(String.valueOf(value(nativeStatus, "vehicleType", "UAV"))));
        unified.put("timestamp", System.currentTimeMillis());
        unified.put("protocol", PROTOCOL_NAME);
        unified.put("route", value(nativeStatus, "activeRoute", "unknown"));
        unified.put("x", number(position.get("lat"), 0.0));
        unified.put("y", number(position.get("lon"), 0.0));
        unified.put("z", number(position.get("alt_m"), 0.0));
        unified.put("speed", number(nativeStatus.get("speedMps"), 0.0));
        unified.put("battery", number(nativeStatus.get("batteryPercent"), 0.0));
        unified.put("rateHz", number(nativeStatus.get("publishRateHz"), 0.0));
        unified.put("missionStatus", value(nativeStatus, "missionStatus", "UNKNOWN"));

        unified.put("messageType", value(nativeStatus, "messageType", ""));
        unified.put("messageInstanceId", longValue(nativeStatus.get("messageInstanceId"), 0L));
        unified.put("streamId", longValue(nativeStatus.get("streamId"), 0L));
        unified.put("ackRequired", booleanValue(nativeStatus.get("ackRequired"), false));
        unified.put("validTimeMs", longValue(nativeStatus.get("validTimeMs"), 0L));
        unified.put("latencyMs", longValue(nativeStatus.get("latencyMs"), 0L));
        unified.put("checksum", value(nativeStatus, "checksum", ""));
        return unified;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> fromNativeCommand(Map<String, Object> nativeCommand) {
        Map<String, Object> unified = new LinkedHashMap<String, Object>();
        unified.put("missionId", value(nativeCommand, "missionId", ""));
        unified.put("action", value(nativeCommand, "action", "EXECUTE"));
        unified.put("route", value(nativeCommand, "route", ""));
        unified.put("protocol", PROTOCOL_NAME);
        return unified;
    }

    private Map<String, Object> point(Object x, Object y, Object z) {
        Map<String, Object> point = new LinkedHashMap<String, Object>();
        point.put("lat", x);
        point.put("lon", y);
        point.put("alt_m", z);
        return point;
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) return "UAV";
        String normalized = role.toUpperCase().replace('-', '_');
        if (normalized.contains("CAR") || normalized.contains("VEHICLE")) return "EGO_CAR";
        if (normalized.contains("UAV") || normalized.contains("UA") || normalized.contains("DRONE")) return "UAV";
        return "UAV";
    }

    private String checksum(Map<String, Object> unified) {
        Object existing = unified.get("checksum");
        if (existing != null && !String.valueOf(existing).isBlank()) {
            return String.valueOf(existing);
        }
        return "AEP84-CHECKSUM-PLACEHOLDER";
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

    private boolean booleanValue(Object value, boolean fallback) {
        if (value instanceof Boolean) return (Boolean) value;
        if (value == null) return fallback;
        String text = String.valueOf(value).trim();
        if ("true".equalsIgnoreCase(text)) return true;
        if ("false".equalsIgnoreCase(text)) return false;
        return fallback;
    }
}
