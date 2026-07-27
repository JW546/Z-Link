package policy;

import java.util.Arrays;
import java.util.List;

public class RouteTrainingStateEncoder {
    public static String encode(AgentState state) {
        AgentState s = state == null ? AgentState.builder("AGENT-UNKNOWN").build() : state;
        return "risk=" + bucket(s.safetyRisk)
            + "|congestion=" + bucket(s.congestionLevel)
            + "|confidence=" + bucket(s.messageConfidence)
            + "|freshness=" + bucket(s.messageFreshness)
            + "|trust=" + bucket(s.sourceTrust)
            + "|conflict=" + (s.conflictStatus ? "yes" : "no")
            + "|urgent=" + (s.passengerUrgent ? "yes" : "no")
            + "|safety=" + (s.passengerSafetyFirst ? "yes" : "no");
    }

    public static List<String> documentation() {
        return Arrays.asList(
            "risk: low/mid/high from local+DDS safetyRisk",
            "congestion: low/mid/high from vehicle and roadside evidence",
            "confidence/freshness/trust: DDS evidence quality buckets",
            "conflict: whether multi-device advice conflicts",
            "urgent/safety: passenger intent constraints from natural language DSL"
        );
    }

    private static String bucket(double value) {
        if (value < 0.38) return "low";
        if (value < 0.68) return "mid";
        return "high";
    }
}
