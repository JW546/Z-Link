package planner;

import java.util.LinkedHashMap;
import java.util.Map;

public class CandidateRoute {
    public final String routeId;
    public final double travelTime;
    public final double distance;
    public final double safetyRisk;
    public final double congestionLevel;
    public final double routeAvailability;
    public final double emergencyPriority;
    public final double passengerPreference;
    public final double messageConfidence;
    public final double sourceTrust;
    public final double messageFreshness;
    public final double conflictPenalty;
    public final double ddsDelayMs;
    public final double packetLossRate;

    private CandidateRoute(Builder builder) {
        this.routeId = builder.routeId;
        this.travelTime = builder.travelTime;
        this.distance = builder.distance;
        this.safetyRisk = clamp(builder.safetyRisk);
        this.congestionLevel = clamp(builder.congestionLevel);
        this.routeAvailability = clamp(builder.routeAvailability);
        this.emergencyPriority = clamp(builder.emergencyPriority);
        this.passengerPreference = clamp(builder.passengerPreference);
        this.messageConfidence = clamp(builder.messageConfidence);
        this.sourceTrust = clamp(builder.sourceTrust);
        this.messageFreshness = clamp(builder.messageFreshness);
        this.conflictPenalty = clamp(builder.conflictPenalty);
        this.ddsDelayMs = Math.max(0.0, builder.ddsDelayMs);
        this.packetLossRate = clamp(builder.packetLossRate);
    }

    public static Builder builder(String routeId) {
        return new Builder(routeId);
    }

    public Builder toBuilder() {
        return builder(routeId)
            .travelTime(travelTime)
            .distance(distance)
            .safetyRisk(safetyRisk)
            .congestionLevel(congestionLevel)
            .routeAvailability(routeAvailability)
            .emergencyPriority(emergencyPriority)
            .passengerPreference(passengerPreference)
            .messageConfidence(messageConfidence)
            .sourceTrust(sourceTrust)
            .messageFreshness(messageFreshness)
            .conflictPenalty(conflictPenalty)
            .ddsDelayMs(ddsDelayMs)
            .packetLossRate(packetLossRate);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("routeId", routeId);
        map.put("travelTime", travelTime);
        map.put("distance", distance);
        map.put("safetyRisk", safetyRisk);
        map.put("congestionLevel", congestionLevel);
        map.put("routeAvailability", routeAvailability);
        map.put("emergencyPriority", emergencyPriority);
        map.put("passengerPreference", passengerPreference);
        map.put("messageConfidence", messageConfidence);
        map.put("sourceTrust", sourceTrust);
        map.put("messageFreshness", messageFreshness);
        map.put("conflictPenalty", conflictPenalty);
        map.put("ddsDelayMs", ddsDelayMs);
        map.put("packetLossRate", packetLossRate);
        return map;
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    public static class Builder {
        private final String routeId;
        private double travelTime = 18.0;
        private double distance = 8.0;
        private double safetyRisk = 0.30;
        private double congestionLevel = 0.30;
        private double routeAvailability = 1.0;
        private double emergencyPriority = 0.0;
        private double passengerPreference = 0.50;
        private double messageConfidence = 0.60;
        private double sourceTrust = 0.60;
        private double messageFreshness = 1.0;
        private double conflictPenalty = 0.0;
        private double ddsDelayMs = 0.0;
        private double packetLossRate = 0.0;

        private Builder(String routeId) {
            this.routeId = routeId == null || routeId.isBlank() ? "candidate-route" : routeId;
        }

        public Builder travelTime(double value) { this.travelTime = Math.max(0.1, value); return this; }
        public Builder distance(double value) { this.distance = Math.max(0.1, value); return this; }
        public Builder safetyRisk(double value) { this.safetyRisk = value; return this; }
        public Builder congestionLevel(double value) { this.congestionLevel = value; return this; }
        public Builder routeAvailability(double value) { this.routeAvailability = value; return this; }
        public Builder emergencyPriority(double value) { this.emergencyPriority = value; return this; }
        public Builder passengerPreference(double value) { this.passengerPreference = value; return this; }
        public Builder messageConfidence(double value) { this.messageConfidence = value; return this; }
        public Builder sourceTrust(double value) { this.sourceTrust = value; return this; }
        public Builder messageFreshness(double value) { this.messageFreshness = value; return this; }
        public Builder conflictPenalty(double value) { this.conflictPenalty = value; return this; }
        public Builder ddsDelayMs(double value) { this.ddsDelayMs = value; return this; }
        public Builder packetLossRate(double value) { this.packetLossRate = value; return this; }
        public CandidateRoute build() { return new CandidateRoute(this); }
    }
}
