package worldmodel;

import dds.DDSMessage;
import dds.VirtualDDSMessageBus;
import policy.AgentState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dynamic world model built from DDS messages.
 */
public class SystemWorldModel {
    public String passengerIntent = "";
    public Map<String, Object> dslConstraints = new LinkedHashMap<String, Object>();
    public List<String> candidateRoutes = new ArrayList<String>();
    public double congestionLevel = 0.30;
    public double roadRisk = 0.20;
    public double averageConfidence = 0.0;
    public double averageFreshness = 1.0;
    public long latestTimestamp;
    public boolean hasConflict;
    public boolean replanRequired;
    public List<String> activeEvents = new ArrayList<String>();
    public List<Map<String, Object>> droneSuggestions = new ArrayList<Map<String, Object>>();
    public List<Map<String, Object>> roadsideSuggestions = new ArrayList<Map<String, Object>>();
    public List<Map<String, Object>> neighborSuggestions = new ArrayList<Map<String, Object>>();
    public Map<String, Double> sourceTrust = new LinkedHashMap<String, Double>();
    public List<Map<String, Object>> timeline = new ArrayList<Map<String, Object>>();

    private final Map<String, DDSMessage> latestByTopic = new LinkedHashMap<String, DDSMessage>();
    private double confidenceSum;
    private double freshnessSum;
    private int evidenceCount;

    public SystemWorldModel() {
        sourceTrust.put("CAR-CQ-001", 0.86);
        sourceTrust.put("CAR-CQ-002", 0.82);
        sourceTrust.put("PEER-CAR-031", 0.72);
        sourceTrust.put("UAV-CQ-007", 0.88);
        sourceTrust.put("RSU-SCHOOL-009", 0.94);
        sourceTrust.put("CN-BACKBONE-CQ", 0.78);
    }

    public static SystemWorldModel fromMessages(List<DDSMessage> messages) {
        SystemWorldModel model = new SystemWorldModel();
        if (messages != null) {
            for (DDSMessage message : messages) {
                model.update(message);
            }
        }
        return model;
    }

    public synchronized boolean update(DDSMessage message) {
        if (message == null) {
            return false;
        }
        DDSMessage previous = latestByTopic.get(message.topic);
        if (previous != null && previous.timestamp > message.timestamp) {
            recordTimeline(message, "IGNORED_OLDER_THAN_CURRENT");
            return false;
        }
        if (previous != null && previous.timestamp == message.timestamp
            && previous.effectiveWeight() >= message.effectiveWeight()) {
            recordTimeline(message, "IGNORED_LOWER_WEIGHT_SAME_TIME");
            return false;
        }

        latestByTopic.put(message.topic, message);
        latestTimestamp = Math.max(latestTimestamp, message.timestamp);
        confidenceSum += message.confidence;
        freshnessSum += message.messageFreshness();
        evidenceCount++;
        averageConfidence = confidenceSum / evidenceCount;
        averageFreshness = freshnessSum / evidenceCount;

        applyPayload(message);
        evaluateConflict();
        replanRequired = replanRequired || hasConflict || roadRisk >= 0.70 || congestionLevel >= 0.82;
        recordTimeline(message, message.isExpired() ? "UPDATED_STALE" : "UPDATED_FRESH");
        return true;
    }

    public AgentState toAgentState(String agentId, String role) {
        return AgentState.fromWorldModel(agentId, this, candidateRoutes.isEmpty() ? "" : candidateRoutes.get(0));
    }

    public double trustFor(String sourceId) {
        return sourceTrust.getOrDefault(sourceId, 0.60);
    }

    public List<String> routesFromSuggestions() {
        List<String> routes = new ArrayList<String>();
        collectRoutes(routes, droneSuggestions);
        collectRoutes(routes, roadsideSuggestions);
        collectRoutes(routes, neighborSuggestions);
        return routes;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("passengerIntent", passengerIntent);
        map.put("dslConstraints", dslConstraints);
        map.put("candidateRoutes", candidateRoutes);
        map.put("congestionLevel", congestionLevel);
        map.put("roadRisk", roadRisk);
        map.put("averageConfidence", averageConfidence);
        map.put("averageFreshness", averageFreshness);
        map.put("latestTimestamp", latestTimestamp);
        map.put("activeEvents", activeEvents);
        map.put("droneSuggestions", droneSuggestions);
        map.put("roadsideSuggestions", roadsideSuggestions);
        map.put("neighborSuggestions", neighborSuggestions);
        map.put("sourceTrust", sourceTrust);
        map.put("hasConflict", hasConflict);
        map.put("replanRequired", replanRequired);
        map.put("timeline", timeline);
        return map;
    }

    private void applyPayload(DDSMessage message) {
        Map<String, Object> payload = message.payload;
        double weight = sourceTrust.getOrDefault(message.sourceId, 0.60) * message.effectiveWeight();

        if (VirtualDDSMessageBus.TOPIC_INTENT_DSL.equals(message.topic)) {
            passengerIntent = string(payload, "intentText", string(payload, "passengerIntent", passengerIntent));
            Object constraints = payload.get("dslConstraints");
            if (constraints instanceof Map<?, ?>) {
                dslConstraints = copyMap((Map<?, ?>) constraints);
            }
        }

        if (payload.containsKey("route")) {
            String route = String.valueOf(payload.get("route"));
            if (!candidateRoutes.contains(route)) {
                candidateRoutes.add(route);
            }
        }

        double congestion = number(payload, "congestionLevel", number(payload, "congestion", -1.0));
        if (congestion >= 0.0) {
            congestionLevel = weightedMax(congestionLevel, congestion, weight);
        }
        double risk = number(payload, "riskScore", number(payload, "risk_score", -1.0));
        if (risk >= 0.0) {
            roadRisk = weightedMax(roadRisk, risk, weight);
            if (risk >= 0.70 && sourceTrust.getOrDefault(message.sourceId, 0.60) >= 0.80) {
                replanRequired = true;
            }
        }
        String event = string(payload, "road_event", string(payload, "roadEvent", ""));
        if (!event.isBlank()) {
            activeEvents.add(event);
            if (event.contains("congestion") || event.contains("拥堵") || event.contains("accident")) {
                congestionLevel = Math.max(congestionLevel, 0.82);
            }
        }

        if (VirtualDDSMessageBus.TOPIC_DRONE_OBSERVATION.equals(message.topic)) {
            droneSuggestions.add(message.toMap());
        } else if (VirtualDDSMessageBus.TOPIC_ROADSIDE_RISK_EVENT.equals(message.topic)) {
            roadsideSuggestions.add(message.toMap());
        } else if (VirtualDDSMessageBus.TOPIC_NEIGHBOR_STATUS.equals(message.topic)
            || VirtualDDSMessageBus.TOPIC_ROUTE_SUGGESTION.equals(message.topic)) {
            neighborSuggestions.add(message.toMap());
        } else if (VirtualDDSMessageBus.TOPIC_REPLANNING_REQUEST.equals(message.topic)) {
            replanRequired = true;
        }
    }

    private void evaluateConflict() {
        List<String> routes = routesFromSuggestions();
        if (routes.size() < 2) {
            hasConflict = false;
            return;
        }
        String first = routes.get(0);
        for (String route : routes) {
            if (!first.equals(route)) {
                hasConflict = true;
                return;
            }
        }
        hasConflict = false;
    }

    private void recordTimeline(DDSMessage message, String status) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("status", status);
        item.put("topic", message.topic);
        item.put("sourceId", message.sourceId);
        item.put("timestamp", message.timestamp);
        item.put("confidence", message.confidence);
        item.put("freshness", message.messageFreshness());
        item.put("effectiveWeight", message.effectiveWeight());
        timeline.add(item);
    }

    private void collectRoutes(List<String> routes, List<Map<String, Object>> messages) {
        for (Map<String, Object> item : messages) {
            Object payload = item.get("payload");
            if (payload instanceof Map<?, ?>) {
                Object route = ((Map<?, ?>) payload).get("route");
                if (route != null) routes.add(String.valueOf(route));
            }
        }
    }

    private double weightedMax(double current, double observed, double weight) {
        if (observed > current) {
            return current * (1.0 - Math.min(0.85, weight)) + observed * Math.min(0.85, weight);
        }
        return current;
    }

    private double linkQualityFor(String role) {
        if ("UAV".equals(role)) return 0.80;
        if ("RSU".equals(role)) return 0.92;
        if ("COMPLIANT_BACKBONE".equals(role)) return 0.86;
        return 0.84;
    }

    private boolean containsAny(String text, String... tokens) {
        String value = text == null ? "" : text;
        for (String token : tokens) {
            if (value.contains(token)) return true;
        }
        return false;
    }

    private String string(Map<String, Object> payload, String key, String fallback) {
        Object value = payload.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private double number(Map<String, Object> payload, String key, double fallback) {
        Object value = payload.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : fallback;
    }

    private Map<String, Object> copyMap(Map<?, ?> source) {
        Map<String, Object> copy = new LinkedHashMap<String, Object>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            copy.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return copy;
    }
}
