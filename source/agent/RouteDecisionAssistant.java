package agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Optional LLM seam for route-decision explanation and analysis.
 * The assistant may enrich a decision, but it must not own route selection.
 */
public interface RouteDecisionAssistant {
    RouteDecisionSupport analyze(RouteDecisionRequest request);

    class RouteDecisionRequest {
        public final String passengerIntent;
        public final String destination;
        public final String selectedRoute;
        public final String ruleReason;
        public final List<Map<String, Object>> deviceAdvice;
        public final Map<String, Object> conflictResolution;
        public final Map<String, Object> routeSelection;

        public RouteDecisionRequest(String passengerIntent, String destination, String selectedRoute,
                                    String ruleReason, List<Map<String, Object>> deviceAdvice,
                                    Map<String, Object> conflictResolution,
                                    Map<String, Object> routeSelection) {
            this.passengerIntent = passengerIntent == null ? "" : passengerIntent;
            this.destination = destination == null ? "" : destination;
            this.selectedRoute = selectedRoute == null ? "" : selectedRoute;
            this.ruleReason = ruleReason == null ? "" : ruleReason;
            this.deviceAdvice = deviceAdvice == null ? Collections.emptyList() : new ArrayList<Map<String, Object>>(deviceAdvice);
            this.conflictResolution = conflictResolution == null ? Collections.emptyMap() : conflictResolution;
            this.routeSelection = routeSelection == null ? Collections.emptyMap() : routeSelection;
        }
    }

    class RouteDecisionSupport {
        public final boolean available;
        public final String provider;
        public final String model;
        public final String summary;
        public final String passengerExplanation;
        public final double confidence;
        public final long latencyMs;

        public RouteDecisionSupport(boolean available, String provider, String model, String summary,
                                    String passengerExplanation, double confidence, long latencyMs) {
            this.available = available;
            this.provider = provider == null ? "none" : provider;
            this.model = model == null ? "none" : model;
            this.summary = summary == null ? "" : summary;
            this.passengerExplanation = passengerExplanation == null ? "" : passengerExplanation;
            this.confidence = confidence;
            this.latencyMs = latencyMs;
        }

        public static RouteDecisionSupport unavailable(String reason) {
            return new RouteDecisionSupport(false, "none", "none", reason, "", 0.0, 0);
        }
    }
}
