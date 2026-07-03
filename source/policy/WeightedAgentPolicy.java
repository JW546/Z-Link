package policy;

/**
 * Feature-weighted policy used by autonomous agents. It is deterministic and
 * inspectable, but not a fixed if-else script and not RL/MARL training.
 */
public class WeightedAgentPolicy implements AgentPolicy {
    private final double safetyWeight;
    private final double timeWeight;
    private final double trustWeight;
    private final double conflictWeight;

    public WeightedAgentPolicy(double safetyWeight, double timeWeight, double trustWeight, double conflictWeight) {
        this.safetyWeight = safetyWeight;
        this.timeWeight = timeWeight;
        this.trustWeight = trustWeight;
        this.conflictWeight = conflictWeight;
    }

    public AgentAction decide(AgentState state) {
        double safetyPressure = state.localRisk * safetyWeight
            + state.congestionLevel * 0.35
            + (state.passengerSafetyFirst ? 0.28 : 0.0);
        double timePressure = state.passengerUrgent ? timeWeight : timeWeight * 0.55;
        double evidence = state.messageConfidence * 0.45 + state.messageFreshness * 0.30 + state.sourceTrust * trustWeight;
        double conflict = state.hasConflict ? conflictWeight : 0.0;
        double priority = clamp(safetyPressure + timePressure + evidence + conflict);

        AgentAction.Type action = state.replanRequired || priority > 0.72
            ? AgentAction.Type.REQUEST_REPLANNING : AgentAction.Type.CHOOSE_ROUTE;
        return new AgentAction(action, "", "", priority,
            "weighted policy: safety=" + round(safetyPressure)
                + ", time=" + round(timePressure)
                + ", evidence=" + round(evidence)
                + ", conflict=" + round(conflict),
            safetyPressure >= timePressure,
            timePressure > safetyPressure);
    }

    public String describe() {
        return "WeightedAgentPolicy(safety=" + safetyWeight
            + ", time=" + timeWeight
            + ", trust=" + trustWeight
            + ", conflict=" + conflictWeight + ")";
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
