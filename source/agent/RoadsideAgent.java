package agent;

import dds.VirtualDDSMessageBus;
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

    protected double roleRiskBias() {
        return 0.06;
    }
}
