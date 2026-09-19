package collaboration.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Wire message used by the local DDS-compatible runtime.
 */
public class RuntimeMessage {
    public String type;
    public String topic;
    public String sourceId;
    public String targetId;
    public long timestamp;
    public Map<String, Object> payload;

    public RuntimeMessage() {
        this.payload = new LinkedHashMap<String, Object>();
    }

    public static RuntimeMessage subscribe(String topic, String sourceId) {
        RuntimeMessage message = new RuntimeMessage();
        message.type = "subscribe";
        message.topic = topic;
        message.sourceId = sourceId;
        message.targetId = "BROKER";
        message.timestamp = System.currentTimeMillis();
        return message;
    }

    public static RuntimeMessage publish(String topic, String sourceId, Map<String, Object> payload) {
        RuntimeMessage message = new RuntimeMessage();
        message.type = "publish";
        message.topic = topic;
        message.sourceId = sourceId;
        message.targetId = "ALL";
        message.timestamp = System.currentTimeMillis();
        message.payload = payload == null ? new LinkedHashMap<String, Object>() : new LinkedHashMap<String, Object>(payload);
        return message;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("type", type);
        map.put("topic", topic);
        map.put("sourceId", sourceId);
        map.put("targetId", targetId);
        map.put("timestamp", timestamp);
        map.put("payload", payload == null ? new LinkedHashMap<String, Object>() : payload);
        return map;
    }
}
