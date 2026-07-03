package planner;

import agent.AgentRole;
import policy.AgentState;

public class RouteScoringContext {
    public final AgentRole role;
    public final boolean passengerSafetyFirst;
    public final boolean passengerUrgent;
    public final boolean emergencyOverrideAllowed;
    public final boolean conflictStatus;
    public final boolean replanRequired;
    public final double safetyHardThreshold;
    public final double timeWeight;
    public final double distanceWeight;
    public final double safetyWeight;
    public final double congestionWeight;
    public final double availabilityRewardWeight;
    public final double emergencyRewardWeight;
    public final double passengerPreferenceRewardWeight;
    public final double confidenceRewardWeight;
    public final double trustRewardWeight;
    public final double freshnessRewardWeight;
    public final double conflictPenaltyWeight;
    public final double ddsDelayWeight;
    public final double packetLossWeight;

    private RouteScoringContext(Builder builder) {
        this.role = builder.role;
        this.passengerSafetyFirst = builder.passengerSafetyFirst;
        this.passengerUrgent = builder.passengerUrgent;
        this.emergencyOverrideAllowed = builder.emergencyOverrideAllowed;
        this.conflictStatus = builder.conflictStatus;
        this.replanRequired = builder.replanRequired;
        this.safetyHardThreshold = builder.safetyHardThreshold;
        this.timeWeight = builder.timeWeight;
        this.distanceWeight = builder.distanceWeight;
        this.safetyWeight = builder.safetyWeight;
        this.congestionWeight = builder.congestionWeight;
        this.availabilityRewardWeight = builder.availabilityRewardWeight;
        this.emergencyRewardWeight = builder.emergencyRewardWeight;
        this.passengerPreferenceRewardWeight = builder.passengerPreferenceRewardWeight;
        this.confidenceRewardWeight = builder.confidenceRewardWeight;
        this.trustRewardWeight = builder.trustRewardWeight;
        this.freshnessRewardWeight = builder.freshnessRewardWeight;
        this.conflictPenaltyWeight = builder.conflictPenaltyWeight;
        this.ddsDelayWeight = builder.ddsDelayWeight;
        this.packetLossWeight = builder.packetLossWeight;
    }

    public static RouteScoringContext fromState(AgentState state, AgentRole role) {
        AgentState s = state == null ? AgentState.builder("AGENT-UNKNOWN").build() : state;
        Builder builder = new Builder(role)
            .passengerSafetyFirst(s.passengerSafetyFirst)
            .passengerUrgent(s.passengerUrgent)
            .conflictStatus(s.conflictStatus)
            .replanRequired(s.replanRequired);
        if (s.passengerSafetyFirst) {
            builder.safetyWeight(92.0).congestionWeight(18.0).timeWeight(0.72);
        }
        if (s.passengerUrgent && !s.passengerSafetyFirst) {
            builder.timeWeight(1.22).emergencyRewardWeight(9.0);
        }
        if (role == AgentRole.RSU) {
            builder.safetyWeight(builder.safetyWeight + 10.0).trustRewardWeight(8.0);
        } else if (role == AgentRole.UAV) {
            builder.timeWeight(builder.timeWeight + 0.20).freshnessRewardWeight(10.0);
        } else if (role == AgentRole.PEER_VEHICLE) {
            builder.congestionWeight(builder.congestionWeight + 4.0).confidenceRewardWeight(9.0);
        } else if (role == AgentRole.COMPLIANT_BACKBONE) {
            builder.conflictPenaltyWeight(builder.conflictPenaltyWeight + 6.0).trustRewardWeight(8.5);
        }
        return builder.build();
    }

    public static class Builder {
        private final AgentRole role;
        private boolean passengerSafetyFirst;
        private boolean passengerUrgent;
        private boolean emergencyOverrideAllowed;
        private boolean conflictStatus;
        private boolean replanRequired;
        private double safetyHardThreshold = 0.62;
        private double timeWeight = 0.85;
        private double distanceWeight = 0.22;
        private double safetyWeight = 58.0;
        private double congestionWeight = 13.0;
        private double availabilityRewardWeight = 10.0;
        private double emergencyRewardWeight = 6.0;
        private double passengerPreferenceRewardWeight = 8.0;
        private double confidenceRewardWeight = 8.0;
        private double trustRewardWeight = 7.0;
        private double freshnessRewardWeight = 8.0;
        private double conflictPenaltyWeight = 18.0;
        private double ddsDelayWeight = 1.0 / 120.0;
        private double packetLossWeight = 16.0;

        public Builder(AgentRole role) {
            this.role = role == null ? AgentRole.EGO_VEHICLE : role;
        }

        public Builder passengerSafetyFirst(boolean value) { this.passengerSafetyFirst = value; return this; }
        public Builder passengerUrgent(boolean value) { this.passengerUrgent = value; return this; }
        public Builder emergencyOverrideAllowed(boolean value) { this.emergencyOverrideAllowed = value; return this; }
        public Builder conflictStatus(boolean value) { this.conflictStatus = value; return this; }
        public Builder replanRequired(boolean value) { this.replanRequired = value; return this; }
        public Builder safetyHardThreshold(double value) { this.safetyHardThreshold = value; return this; }
        public Builder timeWeight(double value) { this.timeWeight = value; return this; }
        public Builder distanceWeight(double value) { this.distanceWeight = value; return this; }
        public Builder safetyWeight(double value) { this.safetyWeight = value; return this; }
        public Builder congestionWeight(double value) { this.congestionWeight = value; return this; }
        public Builder availabilityRewardWeight(double value) { this.availabilityRewardWeight = value; return this; }
        public Builder emergencyRewardWeight(double value) { this.emergencyRewardWeight = value; return this; }
        public Builder passengerPreferenceRewardWeight(double value) { this.passengerPreferenceRewardWeight = value; return this; }
        public Builder confidenceRewardWeight(double value) { this.confidenceRewardWeight = value; return this; }
        public Builder trustRewardWeight(double value) { this.trustRewardWeight = value; return this; }
        public Builder freshnessRewardWeight(double value) { this.freshnessRewardWeight = value; return this; }
        public Builder conflictPenaltyWeight(double value) { this.conflictPenaltyWeight = value; return this; }
        public Builder ddsDelayWeight(double value) { this.ddsDelayWeight = value; return this; }
        public Builder packetLossWeight(double value) { this.packetLossWeight = value; return this; }
        public RouteScoringContext build() { return new RouteScoringContext(this); }
    }
}
