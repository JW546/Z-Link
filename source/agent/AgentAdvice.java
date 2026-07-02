package agent;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Route or risk advice produced by an autonomous device Agent.
 */
public class AgentAdvice {
    public final String agentId;
    public final AgentRole role;
    public final String topic;
    public final String route;
    public final double riskScore;
    public final double confidence;
    public final String explanation;

    public AgentAdvice(String agentId, AgentRole role, String topic, String route,
                       double riskScore, double confidence, String explanation) {
        this.agentId = agentId;
        this.role = role;
        this.topic = topic;
        this.route = route;
        this.riskScore = riskScore;
        this.confidence = confidence;
        this.explanation = explanation;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("agentId", agentId);
        map.put("role", role.name());
        map.put("topic", topic);
        map.put("route", route);
        map.put("riskScore", riskScore);
        map.put("confidence", confidence);
        map.put("explanation", explanation);
        return map;
    }
}
