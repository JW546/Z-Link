package dds;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Message delivered over the virtual DDS bus.
 */
public class DDSMessage {
    public final String topic;
    public final String sourceId;
    public final String targetId;
    public final long timestamp;
    public final double confidence;
    public final Map<String, Object> payload;
    public final DDSQoS qos;
    private volatile long deliveredAt;

    public DDSMessage(String topic, String sourceId, String targetId, Map<String, Object> payload, DDSQoS qos) {
        this(topic, sourceId, targetId, payload, qos, extractConfidence(payload), System.currentTimeMillis());
    }

    public DDSMessage(String topic, String sourceId, String targetId, Map<String, Object> payload,
                      DDSQoS qos, double confidence, long timestamp) {
        this.topic = topic;
        this.sourceId = sourceId == null || sourceId.isBlank() ? "UNKNOWN_SOURCE" : sourceId;
        this.targetId = targetId == null || targetId.isBlank() ? "ALL" : targetId;
        this.timestamp = timestamp;
        this.confidence = clamp(confidence);
        this.payload = payload == null ? new LinkedHashMap<String, Object>() : new LinkedHashMap<String, Object>(payload);
        this.qos = qos == null ? DDSQoS.reliable() : qos;
    }

    public void markDelivered(long deliveredAt) {
        this.deliveredAt = Math.max(timestamp, deliveredAt);
    }

    public long deliveredAt() {
        return deliveredAt;
    }

    public long ageMs() {
        return ageMs(System.currentTimeMillis());
    }

    public long ageMs(long nowMs) {
        return Math.max(0L, nowMs - timestamp);
    }

    public boolean isExpired() {
        return isExpired(System.currentTimeMillis());
    }

    public boolean isExpired(long nowMs) {
        return ageMs(nowMs) > qos.timeToLiveMs;
    }

    public double messageFreshness() {
        return messageFreshness(System.currentTimeMillis());
    }

    public double messageFreshness(long nowMs) {
        double freshness = 1.0 - (ageMs(nowMs) / (double) qos.timeToLiveMs);
        return clamp(freshness);
    }

    public double effectiveWeight() {
        return effectiveWeight(System.currentTimeMillis());
    }

    public double effectiveWeight(long nowMs) {
        double stalePenalty = isExpired(nowMs) ? 0.25 : 1.0;
        return clamp(confidence * messageFreshness(nowMs) * stalePenalty);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("topic", topic);
        map.put("sourceId", sourceId);
        map.put("targetId", targetId);
        map.put("timestamp", timestamp);
        map.put("deliveredAt", deliveredAt);
        map.put("confidence", confidence);
        map.put("messageFreshness", messageFreshness());
        map.put("expired", isExpired());
        map.put("effectiveWeight", effectiveWeight());
        map.put("payload", payload);
        map.put("qos", qos.toMap());
        return map;
    }

    private static double extractConfidence(Map<String, Object> payload) {
        if (payload == null) {
            return 1.0;
        }
        Object value = payload.get("confidence");
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 1.0;
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
