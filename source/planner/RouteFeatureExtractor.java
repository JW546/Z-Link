package planner;

import policy.AgentState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RouteFeatureExtractor {
    public List<CandidateRoute> extract(AgentState state) {
        AgentState s = state == null ? AgentState.builder("AGENT-UNKNOWN").build() : state;
        Set<String> routeIds = new LinkedHashSet<String>();
        if (s.currentRoute != null && !s.currentRoute.isBlank()) routeIds.add(s.currentRoute);
        routeIds.addAll(s.candidateRoutes);
        collectRoutes(routeIds, s.ddsMessages);
        collectRouteFromSuggestion(routeIds, s.droneSuggestion);
        collectRouteFromSuggestion(routeIds, s.roadsideSuggestion);
        collectRouteFromSuggestion(routeIds, s.neighborVehicleSuggestion);
        if (routeIds.isEmpty()) {
            routeIds.add("candidate-route-" + Math.abs(s.agentId.hashCode()));
        }

        Map<String, CandidateRoute.Builder> builders = new LinkedHashMap<String, CandidateRoute.Builder>();
        int index = 0;
        for (String routeId : routeIds) {
            builders.put(routeId, CandidateRoute.builder(routeId)
                .travelTime(18.0 + index * 2.0)
                .distance(8.0 + index)
                .safetyRisk(s.safetyRisk)
                .congestionLevel(s.congestionLevel)
                .messageConfidence(Math.max(0.50, s.messageConfidence))
                .sourceTrust(Math.max(0.50, s.sourceTrust))
                .messageFreshness(s.messageFreshness)
                .conflictPenalty(s.conflictStatus ? 0.35 : 0.0)
                .emergencyPriority(s.emergencyLevel)
                .passengerPreference((s.passengerSafetyFirst || s.passengerUrgent) ? 0.62 : 0.50));
            index++;
        }

        applyMessages(builders, s.ddsMessages, s);
        applySuggestion(builders, s.droneSuggestion, s);
        applySuggestion(builders, s.roadsideSuggestion, s);
        applySuggestion(builders, s.neighborVehicleSuggestion, s);

        List<CandidateRoute> routes = new ArrayList<CandidateRoute>();
        for (CandidateRoute.Builder builder : builders.values()) {
            routes.add(builder.build());
        }
        return routes;
    }

    public RouteSelectionResult select(AgentState state, RouteScoringContext context) {
        return select(extract(state), context);
    }

    public RouteSelectionResult select(List<CandidateRoute> routes, RouteScoringContext context) {
        RouteScoringContext ctx = context == null ? new RouteScoringContext.Builder(null).build() : context;
        List<RouteScore> scores = new ArrayList<RouteScore>();
        CandidateRoute best = null;
        double bestScore = Double.POSITIVE_INFINITY;
        boolean rejectedAny = false;

        for (CandidateRoute route : routes == null ? new ArrayList<CandidateRoute>() : routes) {
            boolean rejected = route.safetyRisk > ctx.safetyHardThreshold && !ctx.emergencyOverrideAllowed;
            String rejectionReason = rejected ? "safety risk exceeds hard threshold" : "";
            if (route.routeAvailability <= 0.05) {
                rejected = true;
                rejectionReason = rejectionReason.isBlank() ? "route unavailable" : rejectionReason + "; route unavailable";
            }
            double score = score(route, ctx);
            scores.add(new RouteScore(route, round(score), rejected, rejectionReason));
            rejectedAny = rejectedAny || rejected;
            if (!rejected && score < bestScore) {
                best = route;
                bestScore = score;
            }
        }

        if (best == null && !scores.isEmpty()) {
            best = leastRisk(scores);
        }
        boolean replan = ctx.replanRequired || ctx.conflictStatus || rejectedAny;
        return new RouteSelectionResult(best, scores, replan, explain(best, scores, ctx, replan), influencingMessages(scores));
    }

    private double score(CandidateRoute route, RouteScoringContext ctx) {
        return route.travelTime * ctx.timeWeight
            + route.distance * ctx.distanceWeight
            + route.safetyRisk * ctx.safetyWeight
            + route.congestionLevel * ctx.congestionWeight
            + route.conflictPenalty * ctx.conflictPenaltyWeight
            + route.ddsDelayMs * ctx.ddsDelayWeight
            + route.packetLossRate * ctx.packetLossWeight
            - route.routeAvailability * ctx.availabilityRewardWeight
            - route.emergencyPriority * ctx.emergencyRewardWeight
            - route.passengerPreference * ctx.passengerPreferenceRewardWeight
            - route.messageConfidence * ctx.confidenceRewardWeight
            - route.sourceTrust * ctx.trustRewardWeight
            - route.messageFreshness * ctx.freshnessRewardWeight;
    }

    private CandidateRoute leastRisk(List<RouteScore> scores) {
        CandidateRoute best = null;
        for (RouteScore score : scores) {
            if (best == null || score.route.safetyRisk < best.safetyRisk) {
                best = score.route;
            }
        }
        return best;
    }

    private String explain(CandidateRoute selected, List<RouteScore> scores, RouteScoringContext ctx, boolean replan) {
        StringBuilder builder = new StringBuilder();
        if (selected == null) {
            builder.append("No route selected because no candidate route was available.");
        } else {
            builder.append("Selected ").append(selected.routeId)
                .append(" by feature score under ")
                .append(ctx.passengerSafetyFirst ? "safety-first" : (ctx.passengerUrgent ? "time-aware" : "balanced"))
                .append(" context; safety hard threshold=").append(round(ctx.safetyHardThreshold)).append(".");
        }
        List<String> rejected = new ArrayList<String>();
        for (RouteScore score : scores) {
            if (score.rejected) rejected.add(score.route.routeId + " rejected: " + score.rejectionReason);
        }
        if (!rejected.isEmpty()) builder.append(" Rejected routes: ").append(String.join("; ", rejected)).append(".");
        builder.append(" Replanning triggered=").append(replan).append(".");
        return builder.toString();
    }

    private List<String> influencingMessages(List<RouteScore> scores) {
        List<String> messages = new ArrayList<String>();
        for (RouteScore score : scores) {
            messages.add(score.route.routeId + ": confidence=" + round(score.route.messageConfidence)
                + ", trust=" + round(score.route.sourceTrust)
                + ", freshness=" + round(score.route.messageFreshness)
                + ", delayMs=" + round(score.route.ddsDelayMs)
                + ", packetLoss=" + round(score.route.packetLossRate));
        }
        return messages;
    }

    private void collectRoutes(Set<String> routeIds, List<Map<String, Object>> messages) {
        for (Map<String, Object> message : messages == null ? new ArrayList<Map<String, Object>>() : messages) {
            Map<String, Object> payload = payload(message);
            String route = string(payload, "route", string(payload, "routeId", ""));
            if (!route.isBlank()) routeIds.add(route);
        }
    }

    private void collectRouteFromSuggestion(Set<String> routeIds, Map<String, Object> suggestion) {
        Map<String, Object> payload = payloadOrSelf(suggestion);
        String route = string(payload, "route", string(payload, "routeId", ""));
        if (!route.isBlank()) routeIds.add(route);
    }

    private void applyMessages(Map<String, CandidateRoute.Builder> builders, List<Map<String, Object>> messages, AgentState state) {
        for (Map<String, Object> message : messages == null ? new ArrayList<Map<String, Object>>() : messages) {
            Map<String, Object> payload = payload(message);
            String route = string(payload, "route", string(payload, "routeId", ""));
            if (route.isBlank()) continue;
            CandidateRoute.Builder builder = builders.get(route);
            if (builder == null) continue;
            applyFeatures(builder, payload, message, state);
        }
    }

    private void applySuggestion(Map<String, CandidateRoute.Builder> builders, Map<String, Object> suggestion, AgentState state) {
        Map<String, Object> payload = payloadOrSelf(suggestion);
        String route = string(payload, "route", string(payload, "routeId", ""));
        if (route.isBlank()) return;
        CandidateRoute.Builder builder = builders.get(route);
        if (builder != null) applyFeatures(builder, payload, suggestion, state);
    }

    private void applyFeatures(CandidateRoute.Builder builder, Map<String, Object> payload,
                               Map<String, Object> envelope, AgentState state) {
        builder.travelTime(number(payload, "travelTime", number(payload, "travelTimeMinutes",
            number(payload, "estimatedTime", number(payload, "estimated_time_min", 18.0)))))
            .distance(number(payload, "distance", number(payload, "distanceKm", 8.0)))
            .safetyRisk(number(payload, "safetyRisk", number(payload, "riskScore", number(payload, "risk_score", state.safetyRisk))))
            .congestionLevel(number(payload, "congestionLevel", number(payload, "congestion", state.congestionLevel)))
            .routeAvailability(number(payload, "routeAvailability", number(payload, "availability", 1.0)))
            .emergencyPriority(number(payload, "emergencyPriority", number(payload, "emergencyLevel", state.emergencyLevel)))
            .passengerPreference(number(payload, "passengerPreference", number(payload, "intentAlignment",
                (state.passengerSafetyFirst || state.passengerUrgent) ? 0.62 : 0.50)))
            .messageConfidence(number(payload, "messageConfidence", number(payload, "confidence",
                number(envelope, "confidence", Math.max(0.50, state.messageConfidence)))))
            .sourceTrust(number(payload, "sourceTrust", number(envelope, "sourceTrust", Math.max(0.50, state.sourceTrust))))
            .messageFreshness(number(payload, "messageFreshness", number(envelope, "freshness", state.messageFreshness)))
            .conflictPenalty(number(payload, "conflictPenalty", state.conflictStatus ? 0.35 : 0.0))
            .ddsDelayMs(number(payload, "ddsDelayMs", number(payload, "delayMs", number(envelope, "deliveryDelayMs", 0.0))))
            .packetLossRate(number(payload, "packetLossRate", number(payload, "packetLoss", 0.0)));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> payload(Map<String, Object> envelope) {
        Object payload = envelope == null ? null : envelope.get("payload");
        return payload instanceof Map<?, ?> ? (Map<String, Object>) payload : new LinkedHashMap<String, Object>();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> payloadOrSelf(Map<String, Object> envelope) {
        if (envelope == null) return new LinkedHashMap<String, Object>();
        Object payload = envelope.get("payload");
        return payload instanceof Map<?, ?> ? (Map<String, Object>) payload : envelope;
    }

    private String string(Map<String, Object> map, String key, String fallback) {
        Object value = map == null ? null : map.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private double number(Map<String, Object> map, String key, double fallback) {
        Object value = map == null ? null : map.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : fallback;
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
