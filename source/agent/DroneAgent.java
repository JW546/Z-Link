package agent;

import dds.VirtualDDSMessageBus;
import policy.AgentState;
import policy.WeightedAgentPolicy;

import java.util.Arrays;
import java.util.List;

public class DroneAgent extends AutonomousAgent {
    public DroneAgent(String agentId) {
        super(agentId, AgentRole.UAV, new WeightedAgentPolicy(0.48, 0.30, 0.36, 0.18));
    }

    protected List<String> subscribedTopics() {
        return Arrays.asList(VirtualDDSMessageBus.TOPIC_INTENT_DSL, VirtualDDSMessageBus.TOPIC_ROADSIDE_RISK_EVENT);
    }

    protected String publishTopic() {
        return VirtualDDSMessageBus.TOPIC_DRONE_OBSERVATION;
    }

    protected String chooseRoute(String policyRoute, AgentState state) {
        return state.congestionLevel > 0.65 ? "route_C_elevated_detour" : "route_A_main_road";
    }

    protected double roleRiskBias() {
        return -0.05;
    }
}
