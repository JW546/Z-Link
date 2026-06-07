package agent;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 可回放执行时间线事件。
 */
public class ExecutionTimelineEvent {
    public long timestamp;
    public String stage;
    public String actor;
    public String topic;
    public String action;
    public String status;
    public String message;
    public int latencyMs;
    public int retryCount;
    public String riskLevel;

    public ExecutionTimelineEvent(String stage, String actor, String topic, String action, String status,
                                  String message, int latencyMs, int retryCount, String riskLevel) {
        this.timestamp = System.currentTimeMillis();
        this.stage = stage;
        this.actor = actor;
        this.topic = topic;
        this.action = action;
        this.status = status;
        this.message = message;
        this.latencyMs = latencyMs;
        this.retryCount = retryCount;
        this.riskLevel = riskLevel;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("timestamp", timestamp);
        map.put("stage", stage);
        map.put("actor", actor);
        map.put("topic", topic);
        map.put("action", action);
        map.put("status", status);
        map.put("message", message);
        map.put("latencyMs", latencyMs);
        map.put("retryCount", retryCount);
        map.put("riskLevel", riskLevel);
        return map;
    }
}
