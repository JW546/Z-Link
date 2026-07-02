package agent;

import dds.VirtualDDSMessageBus;
import policy.AgentState;
import policy.WeightedAgentPolicy;

import java.util.Arrays;
import java.util.List;

public class RoadsideAgent extends AutonomousAgent {
    public RoadsideAgent(String agentId) {
        super(agentId, AgentRole.RSU, new WeightedAgentPolicy(0.64, 0.24, 0.40, 0.30));
    }

    protected List<String> subscribedTopics() {
        return Arrays.asList(VirtualDDSMessageBus.TOPIC_INTENT_DSL, VirtualDDSMessageBus.TOPIC_NEIGHBOR_STATUS);
    }

    protected String publishTopic() {
        return VirtualDDSMessageBus.TOPIC_ROADSIDE_RISK_EVENT;
    }

    protected String chooseRoute(String policyRoute, AgentState state) {
        return state.localRisk > 0.50 || state.passengerSafetyFirst
            ? "route_B_school_zone_slow_safe"
            : "route_D_emergency_green_wave";
    }

    protected double roleRiskBias() {
        return 0.06;
    }
}
