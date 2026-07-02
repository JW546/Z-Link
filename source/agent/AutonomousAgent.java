package agent;

import dds.DDSMessage;
import dds.DDSQoS;
import dds.VirtualDDSMessageBus;
import policy.AgentAction;
import policy.AgentState;
import policy.WeightedAgentPolicy;
import worldmodel.SystemWorldModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class AutonomousAgent {
    protected final String agentId;
    protected final AgentRole role;
    protected final SystemWorldModel worldModel;
    protected final WeightedAgentPolicy policy;
    protected final List<DDSMessage> inbox = new ArrayList<DDSMessage>();
    protected final List<AgentAdvice> proposals = new ArrayList<AgentAdvice>();
    protected String lastExplanation = "";

    protected AutonomousAgent(String agentId, AgentRole role, WeightedAgentPolicy policy) {
        this.agentId = agentId;
        this.role = role;
        this.policy = policy;
        this.worldModel = new SystemWorldModel();
    }

    public void observe(VirtualDDSMessageBus bus) {
        for (String topic : subscribedTopics()) {
            bus.subscribe(topic, this::updateState);
        }
    }

    public void updateState(DDSMessage message) {
        inbox.add(message);
        worldModel.update(message);
    }

    public MessageAssessment evaluate(DDSMessage message) {
        double sourceTrust = worldModel.trustFor(message.sourceId);
        double confidence = message.confidence;
        double freshness = message.messageFreshness();
        double risk = readNumber(message.payload, "riskScore", readNumber(message.payload, "risk_score", worldModel.roadRisk));
        double alignment = intentAlignment(message);
        double conflict = worldModel.hasConflict ? 0.35 : 0.0;
        return new MessageAssessment(message, sourceTrust, confidence, freshness, risk, alignment, conflict);
    }

    public AgentAdvice propose() {
        AgentState state = worldModel.toAgentState(agentId, role.name());
        AgentAction action = policy.decide(state);
        String route = chooseRoute(action.route, state);
        double risk = clamp(state.localRisk + roleRiskBias());
        double confidence = clamp(state.messageConfidence * 0.55 + state.sourceTrust * 0.30 + state.messageFreshness * 0.15);
        lastExplanation = explain(action, state, route);
        AgentAdvice advice = new AgentAdvice(agentId, role, publishTopic(), route, risk, confidence, lastExplanation);
        proposals.add(advice);
        return advice;
    }

    public double arbitrate(List<AgentAdvice> advice) {
        AgentState state = worldModel.toAgentState(agentId, role.name());
        double routeAgreement = 0.0;
        AgentAdvice own = proposals.isEmpty() ? propose() : proposals.get(proposals.size() - 1);
        if (advice != null && !advice.isEmpty()) {
            int same = 0;
            for (AgentAdvice item : advice) {
                if (own.route.equals(item.route)) same++;
            }
            routeAgreement = same / (double) advice.size();
        }
        return clamp(state.sourceTrust * 0.35 + own.confidence * 0.30 + (1.0 - own.riskScore) * 0.20 + routeAgreement * 0.15);
    }

    public DDSMessage act(VirtualDDSMessageBus bus) {
        AgentAdvice advice = proposals.isEmpty() ? propose() : proposals.get(proposals.size() - 1);
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("agentId", advice.agentId);
        payload.put("role", advice.role.name());
        payload.put("route", advice.route);
        payload.put("riskScore", advice.riskScore);
        payload.put("confidence", advice.confidence);
        payload.put("explanation", advice.explanation);
        return bus.publish(advice.topic, agentId, payload, DDSQoS.reliable().withTimeToLiveMs(2_500L));
    }

    public String explain() {
        return lastExplanation;
    }

    public int observedCount() {
        return inbox.size();
    }

    public SystemWorldModel worldModel() {
        return worldModel;
    }

    public String agentId() {
        return agentId;
    }

    public AgentRole role() {
        return role;
    }

    protected abstract List<String> subscribedTopics();

    protected abstract String publishTopic();

    protected abstract String chooseRoute(String policyRoute, AgentState state);

    protected abstract double roleRiskBias();

    protected String explain(AgentAction action, AgentState state, String route) {
        return role.name() + " agent " + agentId + " chose " + route
            + " because " + action.reason
            + ", risk=" + round(state.localRisk)
            + ", congestion=" + round(state.congestionLevel)
            + ", freshness=" + round(state.messageFreshness)
            + ", trust=" + round(state.sourceTrust) + ".";
    }

    protected double intentAlignment(DDSMessage message) {
        String text = worldModel.passengerIntent == null ? "" : worldModel.passengerIntent;
        String route = String.valueOf(message.payload.getOrDefault("route", ""));
        double score = 0.45;
        if (text.contains("安全") && (route.contains("elevated") || route.contains("school") || route.contains("safe"))) score += 0.35;
        if (text.contains("尽快") && route.contains("green_wave")) score += 0.35;
        if (text.contains("拥堵") && !route.contains("main_road")) score += 0.20;
        return clamp(score);
    }

    protected double readNumber(Map<String, Object> payload, String key, double fallback) {
        Object value = payload.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : fallback;
    }

    protected double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    protected double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
