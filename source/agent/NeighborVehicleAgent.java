package agent;

import dds.VirtualDDSMessageBus;
import policy.AgentState;
import policy.WeightedAgentPolicy;

import java.util.Arrays;
import java.util.List;

public class NeighborVehicleAgent extends AutonomousAgent {
    public NeighborVehicleAgent(String agentId) {
        super(agentId, AgentRole.PEER_VEHICLE, new WeightedAgentPolicy(0.40, 0.46, 0.22, 0.18));
    }

    protected List<String> subscribedTopics() {
        return Arrays.asList(VirtualDDSMessageBus.TOPIC_INTENT_DSL, VirtualDDSMessageBus.TOPIC_ROUTE_SUGGESTION);
    }

    protected String publishTopic() {
        return VirtualDDSMessageBus.TOPIC_NEIGHBOR_STATUS;
    }

    protected String chooseRoute(String policyRoute, AgentState state) {
        return state.congestionLevel > 0.70 ? "route_C_elevated_detour" : "route_A_main_road";
    }

    protected double roleRiskBias() {
        return 0.00;
    }
}
