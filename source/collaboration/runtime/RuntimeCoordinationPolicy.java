package collaboration.runtime;

import dds.DDSMessage;
import dds.DDSQoS;
import dds.VirtualDDSMessageBus;
import decision.ConflictResolver;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RuntimeCoordinationPolicy {
    private final ConflictResolver conflictResolver;

    public RuntimeCoordinationPolicy() {
        this(new ConflictResolver());
    }

    public RuntimeCoordinationPolicy(ConflictResolver conflictResolver) {
        this.conflictResolver = conflictResolver == null ? new ConflictResolver() : conflictResolver;
    }

    public RuntimeCoordinationDecision decide(RuntimeMessage ego, RuntimeMessage peer, RuntimeMessage uav) {
        return decide(ego, peer, uav, null);
    }

    public RuntimeCoordinationDecision decide(RuntimeMessage ego, RuntimeMessage peer, RuntimeMessage uav, RuntimeMessage aep84Uav) {
        String egoRoute = route(ego);
        String peerRoute = route(peer);
        boolean occupancyConflict = egoRoute.equals(peerRoute);

        List<DDSMessage> evidence = new java.util.ArrayList<DDSMessage>(Arrays.asList(
            evidence(VirtualDDSMessageBus.TOPIC_DRONE_OBSERVATION, source(uav), payload(
                "role", "UAV",
                "route", route(uav),
                "riskScore", 0.34,
                "timeAdvantage", 0.74,
                "confidence", number(uav.payload, "confidence", 0.89),
                "explanation", "UAV provides aerial route observation")),
            evidence(VirtualDDSMessageBus.TOPIC_VEHICLE_STATUS, source(ego), payload(
                "role", "EGO_CAR",
                "route", egoRoute,
                "riskScore", occupancyConflict ? 0.64 : 0.36,
                "timeAdvantage", 0.90,
                "confidence", number(ego.payload, "confidence", 0.84),
                "explanation", "Ego vehicle reports current route occupancy")),
            evidence(VirtualDDSMessageBus.TOPIC_NEIGHBOR_STATUS, source(peer), payload(
                "role", "PEER_CAR",
                "route", peerRoute,
                "riskScore", occupancyConflict ? 0.58 : 0.35,
                "timeAdvantage", 0.80,
                "confidence", number(peer.payload, "confidence", 0.76),
                "explanation", "Peer vehicle reports overlapping route intent")),
            evidence(VirtualDDSMessageBus.TOPIC_ROADSIDE_RISK_EVENT, "RSU-SCHOOL-009", payload(
                "role", "RSU",
                "route", "route_D_emergency_green_wave",
                "riskScore", 0.24,
                "timeAdvantage", 0.60,
                "confidence", 0.94,
                "affectedRoute", occupancyConflict ? egoRoute : "",
                "affectedRiskScore", occupancyConflict ? 0.82 : 0.30,
                "explanation", "Coordinator candidate evidence favors green-wave detour when route occupancy conflicts"))
        ));

        if (aep84Uav != null) {
            evidence.add(evidence(VirtualDDSMessageBus.TOPIC_DRONE_OBSERVATION, source(aep84Uav), payload(
                "role", "UAV",
                "protocol", "AEP84-STANAG4586",
                "route", route(aep84Uav),
                "riskScore", number(aep84Uav.payload, "riskScore", 0.28),
                "timeAdvantage", number(aep84Uav.payload, "timeAdvantage", 0.66),
                "confidence", number(aep84Uav.payload, "confidence", 0.88),
                "explanation", "AEP84 UAV provides standardized interop status for final route selection")));
        }

        Map<String, Object> resolution = conflictResolver.resolveMessages(evidence,
            "urgent hospital-like coordination; safety first; avoid conflict");
        return new RuntimeCoordinationDecision(occupancyConflict, resolution);
    }

    private DDSMessage evidence(String topic, String sourceId, Map<String, Object> payload) {
        return new DDSMessage(topic, sourceId, "ALL", payload, DDSQoS.reliable());
    }

    private String source(RuntimeMessage message) {
        return message == null || message.sourceId == null ? "UNKNOWN" : message.sourceId;
    }

    private String route(RuntimeMessage message) {
        if (message == null || message.payload == null) return "unknown";
        Object route = message.payload.get("route");
        return route == null ? "unknown" : String.valueOf(route);
    }

    private Map<String, Object> payload(Object... values) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        for (int i = 0; i < values.length; i += 2) {
            payload.put(String.valueOf(values[i]), values[i + 1]);
        }
        return payload;
    }

    private double number(Map<String, Object> map, String key, double fallback) {
        Object value = map == null ? null : map.get(key);
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return value == null ? fallback : Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static class RuntimeCoordinationDecision {
        public final boolean occupancyConflict;
        public final Map<String, Object> resolution;

        RuntimeCoordinationDecision(boolean occupancyConflict, Map<String, Object> resolution) {
            this.occupancyConflict = occupancyConflict;
            this.resolution = resolution == null ? new LinkedHashMap<String, Object>() : resolution;
        }

        public String selectedRoute() {
            Object route = resolution.get("finalRoute");
            return route == null ? "unknown" : String.valueOf(route);
        }

        public String selectedStrategy() {
            return "cooperative_score_policy";
        }

        public String explanation() {
            Object reason = resolution.get("decisionReason");
            return reason == null ? "" : String.valueOf(reason);
        }

        public boolean replanningTriggered() {
            return Boolean.TRUE.equals(resolution.get("whetherReplanningTriggered")) || occupancyConflict;
        }

        public String conflictType() {
            String resolverType = String.valueOf(resolution.getOrDefault("conflictType", "NONE"));
            if (!occupancyConflict) return resolverType;
            if ("NONE".equals(resolverType)) return "ROUTE_OCCUPANCY_CONFLICT";
            return "ROUTE_OCCUPANCY_CONFLICT+" + resolverType;
        }
    }
}
