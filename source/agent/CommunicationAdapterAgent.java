package agent;

import dds.VirtualDDSMessageBus;
import policy.WeightedAgentPolicy;

import java.util.Arrays;
import java.util.List;

public class CommunicationAdapterAgent extends AutonomousAgent {
    public CommunicationAdapterAgent(String agentId) {
        super(agentId, AgentRole.COMPLIANT_BACKBONE, new WeightedAgentPolicy(0.42, 0.22, 0.46, 0.36));
    }

    protected List<String> subscribedTopics() {
        return Arrays.asList(
            VirtualDDSMessageBus.TOPIC_INTENT_DSL,
            VirtualDDSMessageBus.TOPIC_ROUTE_SUGGESTION,
            VirtualDDSMessageBus.TOPIC_DRONE_OBSERVATION,
            VirtualDDSMessageBus.TOPIC_ROADSIDE_RISK_EVENT,
            VirtualDDSMessageBus.TOPIC_NEIGHBOR_STATUS
        );
    }

    protected String publishTopic() {
        return VirtualDDSMessageBus.TOPIC_REPLANNING_REQUEST;
    }

    protected double roleRiskBias() {
        return 0.03;
    }
}
