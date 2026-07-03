package agent;

import dds.VirtualDDSMessageBus;
import policy.WeightedAgentPolicy;

import java.util.Arrays;
import java.util.List;

public class VehicleAgent extends AutonomousAgent {
    public VehicleAgent(String agentId) {
        super(agentId, AgentRole.EGO_VEHICLE, new WeightedAgentPolicy(0.56, 0.42, 0.30, 0.28));
    }

    protected List<String> subscribedTopics() {
        return Arrays.asList(
            VirtualDDSMessageBus.TOPIC_INTENT_DSL,
            VirtualDDSMessageBus.TOPIC_DRONE_OBSERVATION,
            VirtualDDSMessageBus.TOPIC_ROADSIDE_RISK_EVENT,
            VirtualDDSMessageBus.TOPIC_NEIGHBOR_STATUS
        );
    }

    protected String publishTopic() {
        return VirtualDDSMessageBus.TOPIC_ROUTE_SUGGESTION;
    }

    protected double roleRiskBias() {
        return 0.02;
    }
}
