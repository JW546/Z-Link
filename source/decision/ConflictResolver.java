package decision;

import agent.AgentAdvice;
import dds.DDSMessage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Configurable resolver for DDS route-advice conflicts.
 */
public class ConflictResolver {
    private final ConflictResolutionConfig config;

    public ConflictResolver() {
        this(new ConflictResolutionConfig());
    }

    public ConflictResolver(ConflictResolutionConfig config) {
        this.config = config == null ? new ConflictResolutionConfig() : config;
    }

    public Map<String, Object> resolve(List<AgentAdvice> adviceList, boolean urgent, boolean safetyFirst) {
        List<SuggestionEvidence> evidence = new ArrayList<SuggestionEvidence>();
        if (adviceList != null) {
            for (AgentAdvice advice : adviceList) {
                if (advice == null) continue;
                evidence.add(SuggestionEvidence.fromAdvice(advice, config));
            }
        }
        return resolveEvidence(evidence, urgent, safetyFirst);
    }

    public Map<String, Object> resolveMessages(List<DDSMessage> messages, String passengerIntent) {
        boolean urgent = containsAny(passengerIntent, "尽快", "马上", "快", "urgent");
        boolean safetyFirst = containsAny(passengerIntent, "安全", "避开", "safe");
        List<SuggestionEvidence> evidence = new ArrayList<SuggestionEvidence>();
        if (messages != null) {
            for (DDSMessage message : messages) {
                if (message == null) continue;
                evidence.add(SuggestionEvidence.fromMessage(message, config));
            }
        }
        return resolveEvidence(evidence, urgent, safetyFirst);
    }

    public Map<String, Object> resolveEvidence(List<SuggestionEvidence> evidenceList,
                                               boolean urgent, boolean safetyFirst) {
        List<SuggestionEvidence> safeEvidence = evidenceList == null
            ? new ArrayList<SuggestionEvidence>() : new ArrayList<SuggestionEvidence>(evidenceList);
        Map<String, Double> routeRiskEvidence = routeRiskEvidence(safeEvidence);
        for (SuggestionEvidence evidence : safeEvidence) {
            Double externalRisk = routeRiskEvidence.get(evidence.route);
            if (externalRisk != null && externalRisk > evidence.riskScore) {
                evidence.riskScore = externalRisk;
                evidence.reason = appendReason(evidence.reason,
                    "external DDS risk evidence marks this route as unsafe");
            }
        }
        for (SuggestionEvidence evidence : safeEvidence) {
            evidence.score = score(evidence, urgent, safetyFirst);
            evidence.rejected = shouldReject(evidence, safetyFirst);
            evidence.rejectionReason = rejectionReason(evidence, safetyFirst);
        }

        safeEvidence.sort(Comparator
            .comparing((SuggestionEvidence item) -> item.rejected)
            .thenComparing((SuggestionEvidence item) -> -item.score));

        SuggestionEvidence accepted = null;
        for (SuggestionEvidence evidence : safeEvidence) {
            if (!evidence.rejected) {
                accepted = evidence;
                break;
            }
        }
        if (accepted == null && !safeEvidence.isEmpty()) {
            accepted = safeEvidence.get(0);
        }

        List<Map<String, Object>> rejected = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> priorityOrder = new ArrayList<Map<String, Object>>();
        for (SuggestionEvidence evidence : safeEvidence) {
            priorityOrder.add(evidence.toMap());
            if (accepted == null || evidence != accepted) {
                Map<String, Object> item = evidence.toMap();
                item.put("rejectionReason", evidence.rejectionReason.isBlank()
                    ? "lower weighted evidence than accepted suggestion" : evidence.rejectionReason);
                rejected.add(item);
            }
        }

        List<String> conflictTypes = detectConflictTypes(safeEvidence, urgent, safetyFirst);
        boolean replanning = accepted != null && (accepted.riskScore >= config.replanningRiskThreshold
            || conflictTypes.contains("ROUTE_DIVERGENCE")
            || conflictTypes.contains("SAFETY_RISK_CONFLICT"));

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("hasConflict", !conflictTypes.isEmpty());
        result.put("conflictType", conflictTypes.isEmpty() ? "NONE" : String.join("+", conflictTypes));
        result.put("acceptedSuggestion", accepted == null ? new LinkedHashMap<String, Object>() : accepted.toMap());
        result.put("rejectedSuggestions", rejected);
        result.put("priorityOrder", priorityOrder);
        result.put("decisionReason", decisionReason(accepted, conflictTypes, urgent, safetyFirst));
        result.put("confidence", accepted == null ? 0.0 : round(accepted.score));
        result.put("whetherReplanningTriggered", replanning);
        result.put("timelineEvent", timelineEvent(accepted, conflictTypes, replanning));

        // Backward-compatible fields used by earlier tests and API code.
        result.put("conflictingRoutes", routes(safeEvidence));
        result.put("finalRoute", accepted == null ? "" : accepted.route);
        result.put("riskScore", accepted == null ? 0.50 : round(accepted.riskScore));
        result.put("policy", safetyFirst ? "configurable_safety_first_conflict_resolution"
            : "configurable_time_risk_conflict_resolution");
        result.put("explanation", result.get("decisionReason"));
        return result;
    }

    private double score(SuggestionEvidence evidence, boolean urgent, boolean safetyFirst) {
        double timeReward = urgent ? evidence.timeAdvantage * config.urgentTimeWeight : evidence.timeAdvantage * 0.08;
        double safetyPenalty = evidence.riskScore * (safetyFirst ? config.safetyFirstRiskWeight : config.normalRiskWeight);
        double evidenceQuality = evidence.sourceTrust * config.sourceTrustWeight
            + evidence.confidence * config.messageConfidenceWeight
            + evidence.freshness * config.messageFreshnessWeight;
        double ddsPenalty = evidence.ddsDelayMs / config.delayPenaltyScaleMs + evidence.packetLossRate * config.packetLossPenalty;
        double intentReward = evidence.intentAlignment * config.intentAlignmentWeight;
        return clamp(evidenceQuality + timeReward + intentReward - safetyPenalty - ddsPenalty);
    }

    private boolean shouldReject(SuggestionEvidence evidence, boolean safetyFirst) {
        if (safetyFirst && evidence.riskScore >= config.safetyHardRejectThreshold) return true;
        if (evidence.confidence < config.minConfidenceThreshold) return true;
        if (evidence.sourceTrust < config.minSourceTrustThreshold) return true;
        if (evidence.freshness < config.minFreshnessThreshold) return true;
        if (evidence.packetLossRate > config.maxPacketLossThreshold) return true;
        return false;
    }

    private String rejectionReason(SuggestionEvidence evidence, boolean safetyFirst) {
        List<String> reasons = new ArrayList<String>();
        if (safetyFirst && evidence.riskScore >= config.safetyHardRejectThreshold) {
            reasons.add("safety risk exceeds hard threshold");
        }
        if (evidence.confidence < config.minConfidenceThreshold) reasons.add("message confidence below threshold");
        if (evidence.sourceTrust < config.minSourceTrustThreshold) reasons.add("source trust below threshold");
        if (evidence.freshness < config.minFreshnessThreshold) reasons.add("message freshness below threshold");
        if (evidence.packetLossRate > config.maxPacketLossThreshold) reasons.add("packet loss above threshold");
        return String.join("; ", reasons);
    }

    private List<String> detectConflictTypes(List<SuggestionEvidence> evidence, boolean urgent, boolean safetyFirst) {
        List<String> types = new ArrayList<String>();
        Set<String> routeSet = routes(evidence);
        boolean hasHighRisk = false;
        boolean hasLowQuality = false;
        for (SuggestionEvidence item : evidence) {
            hasHighRisk = hasHighRisk || item.riskScore >= config.replanningRiskThreshold;
            hasLowQuality = hasLowQuality || item.confidence < config.minConfidenceThreshold
                || item.sourceTrust < config.minSourceTrustThreshold
                || item.freshness < config.minFreshnessThreshold
                || item.packetLossRate > config.maxPacketLossThreshold;
        }
        if (routeSet.size() > 1) types.add("ROUTE_DIVERGENCE");
        if (urgent && safetyFirst) types.add("INTENT_TENSION");
        if (hasHighRisk) types.add("SAFETY_RISK_CONFLICT");
        if (hasLowQuality) types.add("EVIDENCE_QUALITY_CONFLICT");
        return types;
    }

    private Map<String, Double> routeRiskEvidence(List<SuggestionEvidence> evidence) {
        Map<String, Double> riskByRoute = new LinkedHashMap<String, Double>();
        for (SuggestionEvidence item : evidence) {
            if (item.affectedRoute == null || item.affectedRoute.isBlank()) continue;
            double weightedRisk = item.affectedRiskScore * Math.max(0.25, item.sourceTrust) * Math.max(0.25, item.confidence);
            riskByRoute.put(item.affectedRoute,
                Math.max(riskByRoute.getOrDefault(item.affectedRoute, 0.0), weightedRisk));
        }
        return riskByRoute;
    }

    private String appendReason(String current, String extra) {
        if (current == null || current.isBlank()) return extra;
        return current + "; " + extra;
    }

    private Set<String> routes(List<SuggestionEvidence> evidence) {
        Set<String> routes = new LinkedHashSet<String>();
        for (SuggestionEvidence item : evidence) {
            if (item.route != null && !item.route.isBlank()) routes.add(item.route);
        }
        return routes;
    }

    private String decisionReason(SuggestionEvidence accepted, List<String> conflictTypes,
                                  boolean urgent, boolean safetyFirst) {
        if (accepted == null) {
            return "No valid suggestion was available, so the system keeps the conservative fallback route.";
        }
        String priority = safetyFirst
            ? "Passenger requested speed and safety; safety is treated as the higher-priority constraint."
            : "Passenger priority allows a stronger time component while still considering risk.";
        return priority + " Accepted " + accepted.route + " from " + accepted.sourceId
            + " because weighted evidence score=" + round(accepted.score)
            + ", risk=" + round(accepted.riskScore)
            + ", sourceTrust=" + round(accepted.sourceTrust)
            + ", confidence=" + round(accepted.confidence)
            + ", freshness=" + round(accepted.freshness)
            + ". Conflict types: " + (conflictTypes.isEmpty() ? "NONE" : String.join(",", conflictTypes))
            + ". Fastest routes are rejected when their risk or evidence quality violates thresholds.";
    }

    private Map<String, Object> timelineEvent(SuggestionEvidence accepted, List<String> conflictTypes,
                                              boolean replanning) {
        Map<String, Object> event = new LinkedHashMap<String, Object>();
        event.put("phase", "CONFLICT_RESOLUTION");
        event.put("timestamp", System.currentTimeMillis());
        event.put("acceptedRoute", accepted == null ? null : accepted.route);
        event.put("acceptedSource", accepted == null ? null : accepted.sourceId);
        event.put("conflictType", conflictTypes.isEmpty() ? "NONE" : String.join("+", conflictTypes));
        event.put("replanningTriggered", replanning);
        return event;
    }

    private boolean containsAny(String text, String... tokens) {
        String value = text == null ? "" : text;
        for (String token : tokens) {
            if (value.contains(token)) return true;
        }
        return false;
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    public static class ConflictResolutionConfig {
        public double sourceTrustWeight = 0.26;
        public double messageConfidenceWeight = 0.22;
        public double messageFreshnessWeight = 0.18;
        public double intentAlignmentWeight = 0.14;
        public double urgentTimeWeight = 0.18;
        public double safetyFirstRiskWeight = 0.62;
        public double normalRiskWeight = 0.35;
        public double safetyHardRejectThreshold = 0.62;
        public double replanningRiskThreshold = 0.55;
        public double minConfidenceThreshold = 0.45;
        public double minSourceTrustThreshold = 0.45;
        public double minFreshnessThreshold = 0.20;
        public double maxPacketLossThreshold = 0.50;
        public double delayPenaltyScaleMs = 1_500.0;
        public double packetLossPenalty = 0.24;
        public Map<String, Double> sourceTrust = defaultSourceTrust();

        private static Map<String, Double> defaultSourceTrust() {
            Map<String, Double> trust = new LinkedHashMap<String, Double>();
            trust.put("UAV-CQ-007", 0.88);
            trust.put("RSU-SCHOOL-009", 0.94);
            trust.put("PEER-CAR-031", 0.72);
            trust.put("CAR-CQ-001", 0.86);
            trust.put("CAR-CQ-002", 0.82);
            trust.put("CN-BACKBONE-CQ", 0.78);
            return trust;
        }
    }

    public static class SuggestionEvidence {
        public String sourceId;
        public String role;
        public String route;
        public double riskScore;
        public double confidence;
        public double sourceTrust;
        public double freshness;
        public double timeAdvantage;
        public double intentAlignment;
        public double ddsDelayMs;
        public double packetLossRate;
        public String affectedRoute;
        public double affectedRiskScore;
        public String reason;
        public double score;
        public boolean rejected;
        public String rejectionReason = "";

        public static SuggestionEvidence fromAdvice(AgentAdvice advice, ConflictResolutionConfig config) {
            SuggestionEvidence evidence = new SuggestionEvidence();
            evidence.sourceId = advice.agentId;
            evidence.role = advice.role.name();
            evidence.route = advice.route;
            evidence.riskScore = advice.riskScore;
            evidence.confidence = advice.confidence;
            evidence.sourceTrust = config.sourceTrust.getOrDefault(advice.agentId, 0.60);
            evidence.freshness = 1.0;
            evidence.timeAdvantage = 0.50;
            evidence.intentAlignment = 0.65;
            evidence.ddsDelayMs = 0.0;
            evidence.packetLossRate = 0.0;
            evidence.affectedRoute = "";
            evidence.affectedRiskScore = advice.riskScore;
            evidence.reason = advice.explanation;
            return evidence;
        }

        public static SuggestionEvidence fromMessage(DDSMessage message, ConflictResolutionConfig config) {
            SuggestionEvidence evidence = new SuggestionEvidence();
            evidence.sourceId = message.sourceId;
            evidence.role = string(message.payload, "role", roleFromSource(message.sourceId));
            evidence.route = string(message.payload, "route", "");
            evidence.riskScore = number(message.payload, "riskScore", number(message.payload, "risk_score", 0.35));
            evidence.confidence = message.confidence;
            evidence.sourceTrust = number(message.payload, "sourceTrust",
                config.sourceTrust.getOrDefault(message.sourceId, 0.60));
            evidence.freshness = message.messageFreshness();
            evidence.timeAdvantage = number(message.payload, "timeAdvantage", 0.50);
            evidence.intentAlignment = number(message.payload, "intentAlignment", 0.65);
            evidence.ddsDelayMs = number(message.payload, "ddsDelayMs",
                message.deliveredAt() > 0L ? message.deliveredAt() - message.timestamp : 0.0);
            evidence.packetLossRate = number(message.payload, "packetLossRate", 0.0);
            evidence.affectedRoute = string(message.payload, "affectedRoute",
                string(message.payload, "riskRoute", string(message.payload, "blockedRoute", "")));
            evidence.affectedRiskScore = number(message.payload, "affectedRiskScore",
                number(message.payload, "riskRouteScore", evidence.riskScore));
            evidence.reason = string(message.payload, "explanation", string(message.payload, "reason", ""));
            return evidence;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            map.put("sourceId", sourceId);
            map.put("role", role);
            map.put("route", route);
            map.put("riskScore", roundLocal(riskScore));
            map.put("confidence", roundLocal(confidence));
            map.put("sourceTrust", roundLocal(sourceTrust));
            map.put("freshness", roundLocal(freshness));
            map.put("timeAdvantage", roundLocal(timeAdvantage));
            map.put("intentAlignment", roundLocal(intentAlignment));
            map.put("ddsDelayMs", roundLocal(ddsDelayMs));
            map.put("packetLossRate", roundLocal(packetLossRate));
            map.put("affectedRoute", affectedRoute);
            map.put("affectedRiskScore", roundLocal(affectedRiskScore));
            map.put("score", roundLocal(score));
            map.put("rejected", rejected);
            map.put("reason", reason);
            return map;
        }

        private static double routeTimeAdvantage(String route) {
            return 0.50;
        }

        private static String roleFromSource(String sourceId) {
            if (sourceId == null) return "UNKNOWN";
            if (sourceId.contains("UAV")) return "UAV";
            if (sourceId.contains("RSU")) return "RSU";
            if (sourceId.contains("PEER")) return "PEER_VEHICLE";
            if (sourceId.contains("BACKBONE")) return "COMPLIANT_BACKBONE";
            return "UNKNOWN";
        }

        private static String string(Map<String, Object> payload, String key, String fallback) {
            Object value = payload == null ? null : payload.get(key);
            return value == null ? fallback : String.valueOf(value);
        }

        private static double number(Map<String, Object> payload, String key, double fallback) {
            Object value = payload == null ? null : payload.get(key);
            return value instanceof Number ? ((Number) value).doubleValue() : fallback;
        }

        private static double roundLocal(double value) {
            return Math.round(value * 1000.0) / 1000.0;
        }
    }
}
